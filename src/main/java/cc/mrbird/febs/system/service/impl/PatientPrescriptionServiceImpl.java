package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.dao.PatientPrescriptionMapper;
import cc.mrbird.febs.system.domain.PatientPrescription;
import cc.mrbird.febs.system.service.PatientPrescriptionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class PatientPrescriptionServiceImpl extends ServiceImpl<PatientPrescriptionMapper, PatientPrescription>
        implements PatientPrescriptionService {

    @Override
    public void recordUsage(Long patientId, Long prescriptionId) {
        if (patientId == null || prescriptionId == null) {
            return;
        }
        Date now = new Date();
        // 1) 尝试更新已存在记录的 last_used_at
        int updated = this.baseMapper.update(null, new LambdaUpdateWrapper<PatientPrescription>()
                .eq(PatientPrescription::getPatientId, patientId)
                .eq(PatientPrescription::getPrescriptionId, prescriptionId)
                .set(PatientPrescription::getLastUsedAt, now));
        if (updated > 0) {
            return;
        }
        // 2) 不存在则尝试插入（并发下可能触发唯一键冲突）
        PatientPrescription pp = new PatientPrescription();
        pp.setPatientId(patientId);
        pp.setPrescriptionId(prescriptionId);
        pp.setCreatedAt(now);
        pp.setLastUsedAt(now);
        try {
            this.baseMapper.insert(pp);
        } catch (DuplicateKeyException e) {
            // 3) 并发下二次保证：转回更新 last_used_at
            this.baseMapper.update(null, new LambdaUpdateWrapper<PatientPrescription>()
                    .eq(PatientPrescription::getPatientId, patientId)
                    .eq(PatientPrescription::getPrescriptionId, prescriptionId)
                    .set(PatientPrescription::getLastUsedAt, now));
        } catch (Exception e) {
            log.warn("recordUsage 插入患者处方关系失败 patientId={}, prescriptionId={}, err={}", patientId, prescriptionId, e.getMessage());
        }
    }

    @Override
    public void recordBind(Long patientId, Long prescriptionId) {
        if (patientId == null || prescriptionId == null) {
            return;
        }
        // 已存在直接返回
        Integer exists = this.lambdaQuery()
                .eq(PatientPrescription::getPatientId, patientId)
                .eq(PatientPrescription::getPrescriptionId, prescriptionId)
                .count();
        if (exists != null && exists > 0) {
            return;
        }
        PatientPrescription pp = new PatientPrescription();
        pp.setPatientId(patientId);
        pp.setPrescriptionId(prescriptionId);
        pp.setCreatedAt(new Date());
        pp.setLastUsedAt(null);
        try {
            this.baseMapper.insert(pp);
        } catch (DuplicateKeyException e) {
            // 并发下已被其他请求创建，忽略
        } catch (Exception e) {
            log.warn("recordBind 插入患者处方关系失败 patientId={}, prescriptionId={}, err={}", patientId, prescriptionId, e.getMessage());
        }
    }
}


