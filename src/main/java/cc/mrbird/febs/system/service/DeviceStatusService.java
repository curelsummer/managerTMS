package cc.mrbird.febs.system.service;

public interface DeviceStatusService {
    void deviceOnline(Long deviceId);
    void deviceOffline(Long deviceId);
    void deviceHeartbeat(Long deviceId);
    String getDeviceStatus(Long deviceId);
    
    // 新增方法：处理包含新字段的状态更新
    void deviceOnlineWithInfo(Long deviceId, Integer deviceNo, Integer batTimes, Integer capTimes, Integer treatmentStatus);
    void deviceOfflineWithInfo(Long deviceId, Integer deviceNo, Integer batTimes, Integer capTimes, Integer treatmentStatus);
    void deviceHeartbeatWithInfo(Long deviceId, Integer deviceNo, Integer batTimes, Integer capTimes, Integer treatmentStatus);
    
    // 通过deviceNo查询deviceId
    Long getDeviceIdByDeviceNo(Integer deviceNo);
    
    // 处理WebSocket连接断开
    void handleConnectionDisconnect(Long deviceId);
} 