package cc.mrbird.febs.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * TBS处方记录实体类
 * 
 * @author MrBird
 */
@Data
@TableName("tbs_prescription")
public class TbsPrescription implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 治疗记录ID
     */
    private Long treatmentRecordId;
    
    /**
     * 患者TBS处方ID
     */
    private Long patientPresTbsId;
    
    /**
     * 处方强度
     */
    private Integer presStrength;
    
    /**
     * 内频率
     */
    private BigDecimal innerFreq;
    
    /**
     * 内次数
     */
    private Integer innerCount;
    
    /**
     * 间频率
     */
    private BigDecimal interFreq;
    
    /**
     * 间次数
     */
    private Integer interCount;
    
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
     * 周期数
     */
    private Integer periods;
    
    /**
     * TBS类型
     */
    private String tbsType;
    
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
