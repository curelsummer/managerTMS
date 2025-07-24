package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.Hospital;
import cc.mrbird.febs.system.dao.HospitalMapper;
import cc.mrbird.febs.system.service.HospitalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service("hospitalService")
public class HospitalServiceImpl extends ServiceImpl<HospitalMapper, Hospital> implements HospitalService {
    // 可扩展自定义方法
} 