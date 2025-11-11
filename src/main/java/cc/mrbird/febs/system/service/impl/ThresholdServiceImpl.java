package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.dao.PatientMapper;
import cc.mrbird.febs.system.domain.Patient;
import cc.mrbird.febs.system.service.ThresholdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 阈值存在性判断（示例实现：根据患者表中的阈值字段是否有值）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThresholdServiceImpl implements ThresholdService {

    private final PatientMapper patientMapper;

    @Override
    public boolean existsPatientThreshold(String patientId) {
        if (!StringUtils.hasText(patientId)) {
            return false;
        }
        Long pid;
        try {
            pid = Long.valueOf(patientId);
        } catch (NumberFormatException e) {
            return false;
        }
        Patient patient = patientMapper.selectById(pid);
        if (patient == null) {
            return false;
        }
        // 这里假设 Patient 增加了阈值字段，可按你的实际字段名替换
        // 示例：扩展字段通过 getCode() 暂代（仅占位，建议改为真实字段）
        // 判定逻辑：存在且在 0-100 范围内
        Integer threshold = null;
        // TODO: 使用真实字段，例如 patient.getThreshold()
        if (threshold == null) {
            return false;
        }
        return threshold >= 0 && threshold <= 100;
    }
}


