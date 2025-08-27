package cc.mrbird.febs.system.service;

import cc.mrbird.febs.common.domain.QueryRequest;
import cc.mrbird.febs.system.domain.vo.TreatmentRecordVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 治疗记录服务接口
 */
public interface TreatmentRecordService {
    
    /**
     * 保存治疗记录
     */
    boolean saveTreatmentRecord(JsonNode jsonNode);
    
    /**
     * 分页查询治疗记录
     */
    IPage<TreatmentRecordVO> findTreatmentRecordPage(QueryRequest request, Object query);
    
    /**
     * 根据ID查询治疗记录详情（包含关联数据）
     */
    TreatmentRecordVO findTreatmentRecordById(Long id);
    
    /**
     * 查询治疗记录列表
     */
    List<TreatmentRecordVO> findTreatmentRecordList(Object query);
    
    /**
     * 根据ID查询完整的治疗记录信息（包含所有关联数据）
     */
    TreatmentRecordVO findTreatmentRecordDetailById(Long id);
    
    /**
     * 删除治疗记录
     */
    boolean deleteTreatmentRecord(Long id);
    
    /**
     * 批量删除治疗记录
     */
    boolean deleteTreatmentRecords(String[] ids);
}
