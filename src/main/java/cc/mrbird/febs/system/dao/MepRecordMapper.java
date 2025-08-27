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
     * 根据治疗记录ID查询MEP记录
     */
    List<MepRecordVO> selectByTreatmentRecordId(@Param("treatmentRecordId") Long treatmentRecordId);
}
