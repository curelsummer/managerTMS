package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.MepRecord;
import cc.mrbird.febs.system.domain.vo.MepRecordVO;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * MEP记录服务接口
 */
public interface MepRecordService {
    
    /**
     * 保存MEP记录（从设备上报）
     * 
     * @param jsonNode 设备上报的JSON数据
     * @return 服务器生成的记录ID
     */
    String saveMepRecord(JsonNode jsonNode);
    
    /**
     * 根据患者唯一标识查询MEP记录列表
     * 
     * @param patientIdentifier 患者唯一标识
     * @return MEP记录列表
     */
    List<MepRecord> getByPatientIdentifier(String patientIdentifier);
    
    /**
     * 根据服务器记录ID查询MEP记录
     * 
     * @param serverRecordId 服务器记录ID
     * @return MEP记录
     */
    MepRecord getByServerRecordId(String serverRecordId);
    
    /**
     * 根据患者唯一标识查询MEP记录VO列表（包括关联的和独立的）
     * 
     * @param patientIdentifier 患者唯一标识
     * @return MEP记录VO列表
     */
    List<MepRecordVO> getMepRecordVOByPatientIdentifier(String patientIdentifier);
    
    /**
     * 根据ID查询MEP记录详情（包含MEP数据）
     * 
     * @param id MEP记录ID
     * @return MEP记录VO
     */
    MepRecordVO getMepRecordDetailById(Long id);
}



