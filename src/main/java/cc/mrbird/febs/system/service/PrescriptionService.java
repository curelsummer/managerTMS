package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.Prescription;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface PrescriptionService extends IService<Prescription> {
    // 自定义分页查询方法，包含权限控制
    IPage<Prescription> findPrescriptions(IPage<Prescription> page, Prescription prescription, Long userId);
} 