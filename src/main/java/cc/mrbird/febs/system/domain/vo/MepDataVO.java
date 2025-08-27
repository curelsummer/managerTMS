package cc.mrbird.febs.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * MEP数据VO
 */
@Data
public class MepDataVO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * MEP记录ID
     */
    private Long mepRecordId;
    
    /**
     * MT值
     */
    private Integer mt;
    
    /**
     * 通道
     */
    private String ch;
    
    /**
     * 最大值
     */
    private BigDecimal maxValue;
    
    /**
     * 最大值时间
     */
    private BigDecimal maxTime;
    
    /**
     * 最小值
     */
    private BigDecimal minValue;
    
    /**
     * 最小值时间
     */
    private BigDecimal minTime;
    
    /**
     * 振幅
     */
    private BigDecimal amplitude;
    
    /**
     * 治疗部位
     */
    private String part;
    
    /**
     * 记录部位
     */
    private String recordPart;
    
    /**
     * 创建时间
     */
    private java.util.Date createTime;
    
    /**
     * 更新时间
     */
    private java.util.Date updateTime;
}
