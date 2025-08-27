package cc.mrbird.febs.system.domain.query;

import cc.mrbird.febs.common.domain.QueryRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 治疗记录查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TreatmentRecordQuery extends QueryRequest {
    
    /**
     * 设备ID
     */
    private Long deviceId;
    
    /**
     * 设备编号
     */
    private Integer deviceNo;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 患者姓名
     */
    private String patientName;
    
    /**
     * 患者编号
     */
    private String patientNo;
    
    /**
     * 医生姓名
     */
    private String doctorName;
    
    /**
     * 处方日期开始
     */
    private Date presDateStart;
    
    /**
     * 处方日期结束
     */
    private Date presDateEnd;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 治疗部位
     */
    private String presPart;
}
