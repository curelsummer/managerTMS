package cc.mrbird.febs.system.domain.response;

import lombok.Data;
import java.util.List;

/**
 * 同步响应
 */
@Data
public class SyncResponse {
    
    /**
     * 消息类型
     */
    private String messageType = "SYNC_RESPONSE";
    
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
     * 同步数据
     */
    private SyncData data;
    
    @Data
    public static class SyncData {
        /**
         * 患者唯一标识
         */
        private String patientIdentifier;
        
        /**
         * 治疗记录总数
         */
        private Integer totalTreatmentCount;
        
        /**
         * MEP记录总数
         */
        private Integer totalMepCount;
        
        /**
         * 治疗记录列表
         */
        private List<TreatmentRecordDTO> treatmentRecords;
        
        /**
         * MEP记录列表
         */
        private List<MepRecordDTO> mepRecords;
    }
    
    public static SyncResponse success(SyncData data) {
        SyncResponse response = new SyncResponse();
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(true);
        response.setCode(200);
        response.setMessage("同步成功");
        response.setData(data);
        return response;
    }
    
    public static SyncResponse error(String errorMessage) {
        SyncResponse response = new SyncResponse();
        response.setTimestamp(System.currentTimeMillis());
        response.setSuccess(false);
        response.setCode(500);
        response.setMessage(errorMessage);
        return response;
    }
}



