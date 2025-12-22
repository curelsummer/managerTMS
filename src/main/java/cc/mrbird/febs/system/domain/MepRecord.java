package cc.mrbird.febs.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * MEP记录实体类（可以独立保存，也可以关联治疗记录主表）
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
     * 治疗记录ID（可选，如果提供则关联治疗记录主表）
     */
    private Long treatmentRecordId;
    
    /**
     * 患者唯一标识（姓名_出生日期）
     */
    private String patientIdentifier;
    
    /**
     * 服务器生成的记录ID（如：MEP-20241211-143000-001-156）
     */
    private String serverRecordId;
    
    /**
     * 设备本地MEP记录ID
     */
    private Long localMepRecordId;
    
    /**
     * 设备ID（如：TMS-001）
     */
    private String deviceId;
    
    /**
     * 设备编号
     */
    private Integer deviceNo;
    
    /**
     * 来源设备编号（用于同步过滤）
     */
    private Integer sourceDeviceNo;
    
    /**
     * 患者姓名
     */
    private String patientName;
    
    /**
     * 患者性别
     */
    private String patientSex;
    
    /**
     * 患者年龄字符串
     */
    private String patientAgeStr;
    
    /**
     * 患者出生日期（格式：yyyy-MM-dd）
     */
    private String patientBirthday;
    
    /**
     * 记录时间
     */
    private Date recordTime;
    
    /**
     * 数据类型
     */
    private Integer dType;
    
    /**
     * 上传时间戳（毫秒）
     */
    private Long timestamp;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}
