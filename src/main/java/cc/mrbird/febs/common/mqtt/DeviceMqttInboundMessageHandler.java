package cc.mrbird.febs.common.mqtt;

import cc.mrbird.febs.common.mqtt.handler.PatientInfoUpHandler;
import cc.mrbird.febs.common.mqtt.handler.DeviceInfoUpHandler;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeviceMqttInboundMessageHandler implements MessageHandler {

    private final PatientInfoUpHandler patientInfoUpHandler;
    private final cc.mrbird.febs.common.mqtt.handler.ThresholdResultUpHandler thresholdResultUpHandler;
    private final DeviceInfoUpHandler deviceInfoUpHandler;
    private final MqttAuditLogger mqttAuditLogger;
    private final ObjectMapper objectMapper;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        // 审计日志（入站）
        try {
            mqttAuditLogger.logInbound(message);
        } catch (Exception ignore) {
        }

        if (message.getPayload() == null || StrUtil.isEmpty(message.getPayload().toString())) {
            return;
        }
        String topic = String.valueOf(message.getHeaders().getOrDefault("mqtt_receivedTopic", ""));
        String payload = message.getPayload().toString();
        if (StrUtil.isBlank(topic)) {
            // 主题异常也记为 unknown
            try { mqttAuditLogger.logInboundUnknown(topic, payload); } catch (Exception ignore) {}
            return;
        }
        
        // 检查并填充时间字段（ts）
        try {
            payload = fillTimestampIfMissing(payload);
        } catch (Exception e) {
            log.warn("填充时间字段失败，使用原始消息: {}", e.getMessage());
            // 继续处理，使用原始payload
        }
        
        MqttTopics.TopicParts parts = MqttTopics.parse(topic);
        if (parts == null) {
            try { mqttAuditLogger.logInboundUnknown(topic, payload); } catch (Exception ignore) {}
            return;
        }
        switch (parts.getMsgType()) {
            case "patient-info-up":
                patientInfoUpHandler.handle(topic, payload);
                break;
            case "threshold-result-up":
                thresholdResultUpHandler.handle(topic, payload);
                break;
            case "device-info-up":
                deviceInfoUpHandler.handle(topic, payload);
                break;
            default:
                // 未识别类型：写入 unknown 审计日志
                try { mqttAuditLogger.logInboundUnknown(topic, payload); } catch (Exception ignore) {}
                break;
        }
    }

    /**
     * 检查消息中的 ts 字段，如果为空或不存在，则填充当前时间
     * @param payload 原始消息JSON字符串
     * @return 处理后的消息JSON字符串
     */
    private String fillTimestampIfMissing(String payload) throws Exception {
        if (StrUtil.isBlank(payload)) {
            return payload;
        }
        
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root == null || !root.isObject()) {
                return payload;
            }
            
            ObjectNode objectNode = (ObjectNode) root;
            JsonNode tsNode = objectNode.get("ts");
            
            // 检查 ts 字段是否为空或不存在
            boolean needFill = false;
            if (tsNode == null || tsNode.isNull()) {
                needFill = true;
            } else if (tsNode.isTextual()) {
                String tsValue = tsNode.asText();
                if (StrUtil.isBlank(tsValue)) {
                    needFill = true;
                }
            }
            
            // 如果需要填充，则设置当前时间（ISO-8601格式，带时区偏移）
            if (needFill) {
                String currentTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                objectNode.put("ts", currentTime);
                log.debug("填充消息时间字段: ts={}", currentTime);
                return objectMapper.writeValueAsString(objectNode);
            }
            
            return payload;
        } catch (Exception e) {
            // 如果解析失败，返回原始payload
            log.warn("解析消息JSON失败，跳过时间字段填充: {}", e.getMessage());
            return payload;
        }
    }

}
