package cc.mrbird.febs.system.websocket;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cc.mrbird.febs.system.service.DeviceStatusService;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

@Component
@ServerEndpoint("/ws/device")
public class DeviceWebSocketServer {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private static DeviceStatusService deviceStatusService;
    
    // 添加session到设备ID的映射关系
    private static final Map<Session, Long> sessionDeviceMap = new ConcurrentHashMap<>();

    // 通过Spring注入DeviceStatusService
    @org.springframework.beans.factory.annotation.Autowired
    public void setDeviceStatusService(DeviceStatusService service) {
        DeviceWebSocketServer.deviceStatusService = service;
        // System.out.println("=== DeviceStatusService 注入成功 ===");
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        // System.out.println("=== 设备WebSocket连接建立 ===");
        // System.out.println("端点: /ws/device");
        // System.out.println("会话ID: " + session.getId());
        // System.out.println("当前设备连接数: " + sessions.size());
        // System.out.println("说明：此端点用于接收设备状态消息（心跳、上线、下线等）");
        
        // 打印当前连接状态
        // printConnectionStatus();
    }
    
    /**
     * 记录设备连接，如果同一设备已有连接，则关闭旧连接
     * @param session WebSocket会话
     * @param deviceId 设备ID
     */
    public static void recordDeviceConnection(Session session, Long deviceId) {
        if (deviceId == null) {
            // System.out.println("设备ID为空，跳过连接记录");
            return;
        }
        
        // System.out.println("=== 记录设备连接 ===");
        // System.out.println("Session ID: " + session.getId());
        // System.out.println("设备ID: " + deviceId);
        
        // 检查是否已有同一设备的连接
        Session existingSession = null;
        for (Map.Entry<Session, Long> entry : sessionDeviceMap.entrySet()) {
            if (deviceId.equals(entry.getValue())) {
                existingSession = entry.getKey();
                break;
            }
        }
        
        if (existingSession != null && !existingSession.equals(session)) {
            // System.out.println("=== 检测到同一设备重复连接，关闭旧连接 ===");
            // System.out.println("旧Session ID: " + existingSession.getId());
            // System.out.println("新Session ID: " + session.getId());
            // System.out.println("设备ID: " + deviceId);
            
            try {
                // 关闭旧连接
                if (existingSession.isOpen()) {
                    existingSession.close();
                    // System.out.println("旧连接已关闭");
                }
                
                // 从映射中移除旧连接
                sessionDeviceMap.remove(existingSession);
                // System.out.println("旧连接映射已移除");
                
            } catch (Exception e) {
                // System.err.println("关闭旧连接时发生异常: " + e.getMessage());
                // e.printStackTrace();
            }
        }
        
        // 记录新连接
        sessionDeviceMap.put(session, deviceId);
        // System.out.println("新连接映射已记录: Session " + session.getId() + " -> Device " + deviceId);
        // System.out.println("当前连接映射数: " + sessionDeviceMap.size());
    }

    @OnClose
    public void onClose(Session session) {
        // 获取断开连接的设备ID
        Long deviceId = sessionDeviceMap.get(session);
        
        if (deviceId != null) {
            // System.out.println("=== 设备WebSocket连接断开，处理设备离线 ===");
            // System.out.println("Session ID: " + session.getId());
            // System.out.println("设备ID: " + deviceId);
            
            // 调用设备离线处理（包含刺激状态重置）
            if (deviceStatusService != null) {
                try {
                    // System.out.println("调用设备离线处理，重置刺激状态");
                    deviceStatusService.handleConnectionDisconnect(deviceId);
                } catch (Exception e) {
                    // System.err.println("处理设备连接断开时发生异常: " + e.getMessage());
                    // e.printStackTrace();
                }
            } else {
                // System.err.println("DeviceStatusService未注入，无法处理设备离线");
            }
            
            // 移除session映射
            sessionDeviceMap.remove(session);
            // System.out.println("已移除session映射: Session " + session.getId() + " -> Device " + deviceId);
        } else {
            // System.out.println("=== WebSocket连接断开，但未找到对应的设备ID ===");
            // System.out.println("Session ID: " + session.getId());
        }
        
        // 移除session
        sessions.remove(session);
        // System.out.println("当前设备连接数: " + sessions.size());
        // System.out.println("当前session映射数: " + sessionDeviceMap.size());
        
        // 打印当前连接状态
        // printConnectionStatus();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 输出收到的原始消息
        // System.out.println("=== 收到WebSocket消息 ===");
        // System.out.println("Session ID: " + session.getId());
        // System.out.println("消息内容: " + message);
        // System.out.println("消息长度: " + message.length());
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(message);
            
            // 检查DeviceStatusService是否已注入
            if (deviceStatusService == null) {
                // System.err.println("错误：DeviceStatusService 未注入，无法处理设备状态更新");
                return;
            }
            
            // 统一处理所有消息，支持新字段
            if (node.has("status") && node.has("deviceId")) {
                Long deviceId = node.get("deviceId").asLong();
                String status = node.get("status").asText();
                
                // System.out.println("解析后 deviceId: " + deviceId + ", status: " + status);
                
                // 记录session和设备ID的映射关系
                recordDeviceConnection(session, deviceId);
                // System.out.println("Session映射已更新: Session " + session.getId() + " -> Device " + deviceId);
                
                // 获取新字段的值
                Integer deviceNo = node.has("deviceNo") ? node.get("deviceNo").asInt() : null;
                Integer batTimes = node.has("batTimes") ? node.get("batTimes").asInt() : null;
                Integer capTimes = node.has("capTimes") ? node.get("capTimes").asInt() : null;
                Integer treatmentStatus = node.has("treatmentStatus") ? node.get("treatmentStatus").asInt() : null;
                
                // 输出新字段信息
                // if (deviceNo != null) System.out.println("设备编号: " + deviceNo);
                // if (batTimes != null) System.out.println("拍子使用次数: " + batTimes);
                // if (capTimes != null) System.out.println("电容使用次数: " + capTimes);
                // if (treatmentStatus != null) System.out.println("治疗状态: " + treatmentStatus);
                
                // 根据状态调用相应的方法（包含新字段）
                if ("online".equals(status)) {
                    // System.out.println("调用 deviceOnlineWithInfo 方法");
                    deviceStatusService.deviceOnlineWithInfo(deviceId, deviceNo, batTimes, capTimes, treatmentStatus);
                } else if ("heartbeat".equals(status)) {
                    // System.out.println("=== 处理心跳消息，准备更新治疗状态 ===");
                    // System.out.println("设备ID: " + deviceId);
                    // System.out.println("设备编号: " + deviceNo);
                    // System.out.println("拍子使用次数: " + batTimes);
                    // System.out.println("电容使用次数: " + capTimes);
                    // System.out.println("治疗状态: " + treatmentStatus + " (" + (treatmentStatus != null && treatmentStatus == 1 ? "刺激状态" : "非刺激状态") + ")");
                    
                    // System.out.println("调用 deviceHeartbeatWithInfo 方法");
                    deviceStatusService.deviceHeartbeatWithInfo(deviceId, deviceNo, batTimes, capTimes, treatmentStatus);
                    
                    // System.out.println("=== 心跳消息处理完成，治疗状态已更新 ===");
                } else if ("offline".equals(status)) {
                    // System.out.println("调用 deviceOfflineWithInfo 方法");
                    deviceStatusService.deviceOfflineWithInfo(deviceId, deviceNo, batTimes, capTimes, treatmentStatus);
                } else {
                    // System.out.println("未知状态: " + status);
                }
                
            } else if (node.has("messageType")) {
                // 处方相关消息，只记录日志，不处理
                // System.out.println("收到处方消息: " + node.get("messageType").asText());
                // System.out.println("处方消息暂不处理，仅记录日志");
            } else {
                // System.out.println("消息格式不符合要求，缺少必要字段");
            }
            
            // System.out.println("=== 消息处理完成 ===");
        } catch (Exception e) {
            // System.err.println("处理WebSocket消息时发生异常: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        // System.err.println("=== WebSocket发生错误 ===");
        // System.err.println("Session ID: " + session.getId());
        // System.err.println("错误信息: " + error.getMessage());
        // System.err.println("错误类型: " + error.getClass().getSimpleName());
        // error.printStackTrace();
        
        // 获取断开连接的设备ID
        Long deviceId = sessionDeviceMap.get(session);
        
        if (deviceId != null) {
            // System.err.println("=== WebSocket异常，处理设备离线 ===");
            // System.err.println("Session ID: " + session.getId());
            // System.err.println("设备ID: " + deviceId);
            
            // 调用设备离线处理（包含刺激状态重置）
            if (deviceStatusService != null) {
                try {
                    // System.err.println("调用设备离线处理，重置刺激状态");
                    deviceStatusService.handleConnectionDisconnect(deviceId);
                } catch (Exception e) {
                    // System.err.println("处理WebSocket异常时发生异常: " + e.getMessage());
                    // e.printStackTrace();
                }
            } else {
                // System.err.println("DeviceStatusService未注入，无法处理设备离线");
            }
            
            // 移除session映射
            sessionDeviceMap.remove(session);
            // System.err.println("已移除异常session映射: Session " + session.getId() + " -> Device " + deviceId);
        } else {
            // System.err.println("WebSocket异常，但未找到对应的设备ID");
        }
        
        // 移除session
        sessions.remove(session);
        // System.err.println("当前设备连接数: " + sessions.size());
        // System.err.println("当前session映射数: " + sessionDeviceMap.size());
        
        // 打印当前连接状态
        // printConnectionStatus();
    }

    // 推送消息给所有前端
    public static void broadcast(String message) {
        // System.out.println("=== 开始推送消息给所有前端 ===");
        // System.out.println("推送消息: " + message);
        // System.out.println("当前连接数: " + sessions.size());
        
        // 使用同步块防止并发写入冲突
        synchronized (DeviceWebSocketServer.class) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        // 检查session是否可写
                        if (session.getBasicRemote().getBatchingAllowed()) {
                            session.getBasicRemote().sendText(message);
                            // System.out.println("消息推送成功到 Session: " + session.getId());
                        } else {
                            // 如果session不可写，跳过
                            // System.out.println("Session " + session.getId() + " 不可写，跳过推送");
                        }
                    } catch (IllegalStateException e) {
                        // 处理状态冲突异常
                        // System.err.println("推送消息到 Session " + session.getId() + " 失败（状态冲突）: " + e.getMessage());
                        // 尝试关闭有问题的session
                        try {
                            session.close();
                            // System.out.println("已关闭状态冲突的 Session: " + session.getId());
                        } catch (IOException closeException) {
                            // System.err.println("关闭Session " + session.getId() + " 时发生异常: " + closeException.getMessage());
                        }
                    } catch (IOException e) {
                        // System.err.println("推送消息到 Session " + session.getId() + " 失败: " + e.getMessage());
                        // e.printStackTrace();
                    }
                } else {
                    // System.out.println("Session " + session.getId() + " 已关闭，跳过推送");
                }
            }
        }
        // System.out.println("=== 消息推送完成 ===");
    }
    
    // 获取当前连接数
    public static int getConnectionCount() {
        return sessions.size();
    }
    
    /**
     * 检查指定设备是否有活跃的WebSocket连接
     * @param deviceId 设备ID
     * @return true表示有活跃连接，false表示没有
     */
    public static boolean hasActiveConnection(Long deviceId) {
        if (deviceId == null) {
            return false;
        }
        
        // 遍历session映射，查找是否有该设备的活跃连接
        for (Map.Entry<Session, Long> entry : sessionDeviceMap.entrySet()) {
            Session session = entry.getKey();
            Long sessionDeviceId = entry.getValue();
            
            if (deviceId.equals(sessionDeviceId) && session.isOpen()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取指定设备的活跃连接数
     * @param deviceId 设备ID
     * @return 活跃连接数
     */
    public static int getDeviceConnectionCount(Long deviceId) {
        if (deviceId == null) {
            return 0;
        }
        
        int count = 0;
        for (Map.Entry<Session, Long> entry : sessionDeviceMap.entrySet()) {
            Session session = entry.getKey();
            Long sessionDeviceId = entry.getValue();
            
            if (deviceId.equals(sessionDeviceId) && session.isOpen()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 清理断开的连接
     * 移除已关闭的session映射
     */
    public static void cleanupDisconnectedSessions() {
        // System.out.println("=== 开始清理断开的连接 ===");
        // System.out.println("清理前session映射数: " + sessionDeviceMap.size());
        
        Iterator<Map.Entry<Session, Long>> iterator = sessionDeviceMap.entrySet().iterator();
        int cleanedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<Session, Long> entry = iterator.next();
            Session session = entry.getKey();
            Long deviceId = entry.getValue();
            
            if (!session.isOpen()) {
                // System.out.println("清理断开的连接: Session " + session.getId() + " -> Device " + deviceId);
                iterator.remove();
                cleanedCount++;
            }
        }
        
        // System.out.println("清理后session映射数: " + sessionDeviceMap.size());
        // System.out.println("清理的连接数: " + cleanedCount);
        // System.out.println("=== 连接清理完成 ===");
    }
    
    // 获取所有连接的Session ID
    public static String[] getAllSessionIds() {
        return sessions.stream().map(Session::getId).toArray(String[]::new);
    }

    /**
     * 获取当前所有设备的连接状态
     * 用于调试和监控
     */
    public static Map<Long, String> getConnectionStatus() {
        Map<Long, String> status = new HashMap<>();
        
        for (Map.Entry<Session, Long> entry : sessionDeviceMap.entrySet()) {
            Session session = entry.getKey();
            Long deviceId = entry.getValue();
            
            String sessionStatus = session.isOpen() ? "活跃" : "已断开";
            status.put(deviceId, sessionStatus + " (Session " + session.getId() + ")");
        }
        
        return status;
    }
    
    /**
     * 打印当前连接状态
     */
    public static void printConnectionStatus() {
        // System.out.println("=== 当前WebSocket连接状态 ===");
        // System.out.println("总连接数: " + sessions.size());
        // System.out.println("设备映射数: " + sessionDeviceMap.size());
        
        // Map<Long, String> status = getConnectionStatus();
        // if (status.isEmpty()) {
        //     System.out.println("没有设备连接");
        // } else {
        //     for (Map.Entry<Long, String> entry : status.entrySet()) {
        //         System.out.println("设备 " + entry.getKey() + ": " + entry.getValue());
        //     }
        // }
        // System.out.println("=== 连接状态打印完成 ===");
    }
} 