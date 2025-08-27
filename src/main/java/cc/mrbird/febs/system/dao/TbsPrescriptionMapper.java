package cc.mrbird.febs.system.dao;

import cc.mrbird.febs.system.domain.TbsPrescription;
import cc.mrbird.febs.system.domain.vo.TbsPrescriptionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TBS处方记录Mapper
 */
@Mapper
public interface TbsPrescriptionMapper extends BaseMapper<TbsPrescription> {
    
    /**
     * 根据治疗记录ID查询TBS处方记录
     */
    List<TbsPrescriptionVO> selectByTreatmentRecordId(@Param("treatmentRecordId") Long treatmentRecordId);
}
