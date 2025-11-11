package cc.mrbird.febs.common.mqtt;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MqttClientService {

    // 处方/指令下发通道（由 PrescriptionMqttConfig 暴露）
    @Resource(name = "tmsMqttOutboundChannel")
    private MessageChannel outboundChannel;

    public void sendThresholdRequest(String deviceType, String deviceId, String reason) {
        String topic = MqttTopics.buildDownTopic(deviceType, deviceId, "threshold-request-down");
        Map<String, Object> payload = envelope(MapUtil.<String, Object>builder()
            .put("reason", reason)
            .build());
        publish(topic, payload, 1, false);
        log.info("MQTT 下发阈值请求 topic={}, reason={}", topic, reason);
    }

    public void sendPrescription(String deviceType, String deviceId, Map<String, Object> prescription, boolean retain) {
        String topic = MqttTopics.buildDownTopic(deviceType, deviceId, "prescription-down");
        Map<String, Object> payload = envelope(prescription != null ? prescription : new HashMap<>());
        publish(topic, payload, 1, retain);
        log.info("MQTT 下发处方 topic={}, retain={}, bodySize={}", topic, retain, payload.size());
    }

    private Map<String, Object> envelope(Map<String, Object> data) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("msgId", UUID.randomUUID().toString());
        wrapper.put("ts", System.currentTimeMillis());
        wrapper.put("ver", "1.0");
        wrapper.put("data", data == null ? new HashMap<>() : data);
        return wrapper;
    }

    private void publish(String topic, Object payload, int qos, boolean retained) {
        Message<?> msg = MessageBuilder.withPayload(payload)
            .setHeader(MqttHeaders.TOPIC, topic)
            .setHeader(MqttHeaders.QOS, qos)
            .setHeader(MqttHeaders.RETAINED, retained)
            .build();
        outboundChannel.send(msg);
    }
}


