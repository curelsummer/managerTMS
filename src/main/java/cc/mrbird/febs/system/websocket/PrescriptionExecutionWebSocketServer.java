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
    
    // ========== 新增：处方认领状态管理（内存缓存，防止并发认领）==========
    private static final Map<Long, PrescriptionClaimStatus> prescriptionClaimMap = new java.util.concurrent.ConcurrentHashMap<>();
    
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
            
            // ========== 新增：处理处方认领 ==========
            if ("PRESCRIPTION_CLAIMED".equals(type)) {
                handlePrescriptionClaim(jsonNode, session);
            }
            // ========== 新增：查询待处理处方 ==========
            else if ("QUERY_PENDING_PRESCRIPTION".equals(type)) {
                handleQueryPendingPrescription(jsonNode, session);
            }
            // ========== 处理上位机确认收到（兼容旧逻辑） ==========
            else if ("execution_received".equals(type) || "PRESCRIPTION_EXECUTION_RECEIVED".equals(type)) {
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
    
    // ========================================================================
    // ==================== 新增：处方广播与认领相关方法 ====================
    // ========================================================================
    
    /**
     * 处方认领状态类（用于并发控制）
     */
    private static class PrescriptionClaimStatus {
        private boolean claimed = false;
        private Integer claimedDeviceNo;
        private Date claimTime;
        private String patientName;
        
        public PrescriptionClaimStatus(String patientName) {
            this.patientName = patientName;
        }
        
        public synchronized boolean claim(Integer deviceNo) {
            if (claimed) {
                return false;
            }
            this.claimed = true;
            this.claimedDeviceNo = deviceNo;
            this.claimTime = new Date();
            return true;
        }
        
        public boolean isClaimed() {
            return claimed;
        }
        
        public Integer getClaimedDeviceNo() {
            return claimedDeviceNo;
        }
    }
    
    /**
     * 处理处方认领请求
     */
    private void handlePrescriptionClaim(JsonNode jsonNode, Session session) {
        try {
            Long executionId = jsonNode.get("executionId").asLong();
            Integer deviceNo = jsonNode.has("deviceNo") ? jsonNode.get("deviceNo").asInt() : null;
            Long deviceId = jsonNode.has("deviceId") ? jsonNode.get("deviceId").asLong() : null;
            String clientInfo = jsonNode.has("clientInfo") ? jsonNode.get("clientInfo").asText() : null;
            
            System.out.println("=== 收到处方认领请求 ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("设备编号: " + deviceNo);
            System.out.println("设备ID: " + deviceId);
            System.out.println("客户端信息: " + clientInfo);
            
            if (prescriptionExecutionService == null) {
                System.err.println("prescriptionExecutionService 未注入");
                sendClaimAck(session, executionId, "ERROR", "服务不可用");
                return;
            }
            
            // 查询处方执行记录
            cc.mrbird.febs.system.domain.PrescriptionExecution execution = 
                prescriptionExecutionService.getById(executionId);
            
            if (execution == null) {
                System.err.println("处方执行记录不存在");
                sendClaimAck(session, executionId, "NOT_FOUND", "处方不存在");
                return;
            }
            
            // 获取或创建认领状态对象
            PrescriptionClaimStatus claimStatus = prescriptionClaimMap.computeIfAbsent(
                executionId, 
                k -> new PrescriptionClaimStatus(getPatientName(execution))
            );
            
            // 尝试认领（线程安全）
            boolean claimSuccess = claimStatus.claim(deviceNo);
            
            if (!claimSuccess) {
                System.out.println("认领失败：处方已被设备 " + claimStatus.getClaimedDeviceNo() + " 认领");
                sendClaimAck(session, executionId, "ALREADY_CLAIMED", 
                    "该患者已在设备" + claimStatus.getClaimedDeviceNo() + "上治疗");
                return;
            }
            
            // 更新数据库：状态改为"已领取/待执行"
            execution.setStatus(1); // 1-已领取/待执行(CLAIMED)
            execution.setProgress("已被设备" + deviceNo + "领取");
            execution.setClaimedDeviceNo(deviceNo);
            execution.setClaimedTime(new Date());
            if (deviceId != null) {
                execution.setDeviceId(deviceId);
            }
            execution.setUpdatedAt(new Date());
            
            boolean updateSuccess = prescriptionExecutionService.updateById(execution);
            
            if (!updateSuccess) {
                System.err.println("数据库更新失败");
                // 回滚内存状态
                prescriptionClaimMap.remove(executionId);
                sendClaimAck(session, executionId, "ERROR", "数据库更新失败");
                return;
            }
            
            System.out.println("=== 处方认领成功 ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("设备编号: " + deviceNo);
            System.out.println("状态已更新为: 1 (已领取/待执行)");
            
            // 发送成功确认给请求设备
            sendClaimAck(session, executionId, "SUCCESS", "领取成功");
            
            // 广播给其他设备，通知该处方已被认领
            broadcastClaimNotify(executionId, deviceNo, getPatientName(execution));
            
        } catch (Exception e) {
            System.err.println("处理处方认领请求失败: " + e.getMessage());
            e.printStackTrace();
            try {
                sendClaimAck(session, null, "ERROR", "处理异常: " + e.getMessage());
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 发送认领确认回复
     */
    private void sendClaimAck(Session session, Long executionId, String result, String message) {
        try {
            Map<String, Object> reply = new HashMap<>();
            reply.put("messageType", "PRESCRIPTION_CLAIM_ACK");
            reply.put("executionId", executionId);
            reply.put("result", result);
            reply.put("message", message);
            reply.put("timestamp", new Date());
            
            String replyMessage = objectMapper.writeValueAsString(reply);
            session.getBasicRemote().sendText(replyMessage);
            
            System.out.println("=== 发送认领确认 ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("结果: " + result);
            System.out.println("消息: " + message);
            
        } catch (Exception e) {
            System.err.println("发送认领确认失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 广播处方已被认领的通知
     */
    private void broadcastClaimNotify(Long executionId, Integer claimedDeviceNo, String patientName) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("messageType", "PRESCRIPTION_CLAIM_NOTIFY");
            notification.put("executionId", executionId);
            notification.put("claimedDeviceNo", claimedDeviceNo);
            notification.put("patientName", patientName);
            notification.put("timestamp", new Date());
            
            String message = objectMapper.writeValueAsString(notification);
            
            System.out.println("=== 广播处方认领通知 ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("领取设备: " + claimedDeviceNo);
            System.out.println("患者姓名: " + patientName);
            
            int successCount = 0;
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                        successCount++;
                    } catch (IOException e) {
                        System.err.println("发送失败到会话: " + session.getId());
                    }
                }
            }
            
            System.out.println("成功广播到 " + successCount + " 个设备");
            
        } catch (Exception e) {
            System.err.println("广播处方认领通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理查询待处理处方请求
     */
    private void handleQueryPendingPrescription(JsonNode jsonNode, Session session) {
        try {
            Integer deviceNo = jsonNode.has("deviceNo") ? jsonNode.get("deviceNo").asInt() : null;
            String searchKey = jsonNode.has("searchKey") ? jsonNode.get("searchKey").asText() : null;
            
            System.out.println("=== 收到查询待处理处方请求 ===");
            System.out.println("设备编号: " + deviceNo);
            System.out.println("搜索关键字: " + searchKey);
            
            if (prescriptionExecutionService == null || notificationService == null) {
                System.err.println("服务未注入");
                sendQueryResult(session, searchKey, new java.util.ArrayList<>(), "ERROR", "服务不可用");
                return;
            }
            
            // 构建查询条件
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cc.mrbird.febs.system.domain.PrescriptionExecution> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            
            // 状态为 0（待领取）
            queryWrapper.eq(cc.mrbird.febs.system.domain.PrescriptionExecution::getStatus, 0);
            
            // 如果有搜索关键字，添加患者相关条件（需要关联患者表，这里简化处理）
            // 实际应用中需要join患者表进行搜索
            
            queryWrapper.orderByDesc(cc.mrbird.febs.system.domain.PrescriptionExecution::getCreatedAt);
            
            java.util.List<cc.mrbird.febs.system.domain.PrescriptionExecution> pendingExecutions = 
                prescriptionExecutionService.list(queryWrapper);
            
            System.out.println("查询到 " + pendingExecutions.size() + " 条待处理处方");
            
            // 发送查询结果
            sendQueryResult(session, searchKey, pendingExecutions, "SUCCESS", null);
            
        } catch (Exception e) {
            System.err.println("处理查询待处理处方请求失败: " + e.getMessage());
            e.printStackTrace();
            try {
                sendQueryResult(session, null, new java.util.ArrayList<>(), "ERROR", "查询异常: " + e.getMessage());
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 发送查询结果
     */
    private void sendQueryResult(Session session, String searchKey, 
                                 java.util.List<cc.mrbird.febs.system.domain.PrescriptionExecution> executions,
                                 String result, String errorMessage) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("messageType", "QUERY_PENDING_PRESCRIPTION_RESULT");
            response.put("searchKey", searchKey);
            response.put("result", result);
            response.put("total", executions.size());
            response.put("timestamp", new Date());
            
            if (errorMessage != null) {
                response.put("message", errorMessage);
            }
            
            // 构建处方列表
            java.util.List<Object> prescriptionList = new java.util.ArrayList<>();
            for (cc.mrbird.febs.system.domain.PrescriptionExecution execution : executions) {
                try {
                    // 使用通知服务构建完整的处方信息
                    cc.mrbird.febs.system.domain.PrescriptionExecutionNotification notification = 
                        notificationService.buildNotification(execution);
                    prescriptionList.add(notification);
                } catch (Exception e) {
                    System.err.println("构建处方通知失败: " + e.getMessage());
                }
            }
            
            response.put("prescriptions", prescriptionList);
            
            String message = objectMapper.writeValueAsString(response);
            session.getBasicRemote().sendText(message);
            
            System.out.println("=== 发送查询结果 ===");
            System.out.println("搜索关键字: " + searchKey);
            System.out.println("结果数量: " + executions.size());
            
        } catch (Exception e) {
            System.err.println("发送查询结果失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取患者姓名（辅助方法）
     */
    private String getPatientName(cc.mrbird.febs.system.domain.PrescriptionExecution execution) {
        if (execution == null) {
            return "未知患者";
        }
        
        try {
            if (applicationContext != null) {
                cc.mrbird.febs.system.service.PatientService patientService = 
                    applicationContext.getBean(cc.mrbird.febs.system.service.PatientService.class);
                cc.mrbird.febs.system.domain.Patient patient = 
                    patientService.getById(execution.getPatientId());
                if (patient != null && patient.getName() != null) {
                    return patient.getName();
                }
            }
        } catch (Exception e) {
            System.err.println("获取患者姓名失败: " + e.getMessage());
        }
        
        return "患者ID:" + execution.getPatientId();
    }
} 