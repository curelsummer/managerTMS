package cc.mrbird.febs.common.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttClientService {

    // 处方/指令下发通道（由 PrescriptionMqttConfig 暴露）
    @Autowired(required = false)
    @Qualifier("tmsMqttOutboundChannel")
    private MessageChannel outboundChannel;

    private final MqttAuditLogger mqttAuditLogger;

    public void sendThresholdRequest(String deviceType, String deviceId, String reason) {
        // 根据 reason 映射到对应的 resultcode，然后调用 sendPrescription
        Integer resultcode = mapReasonToResultCode(reason);
        sendPrescription(deviceType, deviceId, new HashMap<>(), true, resultcode, reason);
    }

    /**
     * 发送处方消息（支持 resultcode 和 reason）
     * @param deviceType 设备类型
     * @param deviceId 设备ID
     * @param prescription 处方数据
     * @param retain 是否保留消息
     * @param resultcode 结果码：0=成功，-1=无阈值，-2=患者不存在，-3=patientId格式错误，-4=缺少patientId
     * @param reason 错误原因（仅在 resultcode < 0 时有效）
     */
    public void sendPrescription(String deviceType, String deviceId, Map<String, Object> prescription, 
                                boolean retain, Integer resultcode, String reason) {
        String topic = MqttTopics.buildDownTopic(deviceType, deviceId, "prescription-down");
        
        // 构建 data 内容
        Map<String, Object> data = new HashMap<>();
        if (prescription != null && !prescription.isEmpty()) {
            data.putAll(prescription);
            // 确保 patientId 使用字符串格式
            if (data.containsKey("patientId") && data.get("patientId") != null) {
                data.put("patientId", String.valueOf(data.get("patientId")));
            }
        }
        // 如果是错误情况，将 reason 放入 data 中
        if (resultcode != null && resultcode < 0 && reason != null) {
            data.put("reason", reason);
        }
        
        // 使用专门的处方消息包装方法
        Map<String, Object> payload = envelopePrescription(data, resultcode);
        publish(topic, payload, 1, retain);
        log.info("MQTT 下发处方 topic={}, retain={}, resultcode={}, reason={}", 
                 topic, retain, resultcode, reason);
    }

    /**
     * 发送处方消息（向后兼容的重载方法，默认 resultcode=0）
     */
    public void sendPrescription(String deviceType, String deviceId, Map<String, Object> prescription, boolean retain) {
        sendPrescription(deviceType, deviceId, prescription, retain, 0, null);
    }

    /**
     * 专门用于处方消息的包装方法，添加 resultcode 字段
     */
    private Map<String, Object> envelopePrescription(Map<String, Object> data, Integer resultcode) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("msgId", UUID.randomUUID().toString());
        wrapper.put("ts", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        wrapper.put("ver", "1.0");
        wrapper.put("resultcode", resultcode != null ? resultcode : 0);
        wrapper.put("data", data == null ? new HashMap<>() : data);
        return wrapper;
    }

    /**
     * 通用消息包装方法（保持原样，不影响其他主题）
     */
    private Map<String, Object> envelope(Map<String, Object> data) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("msgId", UUID.randomUUID().toString());
        wrapper.put("ts", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        wrapper.put("ver", "1.0");
        wrapper.put("data", data == null ? new HashMap<>() : data);
        return wrapper;
    }

    /**
     * 将 reason 映射到对应的 resultcode
     */
    private Integer mapReasonToResultCode(String reason) {
        if (reason == null) return 0;
        switch (reason) {
            case "missing_patient_id":
                return -4;
            case "invalid_patient_id":
                return -3;
            case "patient_not_found":
                return -2;
            case "no_threshold":
                return -1;
            default:
                return 0;
        }
    }

    private void publish(String topic, Object payload, int qos, boolean retained) {
        // 将 payload 转换为 JSON 字符串，因为 MQTT 消息处理器只支持 byte[] 或 String
        String jsonPayload = toJsonString(payload);
        Message<?> msg = MessageBuilder.withPayload(jsonPayload)
            .setHeader(MqttHeaders.TOPIC, topic)
            .setHeader(MqttHeaders.QOS, qos)
            .setHeader(MqttHeaders.RETAINED, retained)
            .build();
        try {
            if (outboundChannel != null) {
                outboundChannel.send(msg);
                mqttAuditLogger.logOutbound(topic, qos, retained, jsonPayload, "ok", null);
            } else {
                mqttAuditLogger.logOutbound(topic, qos, retained, jsonPayload, "skip", "tmsMqttOutboundChannel is not configured");
                log.warn("MQTT 下发已跳过：未配置 mqtt-tms.url，topic={}", topic);
            }
        } catch (Exception e) {
            mqttAuditLogger.logOutbound(topic, qos, retained, jsonPayload, "fail", e.getMessage());
            throw e;
        }
    }

    private String toJsonString(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}


