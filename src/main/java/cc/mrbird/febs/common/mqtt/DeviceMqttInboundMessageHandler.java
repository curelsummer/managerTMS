package cc.mrbird.febs.common.mqtt;

import cc.mrbird.febs.common.mqtt.handler.PatientInfoUpHandler;
import cc.mrbird.febs.cos.service.IDeviceTypeService;
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

    private final IDeviceTypeService deviceTypeService;
    private final PatientInfoUpHandler patientInfoUpHandler;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        if (message.getPayload() == null || StrUtil.isEmpty(message.getPayload().toString())) {
            return;
        }
        String topic = String.valueOf(message.getHeaders().getOrDefault("mqtt_receivedTopic", ""));
        String payload = message.getPayload().toString();
        if (StrUtil.isBlank(topic)) {
            deviceTypeService.setDeviceRecordMqtt(payload);
            return;
        }
        MqttTopics.TopicParts parts = MqttTopics.parse(topic);
        if (parts == null) {
            deviceTypeService.setDeviceRecordMqtt(payload);
            return;
        }
        switch (parts.getMsgType()) {
            case "patient-info-up":
                patientInfoUpHandler.handle(topic, payload);
                break;
            default:
                deviceTypeService.setDeviceRecordMqtt(payload);
        }
    }

}
