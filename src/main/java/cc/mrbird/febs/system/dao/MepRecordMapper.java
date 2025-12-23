package cc.mrbird.febs.system.dao;

import cc.mrbird.febs.system.domain.MepRecord;
import cc.mrbird.febs.system.domain.vo.MepRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MEP记录Mapper
 */
@Mapper
public interface MepRecordMapper extends BaseMapper<MepRecord> {
    
    /**
     * 根据治疗记录ID查询MEP记录（包括关联的和独立的）
     */
    List<MepRecordVO> selectByTreatmentRecordId(@Param("treatmentRecordId") Long treatmentRecordId);
    
    /**
     * 根据患者唯一标识查询MEP记录列表（包括关联的和独立的）
     */
    List<MepRecordVO> selectByPatientIdentifier(@Param("patientIdentifier") String patientIdentifier);
}
