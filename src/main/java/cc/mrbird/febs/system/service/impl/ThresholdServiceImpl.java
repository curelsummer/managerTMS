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
        // 读取新增字段 thresholdValue（映射到列 threshold_value）
        Integer threshold = patient.getThresholdValue();
        return threshold != null && threshold >= 0 && threshold <= 100;
    }
}


