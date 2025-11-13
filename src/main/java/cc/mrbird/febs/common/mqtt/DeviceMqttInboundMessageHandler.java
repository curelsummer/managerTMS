package cc.mrbird.febs.common.mqtt;

import cc.mrbird.febs.common.mqtt.handler.PatientInfoUpHandler;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeviceMqttInboundMessageHandler implements MessageHandler {

    private final PatientInfoUpHandler patientInfoUpHandler;
    private final MqttAuditLogger mqttAuditLogger;

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
        MqttTopics.TopicParts parts = MqttTopics.parse(topic);
        if (parts == null) {
            try { mqttAuditLogger.logInboundUnknown(topic, payload); } catch (Exception ignore) {}
            return;
        }
        switch (parts.getMsgType()) {
            case "patient-info-up":
                patientInfoUpHandler.handle(topic, payload);
                break;
            default:
                // 未识别类型：写入 unknown 审计日志
                try { mqttAuditLogger.logInboundUnknown(topic, payload); } catch (Exception ignore) {}
                break;
        }
    }

}
