package cc.mrbird.febs.system.domain.response;

import lombok.Data;

/**
 * 治疗记录创建响应
 */
@Data
public class TreatmentRecordCreatedResponse {
    
    /**
     * 消息类型
     */
    private String messageType = "TREATMENT_RECORD_CREATED_RESPONSE";
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 服务器生成的记录ID
     */
    private String serverRecordId;
    
    /**
     * 设备本地病历ID（回显）
     */
    private Long localMedicalRecordId;
    
    public static TreatmentRecordCreatedResponse success(String serverRecordId, Long localMedicalRecordId) {
        TreatmentRecordCreatedResponse response = new TreatmentRecordCreatedResponse();
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(true);
        response.setCode(200);
        response.setMessage("治疗记录保存成功");
        response.setServerRecordId(serverRecordId);
        response.setLocalMedicalRecordId(localMedicalRecordId);
        return response;
    }
    
    public static TreatmentRecordCreatedResponse error(String errorMessage, Long localMedicalRecordId) {
        TreatmentRecordCreatedResponse response = new TreatmentRecordCreatedResponse();
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(false);
        response.setCode(500);
        response.setMessage(errorMessage);
        response.setLocalMedicalRecordId(localMedicalRecordId);
        return response;
    }
}





