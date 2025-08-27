package cc.mrbird.febs.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MEP数据实体类
 * 
 * @author MrBird
 */
@Data
@TableName("mep_data")
public class MepData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
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
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}
