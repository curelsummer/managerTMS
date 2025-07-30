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
import java.util.Date;

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
        System.out.println("=== 设备上线处理开始，设备ID: " + deviceId + " ===");
        try {
            // 更新数据库
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                System.out.println("找到设备，当前状态: " + device.getStatus());
                device.setStatus("online");
                boolean updateResult = deviceService.updateById(device);
                System.out.println("数据库更新结果: " + updateResult);
            } else {
                System.out.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
            }
            
            // 更新Redis
            redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "online");
            redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + deviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
            System.out.println("Redis状态更新完成");
            
            pushStatus(deviceId, "online");
            System.out.println("=== 设备上线处理完成 ===");
        } catch (Exception e) {
            System.err.println("设备上线处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deviceOffline(Long deviceId) {
        System.out.println("=== 设备下线处理开始，设备ID: " + deviceId + " ===");
        try {
            // 更新数据库
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                System.out.println("找到设备，当前状态: " + device.getStatus());
                device.setStatus("offline");
                boolean updateResult = deviceService.updateById(device);
                System.out.println("数据库更新结果: " + updateResult);
            } else {
                System.out.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
            }
            
            // 更新Redis
            redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "offline");
            System.out.println("Redis状态更新完成");
            
            pushStatus(deviceId, "offline");
            System.out.println("=== 设备下线处理完成 ===");
        } catch (Exception e) {
            System.err.println("设备下线处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deviceHeartbeat(Long deviceId) {
        System.out.println("=== 设备心跳处理开始，设备ID: " + deviceId + " ===");
        try {
            Date currentTime = new Date();
            
            // 更新Redis心跳时间
            redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + deviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
            System.out.println("Redis心跳更新完成");
            
            // 更新数据库设备状态和心跳时间
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                System.out.println("找到设备，当前状态: " + device.getStatus());
                
                // 更新状态为online（如果不是的话）
                if (!"online".equals(device.getStatus())) {
                    device.setStatus("online");
                    System.out.println("更新设备状态为online");
                }
                
                // 更新最后心跳时间
                device.setLastHeartbeat(currentTime);
                boolean updateResult = deviceService.updateById(device);
                System.out.println("数据库心跳时间更新结果: " + updateResult);
                
            } else {
                System.out.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
            }
            
            // 检查Redis状态，如果不是online则设置为online
            String currentStatus = getDeviceStatus(deviceId);
            System.out.println("当前Redis状态: " + currentStatus);
            if (!"online".equals(currentStatus)) {
                System.out.println("Redis状态不是online，调用deviceOnline方法");
                deviceOnline(deviceId);
            } else {
                System.out.println("Redis状态已经是online，无需更新");
            }
            
            System.out.println("=== 设备心跳处理完成 ===");
        } catch (Exception e) {
            System.err.println("设备心跳处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getDeviceStatus(Long deviceId) {
        try {
            String status = redisTemplate.opsForValue().get(DEVICE_STATUS_KEY + deviceId);
            return status != null ? status : "offline";
        } catch (Exception e) {
            System.err.println("获取设备状态异常: " + e.getMessage());
            return "offline";
        }
    }

    // 定时任务：检查心跳超时设备自动下线（Redis方式）
    @Scheduled(fixedDelay = 60000)
    public void checkHeartbeatTimeout() {
        System.out.println("=== 心跳超时检查定时任务启动 ===");
        try {
            System.out.println("=== 开始心跳超时检查 ===");
            
            // 检查Redis连接
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                System.out.println("Redis连接正常");
            } catch (Exception e) {
                System.err.println("Redis连接失败，跳过心跳检查: " + e.getMessage());
                return;
            }
            
            Set<String> keys = redisTemplate.keys("device:heartbeat:*");
            long now = System.currentTimeMillis();
            int timeoutCount = 0;
            
            if (keys != null) {
                System.out.println("检查 " + keys.size() + " 个设备的心跳状态");
                for (String key : keys) {
                    try {
                        String value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            long last = Long.parseLong(value);
                            Long deviceId = Long.parseLong(key.substring("device:heartbeat:".length()));
                            long timeDiff = now - last;
                            
                            System.out.println("设备 " + deviceId + " 最后心跳时间: " + (timeDiff / 1000) + " 秒前");
                            
                            if (timeDiff > HEARTBEAT_TIMEOUT * 1000) {
                                System.out.println("设备 " + deviceId + " 心跳超时，自动下线");
                                deviceOffline(deviceId);
                                timeoutCount++;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("检查设备心跳时发生异常: " + e.getMessage());
                    }
                }
                System.out.println("心跳检查完成，超时设备数: " + timeoutCount);
            } else {
                System.out.println("没有找到设备心跳记录");
            }
            
        } catch (Exception e) {
            System.err.println("心跳超时检查异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    


    private void pushStatus(Long deviceId, String status) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("deviceId", deviceId);
            msg.put("status", status);
            String json = objectMapper.writeValueAsString(msg);
            DeviceWebSocketServer.broadcast(json);
            System.out.println("推送状态消息: " + json);
        } catch (Exception e) {
            System.err.println("推送状态消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 