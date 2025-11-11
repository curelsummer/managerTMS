package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.dto.PrescriptionDeviceRequest;
import cc.mrbird.febs.system.dto.PrescriptionDeviceResponse;
import java.util.List;

/**
 * 处方设备服务接口
 * 负责处理处方与设备之间的通信
 */
public interface PrescriptionDeviceService {
    
    /**
     * 发送处方到设备
     * @param request 处方设备请求
     * @return 发送结果
     */
    PrescriptionDeviceResponse sendPrescriptionToDevice(PrescriptionDeviceRequest request);
    
    /**
     * 批量发送处方到设备
     * @param requests 处方设备请求列表
     * @return 发送结果列表
     */
    List<PrescriptionDeviceResponse> batchSendPrescriptions(List<PrescriptionDeviceRequest> requests);
    
    /**
     * 查询处方发送状态
     * @param prescriptionId 处方ID
     * @return 发送状态
     */
    PrescriptionDeviceResponse getPrescriptionStatus(Long prescriptionId);
    
    /**
     * 取消处方发送
     * @param prescriptionId 处方ID
     * @return 是否成功
     */
    boolean cancelPrescription(Long prescriptionId);
    
    /**
     * 重试发送处方
     * @param prescriptionId 处方ID
     * @return 重试结果
     */
    PrescriptionDeviceResponse retryPrescription(Long prescriptionId);
    
    /**
     * 获取设备连接状态
     * @param deviceId 设备ID
     * @return 连接状态
     */
    boolean isDeviceConnected(Long deviceId);
}



