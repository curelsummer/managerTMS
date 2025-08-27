package cc.mrbird.febs.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * MEP记录实体类
 * 
 * @author MrBird
 */
@Data
@TableName("mep_record")
public class MepRecord implements Serializable {
    
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
}
