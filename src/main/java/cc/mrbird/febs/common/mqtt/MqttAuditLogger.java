package cc.mrbird.febs.common.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MqttAuditLogger {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("MQTT_AUDIT");
    private final ObjectMapper objectMapper;

    public void logInbound(Message<?> message) {
        String topic = String.valueOf(message.getHeaders().getOrDefault(MqttHeaders.RECEIVED_TOPIC, ""));
        int qos = (Integer) message.getHeaders().getOrDefault(MqttHeaders.RECEIVED_QOS, 0);
        boolean retained = (Boolean) message.getHeaders().getOrDefault("mqtt_receivedRetained", false);
        String payload = String.valueOf(message.getPayload());
        log("in", "device", topic, qos, retained, payload, null, null);
    }

    public void logOutbound(String topic, int qos, boolean retained, String payload, String result, String error) {
        log("out", "server", topic, qos, retained, payload, result, error);
    }

    public void logInboundUnknown(String topic, String payload) {
        // 记录一个额外的 unknown 标记行，便于快速筛选未识别类型
        MqttTopics.TopicParts parts = MqttTopics.parse(topic);
        String deviceType = parts == null ? "" : parts.getDeviceType();
        String deviceId = parts == null ? "" : parts.getDeviceId();
        String msgType = parts == null ? "" : parts.getMsgType();
        String msgId = extractMsgId(payload);
        int size = payload == null ? 0 : payload.getBytes(StandardCharsets.UTF_8).length;

        ObjectNode node = objectMapper.createObjectNode();
        node.put("ts", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        node.put("dir", "in");
        node.put("client", "device");
        node.put("topic", topic);
        node.put("qos", 0);
        node.put("retained", false);
        node.put("deviceType", deviceType);
        node.put("deviceId", deviceId);
        node.put("msgType", msgType);
        if (msgId != null) node.put("msgId", msgId);
        node.put("size", size);
        if (payload != null) node.put("messageBody", payload);
        node.put("unknown", true);
        MDC.put("direction", "inbound");
        AUDIT_LOGGER.info(compact(node));
        MDC.remove("direction");
    }

    private void log(String dir, String client, String topic, int qos, boolean retained,
                     String payload, String result, String error) {
        MqttTopics.TopicParts parts = MqttTopics.parse(topic);
        String deviceType = parts == null ? "" : parts.getDeviceType();
        String deviceId = parts == null ? "" : parts.getDeviceId();
        String msgType = parts == null ? "" : parts.getMsgType();
        String msgId = extractMsgId(payload);
        int size = payload == null ? 0 : payload.getBytes(StandardCharsets.UTF_8).length;

        ObjectNode node = objectMapper.createObjectNode();
        node.put("ts", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        node.put("dir", dir);
        node.put("client", client);
        node.put("topic", topic);
        node.put("qos", qos);
        node.put("retained", retained);
        node.put("deviceType", deviceType);
        node.put("deviceId", deviceId);
        node.put("msgType", msgType);
        if (msgId != null) node.put("msgId", msgId);
        node.put("size", size);
        if (payload != null) node.put("messageBody", payload);
        if (result != null) node.put("result", result);
        if (error != null) node.put("error", error);

        // 使用 MDC 控制 SiftingAppender 输出到 in/out 对应文件
        MDC.put("direction", "in".equals(dir) ? "inbound" : "outbound");
        AUDIT_LOGGER.info(compact(node));
        MDC.remove("direction");
    }

    private String extractMsgId(String payload) {
        if (payload == null || payload.isEmpty()) return null;
        try {
            Map<?, ?> m = objectMapper.readValue(payload, Map.class);
            Object v = m.get("msgId");
            if (v == null) {
                Object data = m.get("data");
                if (data instanceof Map) {
                    Object inner = ((Map<?, ?>) data).get("msgId");
                    return inner == null ? null : String.valueOf(inner);
                }
                return null;
            }
            return String.valueOf(v);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String compact(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return "{\"err\":\"json_serialize_failed\"}";
        }
    }
}


