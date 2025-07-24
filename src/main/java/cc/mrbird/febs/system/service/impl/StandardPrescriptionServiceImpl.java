package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.StandardPrescription;
import cc.mrbird.febs.system.dao.StandardPrescriptionMapper;
import cc.mrbird.febs.system.service.StandardPrescriptionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service("standardPrescriptionService")
public class StandardPrescriptionServiceImpl extends ServiceImpl<StandardPrescriptionMapper, StandardPrescription> implements StandardPrescriptionService {

    @Override
    public IPage<StandardPrescription> findStandardPrescriptions(IPage<StandardPrescription> page, StandardPrescription prescription) {
        QueryWrapper<StandardPrescription> wrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (prescription != null) {
            if (StringUtils.hasText(prescription.getIndication1())) {
                wrapper.eq("indication1", prescription.getIndication1());
            }
            if (StringUtils.hasText(prescription.getIndication2())) {
                wrapper.eq("indication2", prescription.getIndication2());
            }
            if (StringUtils.hasText(prescription.getIndication3())) {
                wrapper.eq("indication3", prescription.getIndication3());
            }
            if (StringUtils.hasText(prescription.getIndicationKeyword())) {
                wrapper.like("indication_keyword", prescription.getIndicationKeyword());
            }
            if (StringUtils.hasText(prescription.getPresPart())) {
                wrapper.like("pres_part", prescription.getPresPart());
            }
            if (prescription.getBuiltIn() != null) {
                wrapper.eq("built_in", prescription.getBuiltIn());
            }
        }
        
        // 只查询未删除的记录
        wrapper.eq("is_deleted", 0);
        
        // 按标准处方ID排序
        wrapper.orderByAsc("standard_pres_id");
        
        return this.baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<StandardPrescription> findByPartName(String partName) {
        QueryWrapper<StandardPrescription> wrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(partName)) {
            wrapper.like("pres_part", partName);
        }
        
        // 只查询未删除的记录
        wrapper.eq("is_deleted", 0);
        
        // 按标准处方ID排序
        wrapper.orderByAsc("standard_pres_id");
        
        return this.baseMapper.selectList(wrapper);
    }
} 