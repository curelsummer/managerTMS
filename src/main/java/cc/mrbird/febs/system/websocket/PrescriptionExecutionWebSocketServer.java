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
    
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        // System.out.println("=== 处方执行WebSocket连接建立 ===");
        // System.out.println("端点: /ws/prescriptionexecution");
        // System.out.println("会话ID: " + session.getId());
        // System.out.println("当前处方执行连接数: " + sessions.size());
        // System.out.println("说明：此端点用于处理处方执行相关消息");
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
        // System.out.println("=== 收到WebSocket客户端消息 ===");
        // System.out.println("会话ID: " + session.getId());
        // System.out.println("消息内容: " + message);
        
        try {
            // 解析客户端消息
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(message);
            
            String messageType = jsonNode.get("messageType").asText();
            
            if ("PRESCRIPTION_EXECUTION_RECEIVED".equals(messageType)) {
                // 处理处方执行记录接收确认
                handlePrescriptionExecutionReceived(jsonNode, session);
            } else if ("PRESCRIPTION_STATUS_UPDATE".equals(messageType)) {
                // 处理处方状态更新
                handlePrescriptionStatusUpdate(jsonNode, session);
            } else {
                // System.out.println("未知的消息类型: " + messageType);
            }
            
        } catch (Exception e) {
            // System.err.println("处理WebSocket消息失败: " + e.getMessage());
            // e.printStackTrace();
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
     * 发送确认回复
     */
    private void sendConfirmationReply(Session session, Long executionId, String result) {
        try {
            Map<String, Object> reply = new HashMap<>();
            reply.put("messageType", "PRESCRIPTION_EXECUTION_CONFIRMATION");
            reply.put("executionId", executionId);
            reply.put("result", result);
            reply.put("timestamp", new Date());
            reply.put("serverInfo", "TMS Backend Server");
            
            String replyMessage = objectMapper.writeValueAsString(reply);
            session.getBasicRemote().sendText(replyMessage);
            
            // System.out.println("=== 发送确认回复 ===");
            // System.out.println("会话ID: " + session.getId());
            // System.out.println("执行记录ID: " + executionId);
            // System.out.println("结果: " + result);
            
        } catch (Exception e) {
            // System.err.println("发送确认回复失败: " + e.getMessage());
            // e.printStackTrace();
        }
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