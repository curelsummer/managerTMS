package cc.mrbird.febs.system.dao;

import cc.mrbird.febs.system.domain.TreatmentRecord;
import cc.mrbird.febs.system.domain.vo.TreatmentRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 治疗记录Mapper
 */
@Mapper
public interface TreatmentRecordMapper extends BaseMapper<TreatmentRecord> {
    
    /**
     * 根据消息ID查询治疗记录
     */
    TreatmentRecord selectByMessageId(@Param("messageId") String messageId);
    
    /**
     * 根据服务器记录ID查询治疗记录
     */
    TreatmentRecord selectByServerRecordId(@Param("serverRecordId") String serverRecordId);
    
    /**
     * 根据患者唯一标识查询治疗记录列表
     */
    List<TreatmentRecord> selectByPatientIdentifier(@Param("patientIdentifier") String patientIdentifier);
    
    /**
     * 分页查询治疗记录
     */
    IPage<TreatmentRecordVO> selectTreatmentRecordPage(Page<TreatmentRecordVO> page, @Param("query") Object query);
    
    /**
     * 根据ID查询完整的治疗记录信息
     */
    TreatmentRecordVO selectTreatmentRecordById(@Param("id") Long id);
    
    /**
     * 查询治疗记录列表（不分页）
     */
    List<TreatmentRecordVO> selectTreatmentRecordList(@Param("query") Object query);
}
