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
            log.debug("MQTT patient-info-up 消息已处理，跳过: msgId={}", msgId);
            return;
        }
        String patientId = null;
        JsonNode data = root.get("data");
        if (data != null) {
            patientId = getText(data, "patientId");
        }
        if (patientId == null || patientId.isEmpty()) {
            log.warn("MQTT patient-info-up 缺少 patientId");
            try {
                mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                                   new HashMap<>(), true, -4, "missing_patient_id");
            } catch (Exception e) {
                log.error("MQTT patient-info-up 发送处方失败 (missing_patient_id): {}", e.getMessage(), e);
            }
            return;
        }
        
        // 先检查患者是否存在
        Long pid;
        try {
            pid = Long.valueOf(patientId);
        } catch (NumberFormatException e) {
            log.warn("MQTT patient-info-up 非法 patientId: {}", patientId);
            try {
                mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                                 new HashMap<>(), true, -3, "invalid_patient_id");
            } catch (Exception ex) {
                log.error("MQTT patient-info-up 发送处方失败 (invalid_patient_id): {}", ex.getMessage(), ex);
            }
            return;
        }
        
        Patient patient = patientService.getById(pid);
        if (patient == null) {
            // 患者不存在
            log.warn("MQTT patient-info-up 患者不存在: patientId={}", patientId);
            try {
                mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                                  new HashMap<>(), true, -2, "patient_not_found");
            } catch (Exception e) {
                log.error("MQTT patient-info-up 发送处方失败 (patient_not_found): {}", e.getMessage(), e);
            }
            return;
        }
        
        // 患者存在，检查是否有阈值
        boolean hasThreshold = thresholdService.existsPatientThreshold(patientId);
        if (!hasThreshold) {
            // 患者存在但没有阈值
            log.warn("MQTT patient-info-up 患者没有阈值: patientId={}", patientId);
            try {
                mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                                  new HashMap<>(), true, -1, "no_threshold");
            } catch (Exception e) {
                log.error("MQTT patient-info-up 发送处方失败 (no_threshold): {}", e.getMessage(), e);
            }
            return;
        }
        
        try {
            Map<String, Object> prescription = prescriptionService.getByPatient(patientId);
            if (prescription == null || prescription.isEmpty()) {
                log.info("MQTT patient-info-up 患者没有处方: patientId={}", patientId);
                mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                                  new HashMap<>(), true, 0, null);
            } else {
                log.info("MQTT patient-info-up 发送处方: patientId={}, deviceType={}, deviceId={}", 
                        patientId, p.getDeviceType(), p.getDeviceId());
                mqttClientService.sendPrescription(p.getDeviceType(), p.getDeviceId(), 
                                                  prescription, true, 0, null);
            }
        } catch (Exception e) {
            log.error("MQTT patient-info-up 处理失败: patientId={}, error={}", patientId, e.getMessage(), e);
        }
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode val = node.get(field);
        return val == null ? null : val.asText();
    }
}


