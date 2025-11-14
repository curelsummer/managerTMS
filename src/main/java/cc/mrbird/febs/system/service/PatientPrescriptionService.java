package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.PatientPrescription;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PatientPrescriptionService extends IService<PatientPrescription> {

    /**
     * 记录一次“患者-处方”的使用：
     * - 如不存在关系则插入（created_at=now, last_used_at=now）
     * - 如已存在则仅更新 last_used_at=now
     * 需处理并发下的唯一约束冲突（去重）。
     */
    void recordUsage(Long patientId, Long prescriptionId);

    /**
     * 建立“患者-处方”绑定关系（网页端维护）：
     * - 如不存在则插入（created_at=now, last_used_at=null）
     * - 如已存在则不变（不更新 created_at/last_used_at）
     * 需处理并发下的唯一约束冲突（去重）。
     */
    void recordBind(Long patientId, Long prescriptionId);
}


