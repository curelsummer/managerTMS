package cc.mrbird.febs.system.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * MEP记录VO
 */
@Data
public class MepRecordVO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 治疗记录ID
     */
    private Long treatmentRecordId;
    
    /**
     * MEP记录ID
     */
    private Long mepRecordId;
    
    /**
     * 数据类型
     */
    private Integer dType;
    
    /**
     * 输入时间
     */
    private Date inTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * MEP数据列表
     */
    private List<MepDataVO> mepDataList;
}
