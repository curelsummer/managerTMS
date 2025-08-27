package cc.mrbird.febs.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 标准处方记录VO
 */
@Data
public class PrescriptionRecordVO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 治疗记录ID
     */
    private Long treatmentRecordId;
    
    /**
     * 患者处方ID
     */
    private Long patientPresId;
    
    /**
     * 处方强度
     */
    private Integer presStrength;
    
    /**
     * 处方频率
     */
    private BigDecimal presFreq;
    
    /**
     * 持续时间
     */
    private BigDecimal lastTime;
    
    /**
     * 暂停时间
     */
    private Integer pauseTime;
    
    /**
     * 重复次数
     */
    private Integer repeatCount;
    
    /**
     * 总次数
     */
    private Integer totalCount;
    
    /**
     * 总时间字符串
     */
    private String totalTimeStr;
    
    /**
     * 治疗部位
     */
    private String presPart;
    
    /**
     * 标准处方名称
     */
    private String standardPresName;
    
    /**
     * 周期数
     */
    private Integer periods;
    
    /**
     * 处方日期
     */
    private Date presDate;
    
    /**
     * 处方时间
     */
    private String presTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}
