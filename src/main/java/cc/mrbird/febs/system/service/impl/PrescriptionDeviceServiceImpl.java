package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.common.mqtt.DeviceMqttProperties;
import cc.mrbird.febs.system.domain.Prescription;
import cc.mrbird.febs.system.service.PrescriptionService;
import cc.mrbird.febs.system.service.PrescriptionDeviceService;
import cc.mrbird.febs.system.dto.PrescriptionDeviceRequest;
import cc.mrbird.febs.system.dto.PrescriptionDeviceResponse;
import cc.mrbird.febs.system.domain.Device;
import cc.mrbird.febs.system.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 处方设备服务实现类
 * 负责通过MQTT发送处方信息到设备
 */
@Slf4j
@Service
public class PrescriptionDeviceServiceImpl implements PrescriptionDeviceService {

    private final PrescriptionService prescriptionService;
    private final DeviceService deviceService;
    private final DeviceMqttProperties mqttProperties; // 设备状态相关配置，保留
    private final MessageChannel tmsMqttOutboundChannel;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储处方发送状态的内存缓存
    private final ConcurrentHashMap<Long, PrescriptionDeviceResponse> prescriptionStatusCache = new ConcurrentHashMap<>();
    
    // 存储设备连接状态的内存缓存
    private final ConcurrentHashMap<Long, Boolean> deviceConnectionCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter ISO_OFFSET_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public PrescriptionDeviceServiceImpl(
            PrescriptionService prescriptionService,
            DeviceService deviceService,
            DeviceMqttProperties mqttProperties,
            @Qualifier("tmsMqttOutboundChannel") MessageChannel tmsMqttOutboundChannel
    ) {
        this.prescriptionService = prescriptionService;
        this.deviceService = deviceService;
        this.mqttProperties = mqttProperties;
        this.tmsMqttOutboundChannel = tmsMqttOutboundChannel;
    }

    @Override
    public PrescriptionDeviceResponse sendPrescriptionToDevice(PrescriptionDeviceRequest request) {
        try {
            log.info("开始发送处方到设备，处方ID: {}, 设备ID: {}", request.getPrescriptionId(), request.getDeviceId());
            
            // 1. 获取处方信息
            Prescription prescription = prescriptionService.getById(request.getPrescriptionId());
            if (prescription == null) {
                return PrescriptionDeviceResponse.failure(request.getPrescriptionId(), request.getDeviceId(), "处方不存在");
            }
            
            // 2. 获取设备信息
            Device device = deviceService.getById(request.getDeviceId());
            if (device == null) {
                return PrescriptionDeviceResponse.failure(request.getPrescriptionId(), request.getDeviceId(), "设备不存在");
            }
            
            // 3. 检查设备连接状态
            if (!isDeviceConnected(request.getDeviceId())) {
                return PrescriptionDeviceResponse.failure(request.getPrescriptionId(), request.getDeviceId(), "设备未连接");
            }
            
            // 4. 构建MQTT消息（按需求改造成处方实体JSON结构）
            String messageId = UUID.randomUUID().toString();
            String payload = buildPrescriptionEntityPayloadJson(prescription);
            
            // 5. 发送MQTT消息
            String topic = buildDeviceTopic(device);
            
            // 发送消息到MQTT输出通道（显式指定主题，避免落入 defaultTopic）
            this.tmsMqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, 2)
                    .setHeader(MqttHeaders.RETAINED, false)
                    .build()
            );
            
            // 6. 创建响应并缓存状态
            PrescriptionDeviceResponse response = PrescriptionDeviceResponse.success(
                request.getPrescriptionId(), request.getDeviceId(), messageId);
            response.setDeviceResponse(payload);
            
            // 缓存发送状态
            prescriptionStatusCache.put(request.getPrescriptionId(), response);
            
            log.info("处方发送成功，消息ID: {}, 主题: {}", messageId, topic);
            return response;
            
        } catch (Exception e) {
            log.error("发送处方到设备失败，处方ID: {}, 设备ID: {}", request.getPrescriptionId(), request.getDeviceId(), e);
            return PrescriptionDeviceResponse.failure(request.getPrescriptionId(), request.getDeviceId(), e.getMessage());
        }
    }

    @Override
    public List<PrescriptionDeviceResponse> batchSendPrescriptions(List<PrescriptionDeviceRequest> requests) {
        log.info("开始批量发送处方，数量: {}", requests.size());
        
        return requests.parallelStream()
            .map(this::sendPrescriptionToDevice)
            .collect(Collectors.toList());
    }

    @Override
    public PrescriptionDeviceResponse getPrescriptionStatus(Long prescriptionId) {
        PrescriptionDeviceResponse status = prescriptionStatusCache.get(prescriptionId);
        if (status == null) {
            return PrescriptionDeviceResponse.failure(prescriptionId, null, "未找到处方发送记录");
        }
        return status;
    }

    @Override
    public boolean cancelPrescription(Long prescriptionId) {
        try {
            PrescriptionDeviceResponse status = prescriptionStatusCache.get(prescriptionId);
            if (status != null && !status.isConfirmed()) {
                status.setStatus("CANCELLED");
                status.setErrorMessage("处方已被取消");
                prescriptionStatusCache.put(prescriptionId, status);
                
                // 发送取消消息到设备
                sendCancelMessage(prescriptionId, status.getDeviceId());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("取消处方失败，处方ID: {}", prescriptionId, e);
            return false;
        }
    }

    @Override
    public PrescriptionDeviceResponse retryPrescription(Long prescriptionId) {
        try {
            PrescriptionDeviceResponse status = prescriptionStatusCache.get(prescriptionId);
            if (status != null && status.getRetryCount() < status.getMaxRetryCount()) {
                // 增加重试次数
                status.setRetryCount(status.getRetryCount() + 1);
                status.setStatus("RETRYING");
                status.setErrorMessage(null);
                
                // 重新发送
                PrescriptionDeviceRequest retryRequest = new PrescriptionDeviceRequest();
                retryRequest.setPrescriptionId(prescriptionId);
                retryRequest.setDeviceId(status.getDeviceId());
                
                PrescriptionDeviceResponse retryResponse = sendPrescriptionToDevice(retryRequest);
                retryResponse.setRetryCount(status.getRetryCount());
                
                prescriptionStatusCache.put(prescriptionId, retryResponse);
                return retryResponse;
            }
            return PrescriptionDeviceResponse.failure(prescriptionId, null, "已达到最大重试次数");
        } catch (Exception e) {
            log.error("重试处方失败，处方ID: {}", prescriptionId, e);
            return PrescriptionDeviceResponse.failure(prescriptionId, null, e.getMessage());
        }
    }

    @Override
    public boolean isDeviceConnected(Long deviceId) {
        // 这里可以从设备连接缓存或MQTT连接状态中获取
        // 暂时返回true，实际应该检查MQTT连接状态
        return deviceConnectionCache.getOrDefault(deviceId, true);
    }

    /**
     * 构建处方MQTT消息
     */
    private PrescriptionMqttMessage buildPrescriptionMqttMessage(Prescription prescription, Device device, 
                                                               PrescriptionDeviceRequest request, String messageId) {
        PrescriptionMqttMessage message = new PrescriptionMqttMessage();
        message.setMessageId(messageId);
        message.setMessageType("PRESCRIPTION");
        message.setTimestamp(LocalDateTime.now());
        message.setDeviceId(device.getDeviceId());
        message.setDeviceNo(String.valueOf(device.getDeviceNo()));
        message.setPrescriptionId(prescription.getId());
        message.setPatientId(prescription.getPatientId());
        message.setDoctorId(prescription.getDoctorId());
        message.setPriority(request.getPriority());
        message.setExecuteTime(request.getExecuteTime());
        message.setExecuteImmediately(request.getExecuteImmediately());
        message.setRemark(request.getRemark());
        
        // 处方参数
        message.setPresStrength(prescription.getPresStrength());
        message.setPresFreq(prescription.getPresFreq());
        message.setLastTime(prescription.getLastTime());
        message.setPauseTime(prescription.getPauseTime());
        message.setRepeatCount(prescription.getRepeatCount());
        message.setTotalCount(prescription.getTotalCount());
        message.setTotalTime(prescription.getTotalTime());
        message.setPresPartName(prescription.getPresPartName());
        message.setStandardPresName(prescription.getStandardPresName());
        
        return message;
    }

    /**
     * 构建设备主题
     */
    private String buildDeviceTopic(Device device) {
        return String.format("device/%s/prescription", device.getDeviceNo());
    }

    /**
     * 根据处方实体构建与用户提供结构一致的JSON字符串
     */
    private String buildPrescriptionEntityPayloadJson(Prescription prescription) throws Exception {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", prescription.getId());
        map.put("patientId", prescription.getPatientId());
        map.put("hospitalId", prescription.getHospitalId());
        map.put("doctorId", prescription.getDoctorId());
        map.put("deviceId", prescription.getDeviceId());
        map.put("presType", prescription.getPresType());
        map.put("status", prescription.getStatus());

        map.put("presStrength", prescription.getPresStrength());
        map.put("presFreq", prescription.getPresFreq());
        map.put("lastTime", prescription.getLastTime());
        map.put("pauseTime", prescription.getPauseTime());
        map.put("repeatCount", prescription.getRepeatCount());
        map.put("totalCount", prescription.getTotalCount());
        map.put("totalTime", prescription.getTotalTime());

        map.put("presPartId", prescription.getPresPartId());
        map.put("presPartName", prescription.getPresPartName());

        map.put("standardPresId", prescription.getStandardPresId());
        map.put("standardPresName", prescription.getStandardPresName());

        map.put("tbsType", prescription.getTbsType());
        map.put("innerCount", prescription.getInnerCount());
        map.put("interFreq", prescription.getInterFreq());
        map.put("interCount", prescription.getInterCount());

        map.put("periods", prescription.getPeriods());

        map.put("createdBy", prescription.getCreatedBy());
        // createdAt/updatedAt 为 java.util.Date，这里输出 ISO-8601 带时区偏移字符串（+08:00）
        if (prescription.getCreatedAt() != null) {
            map.put("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(prescription.getCreatedAt()));
        } else {
            map.put("createdAt", null);
        }
        map.put("updatedBy", prescription.getUpdatedBy());
        if (prescription.getUpdatedAt() != null) {
            map.put("updatedAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(prescription.getUpdatedAt()));
        } else {
            map.put("updatedAt", null);
        }

        return objectMapper.writeValueAsString(map);
    }

    /**
     * 发送取消消息
     */
    private void sendCancelMessage(Long prescriptionId, Long deviceId) {
        try {
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                CancelPrescriptionMessage cancelMessage = new CancelPrescriptionMessage();
                cancelMessage.setMessageId(UUID.randomUUID().toString());
                cancelMessage.setMessageType("CANCEL_PRESCRIPTION");
                cancelMessage.setTimestamp(LocalDateTime.now());
                cancelMessage.setPrescriptionId(prescriptionId);
                cancelMessage.setDeviceId(deviceId);
                cancelMessage.setDeviceNo(String.valueOf(device.getDeviceNo()));
                
                String payload = objectMapper.writeValueAsString(cancelMessage);
                String topic = buildDeviceTopic(device);
                this.tmsMqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .setHeader(MqttHeaders.QOS, 2)
                        .setHeader(MqttHeaders.RETAINED, false)
                        .build()
                );
                
                log.info("取消消息发送成功，处方ID: {}, 设备ID: {}", prescriptionId, deviceId);
            }
        } catch (Exception e) {
            log.error("发送取消消息失败，处方ID: {}, 设备ID: {}", prescriptionId, deviceId, e);
        }
    }

    /**
     * 内部类：处方MQTT消息
     */
    private static class PrescriptionMqttMessage {
        private String messageId;
        private String messageType;
        private LocalDateTime timestamp;
        private Long deviceId;
        private String deviceNo;
        private Long prescriptionId;
        private Long patientId;
        private Long doctorId;
        private Integer priority;
        private LocalDateTime executeTime;
        private Boolean executeImmediately;
        private String remark;
        
        // 处方参数
        private Integer presStrength;
        private java.math.BigDecimal presFreq;
        private java.math.BigDecimal lastTime;
        private Integer pauseTime;
        private Integer repeatCount;
        private Integer totalCount;
        private Integer totalTime;
        private String presPartName;
        private String standardPresName;
        
        // getter和setter方法
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        
        public String getDeviceNo() { return deviceNo; }
        public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }
        
        public Long getPrescriptionId() { return prescriptionId; }
        public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
        
        public Long getPatientId() { return patientId; }
        public void setPatientId(Long patientId) { this.patientId = patientId; }
        
        public Long getDoctorId() { return doctorId; }
        public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
        
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        
        public LocalDateTime getExecuteTime() { return executeTime; }
        public void setExecuteTime(LocalDateTime executeTime) { this.executeTime = executeTime; }
        
        public Boolean getExecuteImmediately() { return executeImmediately; }
        public void setExecuteImmediately(Boolean executeImmediately) { this.executeImmediately = executeImmediately; }
        
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        
        public Integer getPresStrength() { return presStrength; }
        public void setPresStrength(Integer presStrength) { this.presStrength = presStrength; }
        
        public java.math.BigDecimal getPresFreq() { return presFreq; }
        public void setPresFreq(java.math.BigDecimal presFreq) { this.presFreq = presFreq; }
        
        public java.math.BigDecimal getLastTime() { return lastTime; }
        public void setLastTime(java.math.BigDecimal lastTime) { this.lastTime = lastTime; }
        
        public Integer getPauseTime() { return pauseTime; }
        public void setPauseTime(Integer pauseTime) { this.pauseTime = pauseTime; }
        
        public Integer getRepeatCount() { return repeatCount; }
        public void setRepeatCount(Integer repeatCount) { this.repeatCount = repeatCount; }
        
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        
        public Integer getTotalTime() { return totalTime; }
        public void setTotalTime(Integer totalTime) { this.totalTime = totalTime; }
        
        public String getPresPartName() { return presPartName; }
        public void setPresPartName(String presPartName) { this.presPartName = presPartName; }
        
        public String getStandardPresName() { return standardPresName; }
        public void setStandardPresName(String standardPresName) { this.standardPresName = standardPresName; }
    }

    /**
     * 内部类：取消处方消息
     */
    private static class CancelPrescriptionMessage {
        private String messageId;
        private String messageType;
        private LocalDateTime timestamp;
        private Long prescriptionId;
        private Long deviceId;
        private String deviceNo;
        
        // getter和setter方法
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Long getPrescriptionId() { return prescriptionId; }
        public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
        
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        
        public String getDeviceNo() { return deviceNo; }
        public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }
    }
}
