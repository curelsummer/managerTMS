package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.service.DeviceStatusService;
import cc.mrbird.febs.system.websocket.DeviceWebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cc.mrbird.febs.system.service.DeviceService;
import cc.mrbird.febs.system.domain.Device;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Set;

@Service
public class DeviceStatusServiceImpl implements DeviceStatusService {
    private static final String DEVICE_STATUS_KEY = "device:status:";
    private static final String DEVICE_HEARTBEAT_KEY = "device:heartbeat:";
    private static final long HEARTBEAT_TIMEOUT = 120; // 秒

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DeviceService deviceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void deviceOnline(Long deviceId) {
        // 更新数据库
        Device device = deviceService.getById(deviceId);
        if (device != null) {
            device.setStatus("online");
            deviceService.updateById(device);
        }
        redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "online");
        redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + deviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
        pushStatus(deviceId, "online");
    }

    @Override
    public void deviceOffline(Long deviceId) {
        // 更新数据库
        Device device = deviceService.getById(deviceId);
        if (device != null) {
            device.setStatus("offline");
            deviceService.updateById(device);
        }
        redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "offline");
        pushStatus(deviceId, "offline");
    }

    @Override
    public void deviceHeartbeat(Long deviceId) {
        redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + deviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
        // 如果数据库不是online则更新
        Device device = deviceService.getById(deviceId);
        if (device != null && !"online".equals(device.getStatus())) {
            device.setStatus("online");
            deviceService.updateById(device);
        }
        if (!"online".equals(getDeviceStatus(deviceId))) {
            deviceOnline(deviceId);
        }
    }

    @Override
    public String getDeviceStatus(Long deviceId) {
        String status = redisTemplate.opsForValue().get(DEVICE_STATUS_KEY + deviceId);
        return status != null ? status : "offline";
    }

    // 定时任务：检查心跳超时设备自动下线
    @Scheduled(fixedDelay = 60000)
    public void checkHeartbeatTimeout() {
        Set<String> keys = redisTemplate.keys("device:heartbeat:*");
        long now = System.currentTimeMillis();
        if (keys != null) {
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    long last = Long.parseLong(value);
                    Long deviceId = Long.parseLong(key.substring("device:heartbeat:".length()));
                    if (now - last > HEARTBEAT_TIMEOUT * 1000) {
                        deviceOffline(deviceId);
                    }
                }
            }
        }
    }

    private void pushStatus(Long deviceId, String status) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("deviceId", deviceId);
            msg.put("status", status);
            String json = objectMapper.writeValueAsString(msg);
            DeviceWebSocketServer.broadcast(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 