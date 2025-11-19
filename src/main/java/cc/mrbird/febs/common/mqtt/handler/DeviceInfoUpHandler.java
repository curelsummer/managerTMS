package cc.mrbird.febs.common.mqtt.handler;

import cc.mrbird.febs.common.mqtt.MsgDedupService;
import cc.mrbird.febs.common.mqtt.MqttTopics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备指标数据上报处理器
 *
 * @author FanK
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceInfoUpHandler {

    private final ObjectMapper objectMapper;
    private final MsgDedupService msgDedupService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String QUEUE_KEY = "mqtt:queue:device-metrics";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void handle(String topic, String payload) {
        MqttTopics.TopicParts parts = MqttTopics.parse(topic);
        if (parts == null) {
            log.warn("MQTT device-info-up topic 解析失败: {}", topic);
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("MQTT device-info-up 解析失败: {}", e.getMessage());
            return;
        }

        // 消息去重检查
        String msgId = getText(root, "msgId");
        if (msgId != null && !msgId.isEmpty()) {
            if (!msgDedupService.checkAndMark(msgId, Duration.ofMinutes(10))) {
                log.debug("消息已处理，跳过: msgId={}", msgId);
                return;
            }
        }

        // 解析数据
        JsonNode data = root.get("data");
        if (data == null) {
            log.warn("MQTT device-info-up 缺少 data 节点");
            return;
        }

        // 提取指标数据
        String coilTempStr = getText(data, "coilTemperature");
        String machineTempStr = getText(data, "machineTemperature");
        String pumpFlowRateStr = getText(data, "pumpFlowRate");
        String timestampStr = getText(data, "timestamp");

        // 构建消息对象
        Map<String, Object> message = new HashMap<>();
        message.put("deviceType", parts.getDeviceType());
        message.put("deviceId", parts.getDeviceId());
        message.put("msgId", msgId);
        message.put("coilTemperature", coilTempStr);
        message.put("machineTemperature", machineTempStr);
        message.put("pumpFlowRate", pumpFlowRateStr);
        message.put("createTime", timestampStr != null ? timestampStr : LocalDateTime.now().format(DATETIME_FORMATTER));
        message.put("serverTime", LocalDateTime.now().format(DATETIME_FORMATTER));

        // 推入Redis队列
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, messageJson);
            log.debug("设备指标数据已入队: deviceType={}, deviceId={}, msgId={}", 
                    parts.getDeviceType(), parts.getDeviceId(), msgId);
        } catch (Exception e) {
            log.error("设备指标数据入队失败: deviceType={}, deviceId={}", 
                    parts.getDeviceType(), parts.getDeviceId(), e);
        }
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode val = node.get(field);
        if (val == null) return null;
        if (val.isNull()) return null;
        if (val.isTextual()) return val.asText();
        return val.toString();
    }
}

