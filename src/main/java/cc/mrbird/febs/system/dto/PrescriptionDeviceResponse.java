package cc.mrbird.febs.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 处方设备响应DTO
 * 用于返回处方发送到设备的结果
 */
@Data
public class PrescriptionDeviceResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 处方ID
     */
    private Long prescriptionId;
    
    /**
     * 设备ID
     */
    private Long deviceId;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 发送状态
     */
    private String status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 设备确认时间
     */
    private LocalDateTime deviceConfirmTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 设备响应数据
     */
    private String deviceResponse;
    
    /**
     * 重试次数
     */
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetryCount = 3;
    
    /**
     * 是否已确认
     */
    private boolean confirmed = false;
    
    /**
     * 确认状态描述
     */
    private String confirmStatus;
    
    /**
     * 创建成功响应
     */
    public static PrescriptionDeviceResponse success(Long prescriptionId, Long deviceId, String messageId) {
        PrescriptionDeviceResponse response = new PrescriptionDeviceResponse();
        response.setSuccess(true);
        response.setPrescriptionId(prescriptionId);
        response.setDeviceId(deviceId);
        response.setMessageId(messageId);
        response.setStatus("SENT");
        response.setSendTime(LocalDateTime.now());
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static PrescriptionDeviceResponse failure(Long prescriptionId, Long deviceId, String errorMessage) {
        PrescriptionDeviceResponse response = new PrescriptionDeviceResponse();
        response.setSuccess(false);
        response.setPrescriptionId(prescriptionId);
        response.setDeviceId(deviceId);
        response.setStatus("FAILED");
        response.setErrorMessage(errorMessage);
        response.setSendTime(LocalDateTime.now());
        return response;
    }
}



