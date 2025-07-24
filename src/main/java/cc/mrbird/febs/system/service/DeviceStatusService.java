package cc.mrbird.febs.system.service;

public interface DeviceStatusService {
    void deviceOnline(Long deviceId);
    void deviceOffline(Long deviceId);
    void deviceHeartbeat(Long deviceId);
    String getDeviceStatus(Long deviceId);
} 