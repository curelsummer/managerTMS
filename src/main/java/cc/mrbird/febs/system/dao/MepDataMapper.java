package cc.mrbird.febs.system.dao;

import cc.mrbird.febs.system.domain.MepData;
import cc.mrbird.febs.system.domain.vo.MepDataVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MEP数据Mapper
 */
@Mapper
public interface MepDataMapper extends BaseMapper<MepData> {
    
    /**
     * 根据MEP记录ID查询MEP数据
     */
    List<MepDataVO> selectByMepRecordId(@Param("mepRecordId") Long mepRecordId);
}
