package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.Prescription;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface PrescriptionService extends IService<Prescription> {

    IPage<Prescription> findPrescriptions(IPage<Prescription> page, Prescription prescription, Long userId);

    /**
     * 按患者ID查询处方，返回下发所需结构（可为空）
     */
    Map<String, Object> getByPatient(String patientId);
    
    /**
     * 增加处方使用次数
     * @param prescriptionId 处方ID
     * @return 是否成功
     */
    boolean incrementUsageCount(Long prescriptionId);
} 