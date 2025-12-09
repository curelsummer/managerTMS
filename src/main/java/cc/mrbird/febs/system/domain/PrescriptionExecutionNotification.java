package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 处方执行记录通知数据传输对象
 * 用于通过WebSocket发送给WPF程序
 */
@Data
public class PrescriptionExecutionNotification implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息类型
     * PRESCRIPTION_BROADCAST - 广播新处方（待领取）
     * PRESCRIPTION_EXECUTION_CREATED - 旧版消息类型（兼容）
     */
    private String messageType = "PRESCRIPTION_BROADCAST";
    
    /**
     * 时间戳
     */
    private Date timestamp;
    
    /**
     * 处方执行记录ID
     */
    private Long executionId;
    
    /**
     * 处方执行记录状态
     */
    private Integer executionStatus;
    
    /**
     * 处方信息
     */
    private PrescriptionInfo prescriptionInfo;
    
    /**
     * 患者信息
     */
    private PatientInfo patientInfo;
    
    /**
     * 设备信息
     */
    private DeviceInfo deviceInfo;
    
    /**
     * 执行人信息
     */
    private UserInfo executorInfo;
    
    /**
     * 医院信息
     */
    private HospitalInfo hospitalInfo;
    
    /**
     * 处方信息内部类
     */
    @Data
    public static class PrescriptionInfo implements Serializable {
        private Long id;
        private Integer presType;
        private Integer status;
        private Integer presStrength;
        private String presFreq;
        private String lastTime;
        private Integer pauseTime;
        private Integer repeatCount;
        private Integer totalCount;
        private Integer totalTime;
        private Integer presPartId;
        private String presPartName;
        private Integer standardPresId;
        private String standardPresName;
        private Integer tbsType;
        private Integer innerCount;
        private String interFreq;
        private Integer interCount;
        private Integer periods;
        private Date createdAt;
    }
    
    /**
     * 患者信息内部类
     */
    @Data
    public static class PatientInfo implements Serializable {
        private Long id;
        private String name;
        private String idCard;
        private String gender;
        private Date birthday;
        private String hisId;
        private String code;
    }
    
    /**
     * 设备信息内部类
     */
    @Data
    public static class DeviceInfo implements Serializable {
        private Long deviceId;
        private Integer deviceNo;        // 设备编号
        private String deviceType;
        private String sn;
        private String status;
        private Date lastHeartbeat;
    }
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo implements Serializable {
        private Long userId;
        private String username;
        private String email;
        private String mobile;
    }
    
    /**
     * 医院信息内部类
     */
    @Data
    public static class HospitalInfo implements Serializable {
        private Long hospitalId;
        private String name;
        private String address;
        private String contact;
    }
} 