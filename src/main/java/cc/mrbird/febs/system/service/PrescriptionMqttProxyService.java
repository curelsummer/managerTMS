package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.dto.PrescriptionDeviceRequest;
import cc.mrbird.febs.system.dto.PrescriptionDeviceResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 处方MQTT代理服务接口
 * 负责管理MQTT消息的发送、接收和状态管理
 */
public interface PrescriptionMqttProxyService {
    
    /**
     * 发送处方消息到设备
     * @param request 处方设备请求
     * @return 异步发送结果
     */
    CompletableFuture<PrescriptionDeviceResponse> sendPrescriptionMessage(PrescriptionDeviceRequest request);
    
    /**
     * 发送取消消息到设备
     * @param prescriptionId 处方ID
     * @param deviceId 设备ID
     * @return 是否发送成功
     */
    boolean sendCancelMessage(Long prescriptionId, Long deviceId);
    
    /**
     * 发送状态查询消息到设备
     * @param deviceId 设备ID
     * @return 是否发送成功
     */
    boolean sendStatusQueryMessage(Long deviceId);
    
    /**
     * 处理设备响应消息
     * @param deviceId 设备ID
     * @param message 响应消息
     */
    void handleDeviceResponse(Long deviceId, String message);
    
    /**
     * 检查设备连接状态
     * @param deviceId 设备ID
     * @return 连接状态
     */
    boolean isDeviceConnected(Long deviceId);
    
    /**
     * 获取设备最后心跳时间
     * @param deviceId 设备ID
     * @return 最后心跳时间
     */
    java.time.LocalDateTime getDeviceLastHeartbeat(Long deviceId);
}



