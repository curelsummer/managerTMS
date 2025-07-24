package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.StandardPrescription;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

public interface StandardPrescriptionService extends IService<StandardPrescription> {
    // 分页查询标准处方
    IPage<StandardPrescription> findStandardPrescriptions(IPage<StandardPrescription> page, StandardPrescription prescription);
    
    // 根据治疗部位查询标准处方
    List<StandardPrescription> findByPartName(String partName);
} 