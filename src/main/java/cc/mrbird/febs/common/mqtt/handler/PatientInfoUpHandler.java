package cc.mrbird.febs.common.mqtt.handler;

import cc.mrbird.febs.common.mqtt.MsgDedupService;
import cc.mrbird.febs.common.mqtt.MqttClientService;
import cc.mrbird.febs.common.mqtt.MqttTopics;
import cc.mrbird.febs.system.domain.Patient;
import cc.mrbird.febs.system.service.PatientService;
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
    private final PatientService patientService;
    private final ThresholdService thresholdService;
    private final PrescriptionService prescriptionService;
    private final MqttClientService mqttClientService;

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
            mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                               new HashMap<>(), true, -4, "missing_patient_id");
            return;
        }
        
        // 先检查患者是否存在
        Long pid;
        try {
            pid = Long.valueOf(patientId);
        } catch (NumberFormatException e) {
            mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                             new HashMap<>(), true, -3, "invalid_patient_id");
            return;
        }
        
        Patient patient = patientService.getById(pid);
        if (patient == null) {
            // 患者不存在
            mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                              new HashMap<>(), true, -2, "patient_not_found");
            return;
        }
        
        // 患者存在，检查是否有阈值
        boolean hasThreshold = thresholdService.existsPatientThreshold(patientId);
        if (!hasThreshold) {
            // 患者存在但没有阈值
            mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                              new HashMap<>(), true, -1, "no_threshold");
            return;
        }
        Map<String, Object> prescription = prescriptionService.getByPatient(patientId);
        if (prescription == null || prescription.isEmpty()) {
            mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                              new HashMap<>(), true, 0, null);
            return;
        }
        mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                          prescription, true, 0, null);
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode val = node.get(field);
        return val == null ? null : val.asText();
    }
}


