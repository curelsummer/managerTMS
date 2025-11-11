package cc.mrbird.febs.system.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 处方设备请求DTO
 * 用于发送处方到设备的请求参数
 */
@Data
public class PrescriptionDeviceRequest {
    
    /**
     * 处方ID
     */
    @NotNull(message = "处方ID不能为空")
    private Long prescriptionId;
    
    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    
    /**
     * 设备编号
     */
    private String deviceNo;
    
    /**
     * 患者ID
     */
    private Long patientId;
    
    /**
     * 医生ID
     */
    private Long doctorId;
    
    /**
     * 优先级 (0-低, 1-中, 2-高)
     */
    private Integer priority = 1;
    
    /**
     * 执行时间 (立即执行为null)
     */
    private LocalDateTime executeTime;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 是否立即执行
     */
    private Boolean executeImmediately = true;
}
