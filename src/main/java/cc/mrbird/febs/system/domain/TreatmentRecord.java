package cc.mrbird.febs.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 治疗记录实体类
 * 
 * @author MrBird
 */
@Data
@TableName("treatment_record")
public class TreatmentRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 设备ID
     */
    private Long deviceId;
    
    /**
     * 设备编号
     */
    private Integer deviceNo;
    
    /**
     * 病历ID
     */
    private Long medicalRecordId;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 患者姓名
     */
    private String patientName;
    
    /**
     * 患者性别
     */
    private String patientSex;
    
    /**
     * 患者年龄
     */
    private String patientAgeStr;
    
    /**
     * 患者唯一标识（姓名_出生日期）
     */
    private String patientIdentifier;
    
    /**
     * 服务器生成的记录ID（如：TR-20241211-150000-001-289）
     * 注意：此字段在数据库表中不存在，仅用于业务逻辑
     */
    @TableField(exist = false)
    private String serverRecordId;
    
    /**
     * 设备本地病历ID
     * 注意：如果表中不存在此字段，请添加 @TableField(exist = false)
     */
    @TableField(exist = false)
    private Long localMedicalRecordId;
    
    /**
     * 来源设备编号（用于同步过滤）
     * 注意：如果表中不存在此字段，请添加 @TableField(exist = false)
     */
    @TableField(exist = false)
    private Integer sourceDeviceNo;
    
    /**
     * 患者出生日期（格式：yyyy-MM-dd）
     * 注意：如果数据库表中不存在此字段，请添加 @TableField(exist = false)
     */
    @TableField(exist = false)
    private String patientBirthday;
    
    /**
     * 患者身高（cm）
     * 注意：数据库表中不存在此字段，仅用于业务逻辑
     */
    @TableField(exist = false)
    private Integer patientHeight;
    
    /**
     * 患者体重（kg）
     * 注意：数据库表中不存在此字段，仅用于业务逻辑
     */
    @TableField(exist = false)
    private Integer patientWeight;
    
    /**
     * 患者病房
     */
    private String patientRoom;
    
    /**
     * 患者编号
     */
    private String patientNo;
    
    /**
     * 患者床位
     */
    private String patientBed;
    
    /**
     * 处方日期
     */
    private Date presDate;
    
    /**
     * 处方时间
     */
    private String presTime;
    
    /**
     * 医生姓名
     */
    private String doctorName;
    
    /**
     * MEP值
     */
    private Integer mepValue;
    
    /**
     * 病历备注
     */
    private String medicalRecordRemark;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
}
