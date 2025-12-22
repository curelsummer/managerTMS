package cc.mrbird.febs.system.domain.response;

import lombok.Data;

/**
 * MEP记录创建响应
 */
@Data
public class MepRecordCreatedResponse {
    
    /**
     * 消息类型
     */
    private String messageType = "MEP_RECORD_CREATED_RESPONSE";
    
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
     * 设备本地MEP记录ID（回显）
     */
    private Long localMepRecordId;
    
    public static MepRecordCreatedResponse success(String serverRecordId, Long localMepRecordId) {
        MepRecordCreatedResponse response = new MepRecordCreatedResponse();
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(true);
        response.setCode(200);
        response.setMessage("MEP记录保存成功");
        response.setServerRecordId(serverRecordId);
        response.setLocalMepRecordId(localMepRecordId);
        return response;
    }
    
    public static MepRecordCreatedResponse error(String errorMessage, Long localMepRecordId) {
        MepRecordCreatedResponse response = new MepRecordCreatedResponse();
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(false);
        response.setCode(500);
        response.setMessage(errorMessage);
        response.setLocalMepRecordId(localMepRecordId);
        return response;
    }
}



