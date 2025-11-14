package cc.mrbird.febs.common.mqtt.handler;

import cc.mrbird.febs.common.mqtt.MsgDedupService;
import cc.mrbird.febs.common.mqtt.MqttClientService;
import cc.mrbird.febs.common.mqtt.MqttTopics;
import cc.mrbird.febs.system.domain.Patient;
import cc.mrbird.febs.system.service.PatientService;
import cc.mrbird.febs.system.service.PrescriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdResultUpHandler {

    private final ObjectMapper objectMapper;
    private final MsgDedupService msgDedupService;
    private final PatientService patientService;
    private final PrescriptionService prescriptionService;
    private final MqttClientService mqttClientService;

    public void handle(String topic, String payload) {
        MqttTopics.TopicParts parts = MqttTopics.parse(topic);
        if (parts == null) {
            log.warn("MQTT threshold-result-up topic 解析失败: {}", topic);
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("MQTT threshold-result-up 解析失败: {}", e.getMessage());
            return;
        }
        String msgId = getText(root, "msgId");
        if (!msgDedupService.checkAndMark(msgId, Duration.ofMinutes(10))) {
            return;
        }
        JsonNode data = root.get("data");
        if (data == null) {
            log.warn("MQTT threshold-result-up 缺少 data 节点");
            return;
        }
        String patientIdStr = getText(data, "patientId");
        Integer thresholdValue = getInt(data, "thresholdValue");
        if (patientIdStr == null || patientIdStr.isEmpty() || thresholdValue == null) {
            log.warn("MQTT threshold-result-up 缺少 patientId 或 thresholdValue");
            return;
        }
        Long patientId;
        try {
            patientId = Long.valueOf(patientIdStr);
        } catch (NumberFormatException e) {
            log.warn("MQTT threshold-result-up 非法 patientId: {}", patientIdStr);
            return;
        }
        try {
            Patient patch = new Patient();
            patch.setId(patientId);
            patch.setThresholdValue(thresholdValue);
            patch.setThresholdSetAt(new Date());
            patch.setThresholdSource(parts.getDeviceId());
            // 仅按主键更新这些字段
            boolean ok = patientService.updateById(patch);
            if (!ok) {
                log.warn("MQTT threshold-result-up 更新患者阈值失败, patientId={}", patientId);
            }
        } catch (Exception e) {
            log.warn("MQTT threshold-result-up 更新患者阈值异常, patientId={}, err={}", patientId, e.getMessage());
            return;
        }
        try {
            Map<String, Object> prescription = prescriptionService.getByPatient(patientIdStr);
            if (prescription != null && !prescription.isEmpty()) {
                mqttClientService.sendPrescription(parts.getDeviceType(), parts.getDeviceId(), prescription, true);
            }
        } catch (Exception e) {
            log.warn("MQTT threshold-result-up 下发处方异常, patientId={}, err={}", patientId, e.getMessage());
        }
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode val = node.get(field);
        return val == null ? null : val.asText();
    }

    private Integer getInt(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode val = node.get(field);
        return val == null || val.isNull() ? null : val.asInt();
    }
}


