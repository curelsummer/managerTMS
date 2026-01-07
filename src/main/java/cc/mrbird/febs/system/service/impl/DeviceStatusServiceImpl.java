package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.service.DeviceStatusService;
import cc.mrbird.febs.system.websocket.DeviceWebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;

import cc.mrbird.febs.system.service.DeviceService;
import cc.mrbird.febs.system.domain.Device;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.Date;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;

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
        // System.out.println("设备上线: " + deviceId);
        try {
            // 更新数据库
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                // System.out.println("找到设备，当前状态: " + device.getStatus());
                device.setStatus("online");
                
                // 注意：这里只是设置状态为online，新字段的值需要在调用时传入
                // 新字段的更新应该在WebSocket消息处理时进行
                
                boolean updateResult = deviceService.updateById(device);
                // System.out.println("数据库更新结果: " + updateResult);
            } else {
                System.err.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
            }
            
            // 更新Redis
            redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "online");
            redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + deviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
            // System.out.println("Redis状态更新完成");
            
            pushStatus(deviceId, "online");
        } catch (Exception e) {
            System.err.println("设备上线处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deviceOffline(Long deviceId) {
        // System.out.println("设备下线: " + deviceId);
        try {
            // 更新数据库
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                // System.out.println("找到设备，当前状态: " + device.getStatus());
                device.setStatus("offline");
                boolean updateResult = deviceService.updateById(device);
                // System.out.println("数据库更新结果: " + updateResult);
            } else {
                System.err.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
                System.err.println("请检查设备ID是否正确，或者设备是否已从数据库中删除");
            }
            
            // 更新Redis
            // System.out.println("开始更新Redis状态...");
            try {
                redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "offline");
                // System.out.println("Redis状态更新完成");
            } catch (Exception e) {
                System.err.println("Redis状态更新失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            pushStatus(deviceId, "offline");
        } catch (Exception e) {
            System.err.println("设备下线处理异常: " + e.getMessage());
            System.err.println("异常类型: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    @Override
    public void deviceHeartbeat(Long deviceId) {
        try {
            Date currentTime = new Date();
            
            // 更新Redis心跳时间
            redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + deviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
            // System.out.println("Redis心跳更新完成");
            
            // 更新数据库设备状态和心跳时间
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                // System.out.println("找到设备，当前状态: " + device.getStatus());
                
                // 更新状态为online（如果不是的话）
                if (!"online".equals(device.getStatus())) {
                    device.setStatus("online");
                    // System.out.println("更新设备状态为online");
                }
                
                // 更新最后心跳时间
                device.setLastHeartbeat(currentTime);
                boolean updateResult = deviceService.updateById(device);
                // System.out.println("数据库心跳时间更新结果: " + updateResult);
                
            } else {
                System.err.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
            }
            
            // 检查Redis状态，如果不是online则设置为online
            String currentStatus = getDeviceStatus(deviceId);
            // System.out.println("当前Redis状态: " + currentStatus);
            if (!"online".equals(currentStatus)) {
                // System.out.println("Redis状态不是online，调用deviceOnline方法");
                deviceOnline(deviceId);
            }
            
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

    @Override
    public Long getDeviceIdByDeviceNo(Integer deviceNo) {
        if (deviceNo == null) {
            return null;
        }
        
        try {
            // 通过deviceNo查询deviceId
            QueryWrapper<Device> wrapper = new QueryWrapper<>();
            wrapper.eq("device_no", deviceNo);
            wrapper.select("device_id");
            
            List<Device> devices = deviceService.list(wrapper);
            
            if (devices.size() == 1) {
                // 只有一条记录，返回对应的deviceId
                Long deviceId = devices.get(0).getDeviceId();
                // System.out.println("通过deviceNo " + deviceNo + " 查询到唯一deviceId: " + deviceId);
                return deviceId;
            } else if (devices.size() == 0) {
                // System.out.println("通过deviceNo " + deviceNo + " 未查询到任何记录");
                return null;
            } else {
                System.err.println("通过deviceNo " + deviceNo + " 查询到 " + devices.size() + " 条记录，不做处理");
                return null;
            }
        } catch (Exception e) {
            System.err.println("通过deviceNo查询deviceId时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void deviceOnlineWithInfo(Long deviceId, Integer deviceNo, Integer batTimes, Integer capTimes, Integer treatmentStatus) {
        // System.out.println("设备上线: " + deviceId);
        try {
            // 先通过deviceNo查询数据库，获取实际的deviceId
            Long actualDeviceId = null;
            if (deviceNo != null) {
                // System.out.println("通过设备编号查询: " + deviceNo);
                actualDeviceId = getDeviceIdByDeviceNo(deviceNo);
                if (actualDeviceId == null) {
                    System.err.println("无法获取有效的deviceId，跳过处理");
                    return;
                }
            } else {
                actualDeviceId = deviceId;
            }
            
            // 更新数据库 - 一次查询，一次更新
            Device device = deviceService.getById(actualDeviceId);
            if (device != null) {
                // System.out.println("找到设备，当前状态: " + device.getStatus());
                
                // 在更新之前保存旧的治疗状态，用于检测变化
                Integer oldTreatmentStatus = device.getTreatmentStatus();
                // System.out.println("旧的治疗状态: " + oldTreatmentStatus);
                
                // 同时更新状态和新字段
                device.setStatus("online");
                if (deviceNo != null) {
                    device.setDeviceNo(deviceNo);
                    // System.out.println("设置设备编号: " + deviceNo);
                }
                if (batTimes != null) {
                    device.setBatTimes(batTimes);
                    // System.out.println("设置拍子使用次数: " + batTimes);
                }
                if (capTimes != null) {
                    device.setCapTimes(capTimes);
                    // System.out.println("设置电容使用次数: " + capTimes);
                }
                if (treatmentStatus != null) {
                    device.setTreatmentStatus(treatmentStatus);
                    // System.out.println("设置治疗状态: " + treatmentStatus + " (" + (treatmentStatus == 1 ? "刺激状态" : "非刺激状态") + ")");
                }
                
                boolean updateResult = deviceService.updateById(device);
                // System.out.println("数据库更新结果: " + updateResult);
                
                if (updateResult) {
                    // System.out.println("=== 设备信息更新成功 ===");
                    // System.out.println("设备ID: " + actualDeviceId);
                    // System.out.println("设备编号: " + deviceNo);
                    // System.out.println("拍子使用次数: " + batTimes);
                    // System.out.println("电容使用次数: " + capTimes);
                    // System.out.println("治疗状态: " + treatmentStatus + " (" + (treatmentStatus != null && treatmentStatus == 1 ? "刺激状态" : "非刺激状态") + ")");
                    
                    // 每次上线都推送状态更新（简化逻辑，确保推送）
                    // System.out.println("=== 推送上线状态更新 ===");
                    pushStatus(actualDeviceId, device.getStatus());
                    
                    // 如果治疗状态发生变化，额外记录日志
                    // if (treatmentStatus != null && oldTreatmentStatus != null && 
                    //     !oldTreatmentStatus.equals(treatmentStatus)) {
                    //     System.out.println("=== 检测到治疗状态变化 ===");
                    //     System.out.println("治疗状态变化: " + oldTreatmentStatus + " -> " + treatmentStatus);
                    // } else if (treatmentStatus != null && oldTreatmentStatus == null) {
                    //     System.out.println("=== 首次设置治疗状态 ===");
                    // } else {
                    //     System.out.println("=== 治疗状态未发生变化 ===");
                    //     System.out.println("旧治疗状态: " + oldTreatmentStatus);
                    //     System.out.println("新治疗状态: " + treatmentStatus);
                    // }
                } else {
                    System.err.println("设备信息更新失败");
                }
                
            } else {
                System.err.println("警告：未找到设备ID为 " + actualDeviceId + " 的设备记录");
            }
            
            // 更新Redis
            redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + actualDeviceId, "online");
            redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + actualDeviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
            // System.out.println("Redis状态更新完成");
            
            pushStatus(actualDeviceId, "online");
        } catch (Exception e) {
            System.err.println("设备上线处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deviceOfflineWithInfo(Long deviceId, Integer deviceNo, Integer batTimes, Integer capTimes, Integer treatmentStatus) {
        // System.out.println("设备下线: " + deviceId);
        try {
            // 先通过deviceNo查询数据库，获取实际的deviceId
            Long actualDeviceId = null;
            if (deviceNo != null) {
                // System.out.println("通过设备编号查询: " + deviceNo);
                actualDeviceId = getDeviceIdByDeviceNo(deviceNo);
                if (actualDeviceId == null) {
                    System.err.println("无法获取有效的deviceId，跳过处理");
                    return;
                }
            } else {
                actualDeviceId = deviceId;
            }
            
            // 更新数据库 - 一次查询，一次更新
            Device device = deviceService.getById(actualDeviceId);
            if (device != null) {
                // System.out.println("找到设备，当前状态: " + device.getStatus());
                
                // 在更新之前保存旧的治疗状态，用于检测变化
                Integer oldTreatmentStatus = device.getTreatmentStatus();
                // System.out.println("旧的治疗状态: " + oldTreatmentStatus);
                
                // 同时更新状态和新字段
                device.setStatus("offline");
                if (deviceNo != null) {
                    device.setDeviceNo(deviceNo);
                    // System.out.println("设置设备编号: " + deviceNo);
                }
                if (batTimes != null) {
                    device.setBatTimes(batTimes);
                    // System.out.println("设置拍子使用次数: " + batTimes);
                }
                if (capTimes != null) {
                    device.setCapTimes(capTimes);
                    // System.out.println("设置电容使用次数: " + capTimes);
                }
                if (treatmentStatus != null) {
                    device.setTreatmentStatus(treatmentStatus);
                    // System.out.println("设置治疗状态: " + treatmentStatus + " (" + (treatmentStatus == 1 ? "刺激状态" : "非刺激状态") + ")");
                }
                
                boolean updateResult = deviceService.updateById(device);
                // System.out.println("数据库更新结果: " + updateResult);
                
                if (updateResult) {
                    // System.out.println("=== 设备信息更新成功 ===");
                    // System.out.println("设备ID: " + actualDeviceId);
                    // System.out.println("设备编号: " + deviceNo);
                    // System.out.println("拍子使用次数: " + batTimes);
                    // System.out.println("电容使用次数: " + capTimes);
                    // System.out.println("治疗状态: " + treatmentStatus + " (" + (treatmentStatus != null && treatmentStatus == 1 ? "刺激状态" : "非刺激状态") + ")");
                    
                    // 每次下线都推送状态更新（简化逻辑，确保推送）
                    // System.out.println("=== 推送下线状态更新 ===");
                    pushStatus(actualDeviceId, device.getStatus());
                    
                    // 如果治疗状态发生变化，额外记录日志
                    // if (treatmentStatus != null && oldTreatmentStatus != null && 
                    //     !oldTreatmentStatus.equals(treatmentStatus)) {
                    //     System.out.println("=== 检测到治疗状态变化 ===");
                    //     System.out.println("治疗状态变化: " + oldTreatmentStatus + " -> " + treatmentStatus);
                    // } else if (treatmentStatus != null && oldTreatmentStatus == null) {
                    //     System.out.println("=== 首次设置治疗状态 ===");
                    // } else {
                    //     System.out.println("=== 治疗状态未发生变化 ===");
                    //     System.out.println("旧治疗状态: " + oldTreatmentStatus);
                    //     System.out.println("新治疗状态: " + treatmentStatus);
                    // }
                } else {
                    System.err.println("设备信息更新失败");
                }
                
            } else {
                System.err.println("警告：未找到设备ID为 " + actualDeviceId + " 的设备记录");
            }
            
            // 更新Redis
            redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + actualDeviceId, "offline");
            // System.out.println("Redis状态更新完成");
            
            pushStatus(actualDeviceId, "offline");
        } catch (Exception e) {
            System.err.println("设备下线处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deviceHeartbeatWithInfo(Long deviceId, Integer deviceNo, Integer batTimes, Integer capTimes, Integer treatmentStatus) {
        try {
            Date currentTime = new Date();
            
            // 先通过deviceNo查询数据库，获取实际的deviceId
            Long actualDeviceId = null;
            if (deviceNo != null) {
                // System.out.println("通过设备编号查询: " + deviceNo);
                actualDeviceId = getDeviceIdByDeviceNo(deviceNo);
                if (actualDeviceId == null) {
                    System.err.println("无法获取有效的deviceId，跳过处理");
                    return;
                }
            } else {
                actualDeviceId = deviceId;
            }
            
            // 更新Redis心跳时间
            redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY + actualDeviceId, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
            // System.out.println("Redis心跳更新完成");
            
            // 更新数据库 - 一次查询，一次更新
            Device device = deviceService.getById(actualDeviceId);
            if (device != null) {
                // System.out.println("找到设备，当前状态: " + device.getStatus());
                
                // 在更新之前保存旧的治疗状态，用于检测变化
                Integer oldTreatmentStatus = device.getTreatmentStatus();
                // System.out.println("旧的治疗状态: " + oldTreatmentStatus);
                
                // 同时更新状态、心跳时间和新字段
                if (!"online".equals(device.getStatus())) {
                    device.setStatus("online");
                    // System.out.println("更新设备状态为online");
                }
                
                device.setLastHeartbeat(currentTime);
                
                if (deviceNo != null) {
                    device.setDeviceNo(deviceNo);
                    // System.out.println("设置设备编号: " + deviceNo);
                }
                if (batTimes != null) {
                    device.setBatTimes(batTimes);
                    // System.out.println("设置拍子使用次数: " + batTimes);
                }
                if (capTimes != null) {
                    device.setCapTimes(capTimes);
                    // System.out.println("设置电容使用次数: " + capTimes);
                }
                if (treatmentStatus != null) {
                    device.setTreatmentStatus(treatmentStatus);
                    // System.out.println("设置治疗状态: " + treatmentStatus + " (" + (treatmentStatus == 1 ? "刺激状态" : "非刺激状态") + ")");
                }
                
                boolean updateResult = deviceService.updateById(device);
                // System.out.println("数据库心跳时间更新结果: " + updateResult);
                
                if (updateResult) {
                    // System.out.println("=== 设备信息更新成功 ===");
                    // System.out.println("设备ID: " + actualDeviceId);
                    // System.out.println("设备编号: " + deviceNo);
                    // System.out.println("拍子使用次数: " + batTimes);
                    // System.out.println("电容使用次数: " + capTimes);
                    // System.out.println("治疗状态: " + treatmentStatus + " (" + (treatmentStatus != null && treatmentStatus == 1 ? "刺激状态" : "非刺激状态") + ")");
                    // System.out.println("最后心跳时间: " + currentTime);
                    
                    // 每次心跳都推送状态更新（简化逻辑，确保推送）
                    // System.out.println("=== 推送心跳状态更新 ===");
                    // System.out.println("设备ID: " + actualDeviceId);
                    // System.out.println("当前状态: " + device.getStatus());
                    // System.out.println("当前治疗状态: " + treatmentStatus);
                    pushStatus(actualDeviceId, device.getStatus());
                    
                    // 如果治疗状态发生变化，额外记录日志
                    // if (treatmentStatus != null && oldTreatmentStatus != null && 
                    //     !oldTreatmentStatus.equals(treatmentStatus)) {
                    //     System.out.println("=== 检测到治疗状态变化 ===");
                    //     System.out.println("治疗状态变化: " + oldTreatmentStatus + " -> " + treatmentStatus);
                    // } else if (treatmentStatus != null && oldTreatmentStatus == null) {
                    //     System.out.println("=== 首次设置治疗状态 ===");
                    // } else {
                    //     System.out.println("=== 治疗状态未发生变化 ===");
                    //     System.out.println("旧治疗状态: " + oldTreatmentStatus);
                    //     System.out.println("新治疗状态: " + treatmentStatus);
                    // }
                } else {
                    System.err.println("设备信息更新失败");
                }
                
            } else {
                System.err.println("警告：未找到设备ID为 " + actualDeviceId + " 的设备记录");
            }
            
        } catch (Exception e) {
            System.err.println("设备心跳处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 定时任务：检查心跳超时设备自动下线（Redis方式）
    @Scheduled(fixedDelay = 60000)
    public void checkHeartbeatTimeout() {
        try {
            // 检查Redis连接（确保及时关闭连接归还给连接池）
            org.springframework.data.redis.connection.RedisConnection conn = null;
            try {
                conn = redisTemplate.getConnectionFactory().getConnection();
                conn.ping();
                // System.out.println("Redis连接正常");
            } catch (Exception e) {
                System.err.println("Redis连接失败，跳过心跳检查: " + e.getMessage());
                return;
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (Exception ignore) {}
                }
            }
            
            Set<String> keys = scanKeys("device:heartbeat:*", 200);
            long now = System.currentTimeMillis();
            int timeoutCount = 0;
            
            if (keys != null) {
                // System.out.println("检查 " + keys.size() + " 个设备的心跳状态");
                for (String key : keys) {
                    try {
                        String value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            long last = Long.parseLong(value);
                            Long deviceId = Long.parseLong(key.substring("device:heartbeat:".length()));
                            long timeDiff = now - last;
                            
                            // System.out.println("设备 " + deviceId + " 最后心跳时间: " + (timeDiff / 1000) + " 秒前");
                            
                            if (timeDiff > HEARTBEAT_TIMEOUT * 1000) {
                                // System.out.println("设备 " + deviceId + " 心跳超时，自动下线");
                                
                                // 调用新的方法：心跳超时处理（包含刺激状态重置）
                                handleHeartbeatTimeout(deviceId);
                                
                                timeoutCount++;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("检查设备心跳时发生异常: " + e.getMessage());
                    }
                }
                // System.out.println("心跳检查完成，超时设备数: " + timeoutCount);
            }
            
        } catch (Exception e) {
            System.err.println("心跳超时检查异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 定时任务：检查WebSocket连接状态，处理设备意外断开
     * 每30秒检查一次，比心跳检查更频繁
     */
    @Scheduled(fixedDelay = 30000)
    public void checkWebSocketConnectionStatus() {
        try {
            // 先清理断开的连接
            DeviceWebSocketServer.cleanupDisconnectedSessions();
            
            // 获取所有在线设备
            Set<String> statusKeys = scanKeys("device:status:*", 200);
            if (statusKeys != null && !statusKeys.isEmpty()) {
                // System.out.println("检查 " + statusKeys.size() + " 个设备的WebSocket连接状态");
                for (String key : statusKeys) {
                    try {
                        Long deviceId = Long.parseLong(key.substring("device:status:".length()));
                        String status = redisTemplate.opsForValue().get(key);
                        if (!"online".equals(status)) {
                            continue;
                        }
                        
                        // 检查该设备是否有活跃的WebSocket连接
                        boolean hasActiveConnection = DeviceWebSocketServer.hasActiveConnection(deviceId);
                        
                        if (!hasActiveConnection) {
                            // System.out.println("设备 " + deviceId + " WebSocket连接已断开，自动下线");
                            
                            // 处理WebSocket连接断开
                            handleConnectionDisconnect(deviceId);
                        }
                        
                    } catch (Exception e) {
                        System.err.println("检查设备 " + key + " WebSocket连接状态时发生异常: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("WebSocket连接状态检查异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Set<String> scanKeys(String pattern, long count) {
        Set<String> result = new java.util.HashSet<>();
        org.springframework.data.redis.connection.RedisConnection conn = null;
        Cursor<byte[]> cursor = null;
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(count).build();
            conn = redisTemplate.getConnectionFactory().getConnection();
            cursor = conn.scan(options);
            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                result.add(new String(key, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.err.println("SCAN keys 失败: " + e.getMessage() + "，重试一次");
            // 轻量重试一次
            try {
                if (cursor != null) { try { cursor.close(); } catch (Exception ignore) {} }
                if (conn != null) { try { conn.close(); } catch (Exception ignore) {} }
                Thread.sleep(100);
                conn = redisTemplate.getConnectionFactory().getConnection();
                cursor = conn.scan(ScanOptions.scanOptions().match(pattern).count(count).build());
                while (cursor.hasNext()) {
                    byte[] key = cursor.next();
                    result.add(new String(key, StandardCharsets.UTF_8));
                }
            } catch (Exception ex) {
                System.err.println("SCAN keys 二次失败: " + ex.getMessage());
            }
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignore) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (Exception ignore) {}
            }
        }
        return result;
    }
    
    /**
     * 处理设备心跳超时
     * 将设备状态变为离线，并将刺激状态重置为0
     */
    private void handleHeartbeatTimeout(Long deviceId) {
        try {
            // 查询设备信息
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                // System.out.println("找到设备: " + device.getSn());
                // System.out.println("当前状态: " + device.getStatus());
                // System.out.println("当前刺激状态: " + device.getTreatmentStatus());
                
                // 更新设备状态为离线
                device.setStatus("offline");
                // System.out.println("设置设备状态为: offline");
                
                // 重置刺激状态为0（非刺激状态）
                Integer oldTreatmentStatus = device.getTreatmentStatus();
                device.setTreatmentStatus(0);
                // System.out.println("重置刺激状态为: 0 (非刺激状态)");
                
                // 更新数据库
                boolean updateResult = deviceService.updateById(device);
                if (updateResult) {
                    // 如果治疗状态发生变化，推送治疗状态变化消息
                    if (oldTreatmentStatus != null && !oldTreatmentStatus.equals(0)) {
                        pushStatus(deviceId, "offline");
                    }
                } else {
                    System.err.println("设备心跳超时处理失败，数据库更新失败");
                }
            } else {
                System.err.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
            }
            
            // 更新Redis状态为离线
            redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "offline");
            
            // 推送状态变更消息
            pushStatus(deviceId, "offline");
            
        } catch (Exception e) {
            System.err.println("处理设备心跳超时时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理WebSocket连接断开
     * 将设备状态变为离线，并将刺激状态重置为0
     */
    @Override
    public void handleConnectionDisconnect(Long deviceId) {
        try {
            // 查询设备信息
            Device device = deviceService.getById(deviceId);
            if (device != null) {
                // System.out.println("找到设备: " + device.getSn());
                // System.out.println("当前状态: " + device.getStatus());
                // System.out.println("当前刺激状态: " + device.getTreatmentStatus());
                
                // 保存旧的治疗状态，用于检测变化
                Integer oldTreatmentStatus = device.getTreatmentStatus();
                // System.out.println("保存旧的治疗状态: " + oldTreatmentStatus);
                
                // 更新设备状态为离线
                device.setStatus("offline");
                // System.out.println("设置设备状态为: offline");
                
                // 重置刺激状态为0（非刺激状态）
                device.setTreatmentStatus(0);
                // System.out.println("重置刺激状态为: 0 (非刺激状态)");
                
                // 更新数据库
                // System.out.println("开始更新数据库...");
                boolean updateResult = deviceService.updateById(device);
                // System.out.println("数据库更新结果: " + updateResult);
                
                if (updateResult) {
                    // 更新Redis状态为离线
                    redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "offline");
                    
                    // 推送状态变更消息（包含最新的治疗状态）
                    pushStatus(deviceId, "offline");
                    
                } else {
                    System.err.println("WebSocket连接断开处理失败，数据库更新失败");
                }
            } else {
                System.err.println("警告：未找到设备ID为 " + deviceId + " 的设备记录");
                
                // 即使找不到设备，也要更新Redis状态为离线
                redisTemplate.opsForValue().set(DEVICE_STATUS_KEY + deviceId, "offline");
                
                // 推送状态变更消息（使用默认值）
                pushStatus(deviceId, "offline");
            }
            
        } catch (Exception e) {
            System.err.println("处理WebSocket连接断开时发生异常: " + e.getMessage());
            System.err.println("异常类型: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    private void pushStatus(Long deviceId, String status) {
        try {
            // System.out.println("=== 开始推送状态消息 ===");
            // System.out.println("设备ID: " + deviceId);
            // System.out.println("状态: " + status);
            // System.out.println("时间: " + new Date());
            
            // 添加小延迟，避免快速连续推送导致的状态冲突
            try {
                Thread.sleep(50); // 50毫秒延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 查询设备信息，获取设备编号和刺激状态
            // System.out.println("开始查询设备信息...");
            Device device = deviceService.getById(deviceId);
            
            Map<String, Object> msg = new HashMap<>();
            msg.put("deviceId", deviceId);
            msg.put("status", status);
            
            if (device != null) {
                Integer treatmentStatus = device.getTreatmentStatus() != null ? device.getTreatmentStatus() : 0;
                msg.put("treatmentStatus", treatmentStatus);
                // System.out.println("设备信息查询成功");
                // System.out.println("设备编号: " + device.getDeviceNo());
                // System.out.println("治疗状态: " + treatmentStatus);
                // System.out.println("推送状态消息包含设备ID: " + deviceId + ", 状态: " + status + ", 治疗状态: " + treatmentStatus);
            } else {
                msg.put("treatmentStatus", 0);
                // System.out.println("未找到设备信息，设置默认治疗状态为0");
            }
            
            // 序列化消息
            // System.out.println("开始序列化消息...");
            String json = objectMapper.writeValueAsString(msg);
            // System.out.println("消息序列化成功，长度: " + json.length());
            
            // 广播消息
            // System.out.println("开始广播消息...");
            DeviceWebSocketServer.broadcast(json);
            // System.out.println("消息广播成功");
            // System.out.println("推送状态消息: " + json);
            
            // System.out.println("=== 状态消息推送完成 ===");
            
        } catch (Exception e) {
            System.err.println("推送状态消息异常: " + e.getMessage());
            System.err.println("异常类型: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }
} 