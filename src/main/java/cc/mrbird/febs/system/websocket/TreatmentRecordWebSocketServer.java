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
import cc.mrbird.febs.common.utils.SpringContextUtil;

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
        System.out.println("=== 治疗记录WebSocket连接建立 ===");
        System.out.println("端点: /ws/treatment-record");
        System.out.println("会话ID: " + session.getId());
        System.out.println("当前治疗记录连接数: " + sessions.size());
        System.out.println("说明：此端点用于接收治疗记录信息");
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("=== 治疗记录WebSocket连接断开 ===");
        System.out.println("会话ID: " + session.getId());
        System.out.println("当前总连接数: " + sessions.size());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("=== 收到WebSocket客户端消息 ===");
        System.out.println("会话ID: " + session.getId());
        System.out.println("消息内容: " + message);
        
        try {
            // 解析客户端消息
            JsonNode jsonNode = objectMapper.readTree(message);
            
            String messageType = jsonNode.get("messageType").asText();
            
            if ("TREATMENT_RECORD_CREATED".equals(messageType)) {
                // 处理治疗记录创建
                handleTreatmentRecordCreated(jsonNode, session);
            } else {
                System.out.println("未知的消息类型: " + messageType);
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
     * 处理治疗记录创建
     */
    private void handleTreatmentRecordCreated(JsonNode jsonNode, Session session) {
        try {
            System.out.println("=== 处理治疗记录创建 ===");
            
            // 获取治疗记录服务
            TreatmentRecordService treatmentRecordService = getTreatmentRecordService();
            if (treatmentRecordService == null) {
                throw new RuntimeException("无法获取治疗记录服务");
            }
            
            // 保存治疗记录到数据库
            boolean success = treatmentRecordService.saveTreatmentRecord(jsonNode);
            
            if (success) {
                System.out.println("治疗记录保存成功");
                // 发送成功确认
                sendSuccessReply(session, "治疗记录保存成功");
            } else {
                System.out.println("治疗记录保存失败");
                // 发送失败确认
                sendErrorReply(session, "治疗记录保存失败");
            }
            
        } catch (Exception e) {
            System.err.println("处理治疗记录创建失败: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误回复
            try {
                sendErrorReply(session, "处理治疗记录创建失败: " + e.getMessage());
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
            System.err.println("获取治疗记录服务失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
