package cc.mrbird.febs.common.mqtt.handler;

import cc.mrbird.febs.common.mqtt.MsgDedupService;
import cc.mrbird.febs.common.mqtt.MqttClientService;
import cc.mrbird.febs.common.mqtt.MqttTopics;
import cc.mrbird.febs.cos.service.NotificationService;
import cc.mrbird.febs.system.service.PrescriptionService;
import cc.mrbird.febs.system.service.ThresholdService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientInfoUpHandler {

    private final ObjectMapper objectMapper;
    private final MsgDedupService msgDedupService;
    private final ThresholdService thresholdService;
    private final PrescriptionService prescriptionService;
    private final MqttClientService mqttClientService;
    private final NotificationService notificationService;

    public void handle(String topic, String payload) {
        MqttTopics.TopicParts p = MqttTopics.parse(topic);
        if (p == null) {
            log.warn("MQTT patient-info-up topic 解析失败: {}", topic);
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("MQTT patient-info-up 解析失败: {}", e.getMessage());
            return;
        }
        String msgId = getText(root, "msgId");
        if (!msgDedupService.checkAndMark(msgId, Duration.ofMinutes(10))) {
            return;
        }
        String patientId = null;
        JsonNode data = root.get("data");
        if (data != null) {
            patientId = getText(data, "patientId");
        }
        if (patientId == null || patientId.isEmpty()) {
            mqttClientService.sendThresholdRequest(p.getDeviceType(), p.getDeviceId(), "missing_patient_id");
            return;
        }
        boolean hasThreshold = thresholdService.existsPatientThreshold(patientId);
        if (!hasThreshold) {
            mqttClientService.sendThresholdRequest(p.getDeviceType(), p.getDeviceId(), "no_patient_or_threshold");
            return;
        }
        Map<String, Object> prescription = prescriptionService.getByPatient(patientId);
        if (prescription == null || prescription.isEmpty()) {
            mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), new HashMap<>(), true);
            try {
                notificationService.notifyDoctor(patientId, "患者缺少处方，请尽快设定。");
            } catch (Exception ignored) {
            }
            return;
        }
        mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), prescription, true);
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode val = node.get(field);
        return val == null ? null : val.asText();
    }
}


