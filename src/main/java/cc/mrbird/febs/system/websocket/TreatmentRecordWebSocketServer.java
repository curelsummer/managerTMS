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
import org.springframework.context.ApplicationContext;
import cc.mrbird.febs.system.service.TreatmentRecordService;
import cc.mrbird.febs.system.service.MepRecordService;
import cc.mrbird.febs.system.service.RecordSyncService;
import cc.mrbird.febs.system.domain.response.MepRecordCreatedResponse;
import cc.mrbird.febs.system.domain.response.TreatmentRecordCreatedResponse;
import cc.mrbird.febs.system.domain.response.SyncResponse;
import cc.mrbird.febs.common.utils.SpringContextUtil;

/**
 * 治疗记录和MEP记录WebSocket服务器
 * 处理三种消息类型：
 * 1. MEP_RECORD_CREATED - MEP记录上传
 * 2. TREATMENT_RECORD_CREATED - 治疗记录上传
 * 3. SYNC_REQUEST - 同步请求
 */
@Component
@ServerEndpoint("/ws/treatment-record")
public class TreatmentRecordWebSocketServer {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 用于获取Spring服务
    private static ApplicationContext applicationContext;
    
    public static void setApplicationContext(ApplicationContext context) {
        TreatmentRecordWebSocketServer.applicationContext = context;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        // System.out.println("=== 治疗记录WebSocket连接建立 ===");
        // System.out.println("端点: /ws/treatment-record");
        // System.out.println("会话ID: " + session.getId());
        // System.out.println("当前治疗记录连接数: " + sessions.size());
        // System.out.println("说明：此端点用于接收治疗记录信息");
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        // System.out.println("=== 治疗记录WebSocket连接断开 ===");
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
            
            String messageType = jsonNode.get("messageType").asText();
            System.out.println("消息类型: " + messageType);
            
            // 根据消息类型分发处理
            switch (messageType) {
                case "MEP_RECORD_CREATED":
                    handleMepRecordCreated(jsonNode, session);
                    break;
                    
                case "TREATMENT_RECORD_CREATED":
                    handleTreatmentRecordCreated(jsonNode, session);
                    break;
                    
                case "SYNC_REQUEST":
                    handleSyncRequest(jsonNode, session);
                    break;
                    
                default:
                    System.out.println("未知的消息类型: " + messageType);
                    sendErrorReply(session, "未知的消息类型: " + messageType);
            }
            
        } catch (Exception e) {
            System.err.println("处理WebSocket消息失败: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误回复
            try {
                sendErrorReply(session, "消息处理失败: " + e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * 处理MEP记录创建
     */
    private void handleMepRecordCreated(JsonNode jsonNode, Session session) {
        Long localMepRecordId = null;
        try {
            System.out.println("=== 处理MEP记录创建 ===");
            
            localMepRecordId = jsonNode.get("localMepRecordId").asLong();
            
            // 获取MEP记录服务
            MepRecordService mepRecordService = getMepRecordService();
            if (mepRecordService == null) {
                throw new RuntimeException("无法获取MEP记录服务");
            }
            
            // 保存MEP记录到数据库
            String serverRecordId = mepRecordService.saveMepRecord(jsonNode);
            
            System.out.println("MEP记录保存成功，serverRecordId: " + serverRecordId);
            
            // 发送成功响应
            MepRecordCreatedResponse response = MepRecordCreatedResponse.success(serverRecordId, localMepRecordId);
            sendResponse(session, response);
            
        } catch (Exception e) {
            System.err.println("处理MEP记录创建失败: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误响应
            try {
                MepRecordCreatedResponse response = MepRecordCreatedResponse.error(e.getMessage(), localMepRecordId);
                sendResponse(session, response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * 处理治疗记录创建
     */
    private void handleTreatmentRecordCreated(JsonNode jsonNode, Session session) {
        Long localMedicalRecordId = null;
        try {
            System.out.println("=== 处理治疗记录创建 ===");
            
            localMedicalRecordId = jsonNode.get("localMedicalRecordId").asLong();
            
            // 获取治疗记录服务
            TreatmentRecordService treatmentRecordService = getTreatmentRecordService();
            if (treatmentRecordService == null) {
                throw new RuntimeException("无法获取治疗记录服务");
            }
            
            // 保存治疗记录到数据库，返回serverRecordId
            // 注意：需要修改 saveTreatmentRecord 方法返回 String 而不是 boolean
            boolean success = treatmentRecordService.saveTreatmentRecord(jsonNode);
            
            if (success) {
                // 从jsonNode生成serverRecordId（临时方案，后续可以改为服务返回）
                Integer deviceNo = jsonNode.get("deviceNo").asInt();
                String serverRecordId = cc.mrbird.febs.system.utils.ServerRecordIdGenerator
                    .generateTreatmentRecordId(deviceNo, localMedicalRecordId);
                
                System.out.println("治疗记录保存成功，serverRecordId: " + serverRecordId);
                
                // 发送成功响应
                TreatmentRecordCreatedResponse response = TreatmentRecordCreatedResponse.success(serverRecordId, localMedicalRecordId);
                sendResponse(session, response);
            } else {
                throw new RuntimeException("治疗记录保存失败");
            }
            
        } catch (Exception e) {
            System.err.println("处理治疗记录创建失败: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误响应
            try {
                TreatmentRecordCreatedResponse response = TreatmentRecordCreatedResponse.error(e.getMessage(), localMedicalRecordId);
                sendResponse(session, response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * 处理同步请求
     */
    private void handleSyncRequest(JsonNode jsonNode, Session session) {
        try {
            System.out.println("=== 处理同步请求 ===");
            
            String patientIdentifier = jsonNode.get("patientIdentifier").asText();
            String syncType = jsonNode.has("syncType") ? jsonNode.get("syncType").asText() : "ALL";
            Integer deviceNo = jsonNode.get("deviceNo").asInt();
            
            System.out.println("患者标识: " + patientIdentifier);
            System.out.println("同步类型: " + syncType);
            System.out.println("请求设备号: " + deviceNo);
            
            // 获取同步服务
            RecordSyncService syncService = getRecordSyncService();
            if (syncService == null) {
                throw new RuntimeException("无法获取同步服务");
            }
            
            // 执行同步查询（服务器返回所有数据，设备端自己过滤）
            SyncResponse response = syncService.syncPatientRecords(patientIdentifier, syncType, deviceNo);
            
            System.out.println("同步查询完成，返回数据");
            
            // 发送同步响应
            sendResponse(session, response);
            
        } catch (Exception e) {
            System.err.println("处理同步请求失败: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误响应
            try {
                SyncResponse response = SyncResponse.error(e.getMessage());
                sendResponse(session, response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * 获取治疗记录服务
     */
    private TreatmentRecordService getTreatmentRecordService() {
        try {
            if (applicationContext != null) {
                return applicationContext.getBean(TreatmentRecordService.class);
            } else {
                // 备用方案：使用SpringContextUtil
                return SpringContextUtil.getBean(TreatmentRecordService.class);
            }
        } catch (Exception e) {
            // System.err.println("获取治疗记录服务失败: " + e.getMessage());
            // e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取MEP记录服务
     */
    private MepRecordService getMepRecordService() {
        try {
            if (applicationContext != null) {
                return applicationContext.getBean(MepRecordService.class);
            } else {
                return SpringContextUtil.getBean(MepRecordService.class);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取同步服务
     */
    private RecordSyncService getRecordSyncService() {
        try {
            if (applicationContext != null) {
                return applicationContext.getBean(RecordSyncService.class);
            } else {
                return SpringContextUtil.getBean(RecordSyncService.class);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 发送响应对象
     */
    private void sendResponse(Session session, Object response) throws Exception {
        String message = objectMapper.writeValueAsString(response);
        session.getBasicRemote().sendText(message);
        System.out.println("响应已发送");
    }
    
    /**
     * 发送成功回复
     */
    private void sendSuccessReply(Session session, String message) throws IOException {
        String reply = objectMapper.writeValueAsString(new ReplyMessage("SUCCESS", message, new Date()));
        session.getBasicRemote().sendText(reply);
    }
    
    /**
     * 发送错误回复
     */
    private void sendErrorReply(Session session, String message) throws IOException {
        String reply = objectMapper.writeValueAsString(new ReplyMessage("ERROR", message, new Date()));
        session.getBasicRemote().sendText(reply);
    }
    
    /**
     * 回复消息内部类
     */
    public static class ReplyMessage {
        private String status;
        private String message;
        private Date timestamp;
        
        public ReplyMessage(String status, String message, Date timestamp) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
}
