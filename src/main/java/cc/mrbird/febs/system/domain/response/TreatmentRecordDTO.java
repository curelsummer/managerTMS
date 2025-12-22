package cc.mrbird.febs.system.domain.response;

import lombok.Data;
import java.util.List;

/**
 * 治疗记录DTO（用于同步响应）
 */
@Data
public class TreatmentRecordDTO {
    
    /**
     * 服务器记录ID
     */
    private String serverRecordId;
    
    /**
     * 来源设备编号
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
     * 患者身高
     */
    private Integer patientHeight;
    
    /**
     * 患者体重
     */
    private Integer patientWeight;
    
    /**
     * 患者年龄字符串
     */
    private String patientAgeStr;
    
    /**
     * 患者出生日期
     */
    private String patientBirthday;
    
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
    private String presDate;
    
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
     * 标准处方记录列表
     */
    private List<PrescriptionRecordDTO> prescription_record;
    
    /**
     * TBS处方记录列表
     */
    private List<TbsPrescriptionDTO> tbsPrescriptions;
    
    @Data
    public static class PrescriptionRecordDTO {
        private Long patientPresId;
        private Integer presStrength;
        private Double presFreq;
        private Double lastTime;
        private Integer pauseTime;
        private Integer repeatCount;
        private Integer totalCount;
        private String totalTimeStr;
        private String presPart;
        private String standardPresName;
        private String presDate;
        private String presTime;
        private Integer periods;
    }
    
    @Data
    public static class TbsPrescriptionDTO {
        private Long patientPresTBSId;
        private Integer presStrength;
        private Double innerFreq;
        private Integer innerCount;
        private Double interFreq;
        private Integer interCount;
        private Integer pauseTime;
        private Integer repeatCount;
        private Integer totalCount;
        private String totalTimeStr;
        private String presPart;
        private String tbsType;
        private String presDate;
        private String presTime;
        private Integer periods;
    }
}



