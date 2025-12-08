package cc.mrbird.febs.system.websocket;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import org.springframework.context.ApplicationContext;
import cc.mrbird.febs.system.service.PrescriptionExecutionStatusService;

@Component
@ServerEndpoint("/ws/prescriptionexecution")
public class PrescriptionExecutionWebSocketServer {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 用于获取Spring服务
    private static ApplicationContext applicationContext;
    
    // ========== 新增：注入服务用于上线推送 ==========
    private static cc.mrbird.febs.system.service.PrescriptionExecutionService prescriptionExecutionService;
    private static cc.mrbird.febs.system.service.PrescriptionExecutionNotificationService notificationService;
    
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }
    
    // ========== 新增：注入处方执行服务 ==========
    @org.springframework.beans.factory.annotation.Autowired
    public void setPrescriptionExecutionService(cc.mrbird.febs.system.service.PrescriptionExecutionService service) {
        PrescriptionExecutionWebSocketServer.prescriptionExecutionService = service;
    }
    
    // ========== 新增：注入通知服务 ==========
    @org.springframework.beans.factory.annotation.Autowired
    public void setPrescriptionExecutionNotificationService(cc.mrbird.febs.system.service.PrescriptionExecutionNotificationService service) {
        PrescriptionExecutionWebSocketServer.notificationService = service;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("=== 上位机连接建立 ===");
        System.out.println("端点: /ws/prescriptionexecution");
        System.out.println("会话ID: " + session.getId());
        System.out.println("当前连接数: " + sessions.size());
        
        // ========== 新增：推送所有待下发的处方执行记录 ==========
        pushPendingExecutions(session);
    }
    
    // ========== 新增：推送所有待下发记录的方法 ==========
    private void pushPendingExecutions(Session session) {
        if (prescriptionExecutionService == null || notificationService == null) {
            System.err.println("服务未注入，跳过待下发记录推送");
            return;
        }
        
        try {
            System.out.println("=== 开始查询待下发的处方执行记录 ===");
            
            // 查询所有 status=0 的记录
            java.util.List<cc.mrbird.febs.system.domain.PrescriptionExecution> pendingExecutions = 
                prescriptionExecutionService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cc.mrbird.febs.system.domain.PrescriptionExecution>()
                        .eq(cc.mrbird.febs.system.domain.PrescriptionExecution::getStatus, 0)
                        .orderByAsc(cc.mrbird.febs.system.domain.PrescriptionExecution::getCreatedAt)
                );
            
            if (pendingExecutions == null || pendingExecutions.isEmpty()) {
                System.out.println("没有待下发的处方执行记录");
                return;
            }
            
            System.out.println("找到 " + pendingExecutions.size() + " 条待下发记录，开始推送...");
            
            int successCount = 0;
            for (cc.mrbird.febs.system.domain.PrescriptionExecution execution : pendingExecutions) {
                try {
                    // 构建通知消息
                    cc.mrbird.febs.system.domain.PrescriptionExecutionNotification notification = 
                        notificationService.buildNotification(execution);
                    
                    String message = objectMapper.writeValueAsString(notification);
                    
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                        successCount++;
                        System.out.println("已推送执行记录ID: " + execution.getId() + 
                                         ", 设备ID: " + execution.getDeviceId());
                        Thread.sleep(50); // 间隔50ms，避免消息过载
                    } else {
                        System.err.println("会话已关闭，停止推送");
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("推送执行记录 " + execution.getId() + " 失败: " + e.getMessage());
                }
            }
            
            System.out.println("=== 待下发记录推送完成 ===");
            System.out.println("成功推送: " + successCount + " 条");
            
        } catch (Exception e) {
            System.err.println("推送待下发记录异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        // System.out.println("=== 处方执行WebSocket连接断开 ===");
        // System.out.println("会话ID: " + session.getId());
        // System.out.println("当前总连接数: " + sessions.size());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("=== 收到WebSocket客户端消息 ===");
        System.out.println("会话ID: " + session.getId());
        
        try {
            // 解析客户端消息
            JsonNode jsonNode = objectMapper.readTree(message);
            
            // 判断消息类型（支持两种格式）
            String type = null;
            if (jsonNode.has("type")) {
                type = jsonNode.get("type").asText();
            } else if (jsonNode.has("messageType")) {
                type = jsonNode.get("messageType").asText();
            }
            
            if (type == null) {
                System.err.println("消息缺少 type 或 messageType 字段");
                return;
            }
            
            System.out.println("消息类型: " + type);
            
            // ========== 新增：处理上位机确认收到 ==========
            if ("execution_received".equals(type) || "PRESCRIPTION_EXECUTION_RECEIVED".equals(type)) {
                handleExecutionReceived(jsonNode, session);
            } 
            // ========== 处理状态更新（原有逻辑） ==========
            else if ("execution_status_update".equals(type) || "PRESCRIPTION_STATUS_UPDATE".equals(type)) {
                handlePrescriptionStatusUpdate(jsonNode, session);
            } 
            else {
                System.out.println("未知的消息类型: " + type);
            }
            
        } catch (Exception e) {
            System.err.println("处理WebSocket消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ========== 新增：处理上位机确认收到消息 ==========
    private void handleExecutionReceived(JsonNode jsonNode, Session session) {
        try {
            Long executionId = jsonNode.get("executionId").asLong();
            Integer deviceNo = jsonNode.has("deviceNo") ? jsonNode.get("deviceNo").asInt() : null;
            String receivedTime = jsonNode.has("receivedTime") ? jsonNode.get("receivedTime").asText() : null;
            String clientInfo = jsonNode.has("clientInfo") ? jsonNode.get("clientInfo").asText() : null;
            
            System.out.println("=== 收到上位机确认 ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("设备编号: " + deviceNo);
            System.out.println("接收时间: " + receivedTime);
            System.out.println("客户端信息: " + clientInfo);
            
            if (prescriptionExecutionService != null) {
                cc.mrbird.febs.system.domain.PrescriptionExecution execution = 
                    prescriptionExecutionService.getById(executionId);
                
                if (execution != null && execution.getStatus() == 0) {
                    // 更新状态为"已下发"，同时更新进度描述
                    execution.setStatus(1);
                    execution.setProgress("已下发到设备");
                    execution.setUpdatedAt(new Date());
                    boolean success = prescriptionExecutionService.updateById(execution);
                    
                    if (success) {
                        System.out.println("执行记录状态更新为：已下发(1)，进度：已下发到设备");
                        sendConfirmationReply(session, executionId, "SUCCESS", "已确认收到");
                    } else {
                        System.err.println("更新执行记录状态失败");
                        sendConfirmationReply(session, executionId, "ERROR", "数据库更新失败");
                    }
                } else if (execution != null && execution.getStatus() != 0) {
                    System.out.println("执行记录已被确认，当前状态: " + execution.getStatus());
                    sendConfirmationReply(session, executionId, "ALREADY_CONFIRMED", 
                                        "该记录已确认，当前状态: " + execution.getStatus());
                } else {
                    System.err.println("执行记录不存在");
                    sendConfirmationReply(session, executionId, "NOT_FOUND", "执行记录不存在");
                }
            } else {
                System.err.println("prescriptionExecutionService 未注入");
                sendConfirmationReply(session, executionId, "ERROR", "服务不可用");
            }
            
        } catch (Exception e) {
            System.err.println("处理上位机确认失败: " + e.getMessage());
            e.printStackTrace();
            try {
                sendConfirmationReply(session, null, "ERROR", "处理异常: " + e.getMessage());
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 处理处方执行记录接收确认
     */
    private void handlePrescriptionExecutionReceived(JsonNode jsonNode, Session session) {
        try {
            Long executionId = jsonNode.get("executionId").asLong();
            String status = jsonNode.get("status").asText();
            String clientInfo = jsonNode.get("clientInfo").asText();
            Date receivedTime = new Date();
            
            // System.out.println("=== 处方执行记录接收确认 ===");
            // System.out.println("执行记录ID: " + executionId);
            // System.out.println("状态: " + status);
            // System.out.println("客户端信息: " + clientInfo);
            // System.out.println("接收时间: " + receivedTime);
            
            // 更新处方执行记录状态
            updatePrescriptionExecutionStatus(executionId, status, clientInfo, receivedTime);
            
            // 发送确认回复
            sendConfirmationReply(session, executionId, "SUCCESS");
            
        } catch (Exception e) {
            // System.err.println("处理处方执行记录接收确认失败: " + e.getMessage());
            // e.printStackTrace();
            
            // 发送错误回复
            try {
                sendConfirmationReply(session, null, "ERROR");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }
    
    /**
     * 更新处方执行记录状态
     */
    private void updatePrescriptionExecutionStatus(Long executionId, String status, String clientInfo, Date receivedTime) {
        try {
            // System.out.println("=== 更新处方执行记录状态 ===");
            // System.out.println("执行记录ID: " + executionId);
            // System.out.println("新状态: " + status);
            // System.out.println("客户端信息: " + clientInfo);
            // System.out.println("接收时间: " + receivedTime);
            
            // 通过ApplicationContext获取服务
            if (applicationContext != null) {
                PrescriptionExecutionStatusService statusService = applicationContext.getBean(PrescriptionExecutionStatusService.class);
                boolean success = statusService.updateExecutionStatus(executionId, status, clientInfo, receivedTime);
                
                // if (success) {
                //     System.out.println("=== 数据库状态更新成功 ===");
                // } else {
                //     System.err.println("=== 数据库状态更新失败 ===");
                // }
            } else {
                // System.err.println("ApplicationContext未初始化，无法更新数据库状态");
            } 
            
        } catch (Exception e) {
            // System.err.println("更新处方执行记录状态失败: " + e.getMessage());
            // e.printStackTrace();
        }
    }
    
    /**
     * 处理处方状态更新
     */
    private void handlePrescriptionStatusUpdate(JsonNode jsonNode, Session session) {
        try {
            Long executionId = jsonNode.get("executionId").asLong();
            String status = jsonNode.get("status").asText();
            Long deviceId = jsonNode.has("deviceId") ? jsonNode.get("deviceId").asLong() : null;
            Integer progress = jsonNode.has("progress") ? jsonNode.get("progress").asInt() : null;
            String message = jsonNode.has("message") ? jsonNode.get("message").asText() : null;
            Date updateTime = new Date();
            
            // System.out.println("=== 处方状态更新 ===");
            // System.out.println("执行记录ID: " + executionId);
            // System.out.println("设备ID: " + deviceId);
            // System.out.println("状态: " + status);
            // System.out.println("进度: " + progress);
            // System.out.println("消息: " + message);
            // System.out.println("更新时间: " + updateTime);
            
            // 更新处方执行记录状态
            updatePrescriptionExecutionStatusWithProgress(executionId, status, deviceId, progress, message, updateTime);
            
            // 发送确认回复
            sendStatusUpdateConfirmation(session, executionId, "SUCCESS");
            
        } catch (Exception e) {
            // System.err.println("处理处方状态更新失败: " + e.getMessage());
            // e.printStackTrace();
            
            // 发送错误回复
            try {
                sendStatusUpdateConfirmation(session, null, "ERROR");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }
    
    /**
     * 更新处方执行记录状态（包含进度信息）
     */
    private void updatePrescriptionExecutionStatusWithProgress(Long executionId, String status, Long deviceId, Integer progress, String message, Date updateTime) {
        try {
            // System.out.println("=== 开始更新处方执行记录状态（含进度） ===");
            // System.out.println("执行记录ID: " + executionId);
            // System.out.println("设备ID: " + deviceId);
            // System.out.println("新状态: " + status);
            // System.out.println("进度: " + progress);
            // System.out.println("消息: " + message);
            // System.out.println("更新时间: " + updateTime);
            
            // 通过ApplicationContext获取服务
            if (applicationContext != null) {
                PrescriptionExecutionStatusService statusService = applicationContext.getBean(PrescriptionExecutionStatusService.class);
                boolean success = statusService.updateExecutionStatusWithProgress(executionId, status, deviceId, progress, message, updateTime);
                
                // if (success) {
                //     System.out.println("=== 数据库状态更新成功 ===");
                // } else {
                //     System.err.println("=== 数据库状态更新失败 ===");
                // }
            } else {
                // System.err.println("ApplicationContext未初始化，无法更新数据库状态");
            }
            
        } catch (Exception e) {
            // System.err.println("更新处方执行记录状态失败: " + e.getMessage());
            // e.printStackTrace();
        }
    }
    
    /**
     * 发送状态更新确认回复
     */
    private void sendStatusUpdateConfirmation(Session session, Long executionId, String result) {
        try {
            Map<String, Object> reply = new HashMap<>();
            reply.put("messageType", "PRESCRIPTION_STATUS_UPDATE_CONFIRMATION");
            reply.put("executionId", executionId);
            reply.put("result", result);
            reply.put("timestamp", new Date());
            reply.put("serverInfo", "TMS Backend Server");
            
            String replyMessage = objectMapper.writeValueAsString(reply);
            session.getBasicRemote().sendText(replyMessage);
            
            // System.out.println("=== 发送状态更新确认回复 ===");
            // System.out.println("会话ID: " + session.getId());
            // System.out.println("执行记录ID: " + executionId);
            // System.out.println("结果: " + result);
            
        } catch (Exception e) {
            // System.err.println("发送状态更新确认回复失败: " + e.getMessage());
            // e.printStackTrace();
        }
    }
    
    /**
     * 发送确认回复（新版本，支持消息参数）
     */
    private void sendConfirmationReply(Session session, Long executionId, String result, String message) {
        try {
            Map<String, Object> reply = new HashMap<>();
            reply.put("type", "execution_received_ack");
            reply.put("executionId", executionId);
            reply.put("result", result);
            reply.put("message", message);
            reply.put("timestamp", new Date());
            reply.put("serverInfo", "TMS Backend Server");
            
            String replyMessage = objectMapper.writeValueAsString(reply);
            session.getBasicRemote().sendText(replyMessage);
            
            System.out.println("=== 发送确认回复 ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("结果: " + result);
            System.out.println("消息: " + message);
            
        } catch (Exception e) {
            System.err.println("发送确认回复失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送确认回复（兼容旧版本）
     */
    private void sendConfirmationReply(Session session, Long executionId, String result) {
        sendConfirmationReply(session, executionId, result, null);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        // System.out.println("处方执行WebSocket发生错误：" + session.getId());
        // error.printStackTrace();
    }

    /**
     * 推送处方执行记录创建通知给所有连接的客户端
     */
    public static void broadcastPrescriptionExecutionCreated(Object data) {
        try {
            // System.out.println("=== 开始WebSocket广播 ===");
            // System.out.println("当前连接数: " + sessions.size());
            
            String message = objectMapper.writeValueAsString(data);
            // System.out.println("消息内容长度: " + message.length() + " 字符");
            
            // 尝试解析消息内容，输出设备编号信息
            // try {
            //     JsonNode jsonNode = objectMapper.readTree(message);
            //     if (jsonNode.has("deviceInfo") && jsonNode.get("deviceInfo").has("deviceNo")) {
            //         Integer deviceNo = jsonNode.get("deviceInfo").get("deviceNo").asInt();
            //         System.out.println("广播消息包含设备编号: " + deviceNo);
            //     } else {
            //         System.out.println("广播消息中未找到设备编号信息");
            //     }
            // } catch (Exception e) {
            //     System.out.println("解析广播消息内容失败: " + e.getMessage());
            // }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                        successCount++;
                        // System.out.println("成功发送到会话: " + session.getId());
                    } catch (IOException e) {
                        failCount++;
                        // System.out.println("发送失败到会话: " + session.getId() + ", 错误: " + e.getMessage());
                        // e.printStackTrace();
                    }
                } else {
                    // System.out.println("跳过关闭的会话: " + session.getId());
                }
            }
            
            // System.out.println("=== WebSocket广播完成 ===");
            // System.out.println("成功发送: " + successCount + " 个客户端");
            // System.out.println("发送失败: " + failCount + " 个客户端");
            // System.out.println("总连接数: " + sessions.size());
            
        } catch (Exception e) {
            // System.err.println("=== WebSocket广播异常 ===");
            // System.err.println("错误信息: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    /**
     * 获取当前连接的客户端数量
     */
    public static int getConnectedCount() {
        return sessions.size();
    }
} 