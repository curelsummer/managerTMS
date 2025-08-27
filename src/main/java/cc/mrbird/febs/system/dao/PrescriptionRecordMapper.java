package cc.mrbird.febs.system.dao;

import cc.mrbird.febs.system.domain.PrescriptionRecord;
import cc.mrbird.febs.system.domain.vo.PrescriptionRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标准处方记录Mapper
 */
@Mapper
public interface PrescriptionRecordMapper extends BaseMapper<PrescriptionRecord> {
    
    /**
     * 根据治疗记录ID查询标准处方记录
     */
    List<PrescriptionRecordVO> selectByTreatmentRecordId(@Param("treatmentRecordId") Long treatmentRecordId);
}
