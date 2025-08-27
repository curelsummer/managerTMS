package cc.mrbird.febs.system.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 治疗记录展示VO
 */
@Data
public class TreatmentRecordVO {
    
    /**
     * 主键ID
     */
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
    
    /**
     * 标准处方记录列表
     */
    private List<PrescriptionRecordVO> prescriptionRecords;
    
    /**
     * TBS处方记录列表
     */
    private List<TbsPrescriptionVO> tbsPrescriptions;
    
    /**
     * MEP记录列表
     */
    private List<MepRecordVO> mepRecords;
}
