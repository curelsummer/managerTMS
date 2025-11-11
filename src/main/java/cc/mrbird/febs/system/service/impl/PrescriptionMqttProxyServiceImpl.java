package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.common.mqtt.DeviceMqttProperties;
import cc.mrbird.febs.system.domain.Device;
import cc.mrbird.febs.system.service.DeviceService;
import cc.mrbird.febs.system.service.PrescriptionMqttProxyService;
import cc.mrbird.febs.system.dto.PrescriptionDeviceRequest;
import cc.mrbird.febs.system.dto.PrescriptionDeviceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处方MQTT代理服务实现类
 * 负责管理MQTT消息的发送、接收和状态管理
 */
@Slf4j
@Service
public class PrescriptionMqttProxyServiceImpl implements PrescriptionMqttProxyService {

    private final DeviceService deviceService;
    private final DeviceMqttProperties mqttProperties;
    private final MessageChannel toiletMqttOutboundChannel;
    
    public PrescriptionMqttProxyServiceImpl(
            DeviceService deviceService,
            DeviceMqttProperties mqttProperties,
            @Lazy @Qualifier("toiletMqttOutboundChannel") MessageChannel toiletMqttOutboundChannel
    ) {
        this.deviceService = deviceService;
        this.mqttProperties = mqttProperties;
        this.toiletMqttOutboundChannel = toiletMqttOutboundChannel;
    }
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 设备连接状态缓存
    private final ConcurrentHashMap<Long, DeviceConnectionStatus> deviceConnectionCache = new ConcurrentHashMap<>();
    
    // 设备心跳时间缓存
    private final ConcurrentHashMap<Long, LocalDateTime> deviceHeartbeatCache = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<PrescriptionDeviceResponse> sendPrescriptionMessage(PrescriptionDeviceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始发送处方消息，处方ID: {}, 设备ID: {}", request.getPrescriptionId(), request.getDeviceId());
                
                // 获取设备信息
                Device device = deviceService.getById(request.getDeviceId());
                if (device == null) {
                    return PrescriptionDeviceResponse.failure(request.getPrescriptionId(), request.getDeviceId(), "设备不存在");
                }
                
                // 检查设备连接状态
                if (!isDeviceConnected(request.getDeviceId())) {
                    return PrescriptionDeviceResponse.failure(request.getPrescriptionId(), request.getDeviceId(), "设备未连接");
                }
                
                // 构建MQTT消息
                String messageId = UUID.randomUUID().toString();
                PrescriptionMqttMessage mqttMessage = buildPrescriptionMqttMessage(request, device, messageId);
                
                // 发送消息
                String payload = objectMapper.writeValueAsString(mqttMessage);
                String topic = buildDeviceTopic(device);
                
                // 发送到MQTT输出通道（显式指定主题，避免落入 defaultTopic）
                toiletMqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .setHeader(MqttHeaders.QOS, 2)
                        .setHeader(MqttHeaders.RETAINED, false)
                        .build()
                );
                
                // 创建成功响应
                PrescriptionDeviceResponse response = PrescriptionDeviceResponse.success(
                    request.getPrescriptionId(), request.getDeviceId(), messageId);
                response.setDeviceResponse(payload);
                
                log.info("处方消息发送成功，消息ID: {}, 主题: {}", messageId, topic);
                return response;
                
            } catch (Exception e) {
                log.error("发送处方消息失败，处方ID: {}, 设备ID: {}", request.getPrescriptionId(), request.getDeviceId(), e);
                return PrescriptionDeviceResponse.failure(request.getPrescriptionId(), request.getDeviceId(), e.getMessage());
            }
        });
    }

    @Override
    public boolean sendCancelMessage(Long prescriptionId, Long deviceId) {
        try {
            Device device = deviceService.getById(deviceId);
            if (device == null) {
                log.error("设备不存在，设备ID: {}", deviceId);
                return false;
            }
            
                    // 构建取消消息
        CancelPrescriptionMessage cancelMessage = new CancelPrescriptionMessage();
        cancelMessage.setMessageId(UUID.randomUUID().toString());
        cancelMessage.setMessageType("CANCEL_PRESCRIPTION");
        cancelMessage.setTimestamp(LocalDateTime.now());
        cancelMessage.setPrescriptionId(prescriptionId);
        cancelMessage.setDeviceId(deviceId);
        cancelMessage.setDeviceNo(String.valueOf(device.getDeviceNo()));
            
            // 发送消息
            String payload = objectMapper.writeValueAsString(cancelMessage);
            String topic = buildDeviceTopic(device);
            toiletMqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, 2)
                    .setHeader(MqttHeaders.RETAINED, false)
                    .build()
            );
            
            log.info("取消消息发送成功，处方ID: {}, 设备ID: {}", prescriptionId, deviceId);
            return true;
            
        } catch (Exception e) {
            log.error("发送取消消息失败，处方ID: {}, 设备ID: {}", prescriptionId, deviceId, e);
            return false;
        }
    }

    @Override
    public boolean sendStatusQueryMessage(Long deviceId) {
        try {
            Device device = deviceService.getById(deviceId);
            if (device == null) {
                log.error("设备不存在，设备ID: {}", deviceId);
                return false;
            }
            
                    // 构建状态查询消息
        StatusQueryMessage statusQueryMessage = new StatusQueryMessage();
        statusQueryMessage.setMessageId(UUID.randomUUID().toString());
        statusQueryMessage.setMessageType("STATUS_QUERY");
        statusQueryMessage.setTimestamp(LocalDateTime.now());
        statusQueryMessage.setDeviceId(deviceId);
        statusQueryMessage.setDeviceNo(String.valueOf(device.getDeviceNo()));
            
            // 发送消息
            String payload = objectMapper.writeValueAsString(statusQueryMessage);
            String topic = buildDeviceTopic(device);
            toiletMqttOutboundChannel.send(
                MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, 2)
                    .setHeader(MqttHeaders.RETAINED, false)
                    .build()
            );
            
            log.info("状态查询消息发送成功，设备ID: {}", deviceId);
            return true;
            
        } catch (Exception e) {
            log.error("发送状态查询消息失败，设备ID: {}", deviceId, e);
            return false;
        }
    }

    @Override
    public void handleDeviceResponse(Long deviceId, String message) {
        try {
            log.info("接收到设备响应，设备ID: {}, 消息: {}", deviceId, message);
            
            // 更新设备心跳时间
            deviceHeartbeatCache.put(deviceId, LocalDateTime.now());
            
            // 更新设备连接状态
            DeviceConnectionStatus status = deviceConnectionCache.get(deviceId);
            if (status == null) {
                status = new DeviceConnectionStatus();
                status.setDeviceId(deviceId);
                status.setConnected(true);
                status.setLastHeartbeat(LocalDateTime.now());
                deviceConnectionCache.put(deviceId, status);
            } else {
                status.setConnected(true);
                status.setLastHeartbeat(LocalDateTime.now());
            }
            
            // 解析响应消息
            DeviceResponseMessage responseMessage = objectMapper.readValue(message, DeviceResponseMessage.class);
            
            // 根据消息类型处理
            switch (responseMessage.getMessageType()) {
                case "PRESCRIPTION_CONFIRM":
                    handlePrescriptionConfirm(deviceId, responseMessage);
                    break;
                case "PRESCRIPTION_REJECT":
                    handlePrescriptionReject(deviceId, responseMessage);
                    break;
                case "DEVICE_STATUS":
                    handleDeviceStatus(deviceId, responseMessage);
                    break;
                case "HEARTBEAT":
                    handleHeartbeat(deviceId, responseMessage);
                    break;
                default:
                    log.warn("未知的消息类型: {}", responseMessage.getMessageType());
            }
            
        } catch (Exception e) {
            log.error("处理设备响应失败，设备ID: {}, 消息: {}", deviceId, message, e);
        }
    }

    @Override
    public boolean isDeviceConnected(Long deviceId) {
        DeviceConnectionStatus status = deviceConnectionCache.get(deviceId);
        if (status == null) {
            return false;
        }
        
        // 检查心跳是否超时（超过30秒没有心跳则认为断开）
        LocalDateTime lastHeartbeat = status.getLastHeartbeat();
        if (lastHeartbeat != null && lastHeartbeat.plusSeconds(30).isBefore(LocalDateTime.now())) {
            status.setConnected(false);
            return false;
        }
        
        return status.isConnected();
    }

    @Override
    public LocalDateTime getDeviceLastHeartbeat(Long deviceId) {
        return deviceHeartbeatCache.get(deviceId);
    }

    /**
     * 处理处方确认消息
     */
    private void handlePrescriptionConfirm(Long deviceId, DeviceResponseMessage responseMessage) {
        log.info("设备确认处方，设备ID: {}, 处方ID: {}", deviceId, responseMessage.getPrescriptionId());
        // 这里可以更新处方状态或通知其他服务
    }

    /**
     * 处理处方拒绝消息
     */
    private void handlePrescriptionReject(Long deviceId, DeviceResponseMessage responseMessage) {
        log.warn("设备拒绝处方，设备ID: {}, 处方ID: {}, 原因: {}", 
            deviceId, responseMessage.getPrescriptionId(), responseMessage.getReason());
        // 这里可以更新处方状态或通知其他服务
    }

    /**
     * 处理设备状态消息
     */
    private void handleDeviceStatus(Long deviceId, DeviceResponseMessage responseMessage) {
        log.info("设备状态更新，设备ID: {}, 状态: {}", deviceId, responseMessage.getStatus());
        // 这里可以更新设备状态
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(Long deviceId, DeviceResponseMessage responseMessage) {
        log.debug("设备心跳，设备ID: {}", deviceId);
        // 心跳已在handleDeviceResponse中处理
    }

    /**
     * 构建处方MQTT消息
     */
    private PrescriptionMqttMessage buildPrescriptionMqttMessage(PrescriptionDeviceRequest request, Device device, String messageId) {
        PrescriptionMqttMessage message = new PrescriptionMqttMessage();
        message.setMessageId(messageId);
        message.setMessageType("PRESCRIPTION");
        message.setTimestamp(LocalDateTime.now());
        message.setDeviceId(device.getDeviceId());
        message.setDeviceNo(String.valueOf(device.getDeviceNo()));
        message.setPrescriptionId(request.getPrescriptionId());
        message.setPatientId(request.getPatientId());
        message.setDoctorId(request.getDoctorId());
        message.setPriority(request.getPriority());
        message.setExecuteTime(request.getExecuteTime());
        message.setExecuteImmediately(request.getExecuteImmediately());
        message.setRemark(request.getRemark());
        return message;
    }

    /**
     * 构建设备主题
     */
    private String buildDeviceTopic(Device device) {
        return String.format("device/%s/prescription", device.getDeviceNo());
    }

    // 内部消息类
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
    }

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

    private static class StatusQueryMessage {
        private String messageId;
        private String messageType;
        private LocalDateTime timestamp;
        private Long deviceId;
        private String deviceNo;
        
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
    }

    private static class DeviceResponseMessage {
        private String messageId;
        private String messageType;
        private LocalDateTime timestamp;
        private Long deviceId;
        private String deviceNo;
        private Long prescriptionId;
        private String status;
        private String reason;
        
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
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    private static class DeviceConnectionStatus {
        private Long deviceId;
        private boolean connected;
        private LocalDateTime lastHeartbeat;
        
        // getter和setter方法
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        
        public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    }
}
