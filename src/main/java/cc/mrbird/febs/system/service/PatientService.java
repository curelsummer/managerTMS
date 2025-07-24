package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.Patient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PatientService extends IService<Patient> {
    // 可根据需要添加自定义方法
    IPage<Patient> findPatients(IPage<Patient> page, Patient patient);
    IPage<Patient> findPatients(IPage<Patient> page, Patient patient, Long userId);
} 