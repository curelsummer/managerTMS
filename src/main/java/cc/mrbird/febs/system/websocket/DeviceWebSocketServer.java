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

@Component
@ServerEndpoint("/ws/device")
public class DeviceWebSocketServer {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private static DeviceStatusService deviceStatusService;

    // 通过Spring注入DeviceStatusService
    @org.springframework.beans.factory.annotation.Autowired
    public void setDeviceStatusService(DeviceStatusService service) {
        DeviceWebSocketServer.deviceStatusService = service;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("前端WebSocket连接：" + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("前端WebSocket断开：" + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 输出收到的原始消息
        System.out.println("收到WebSocket消息: " + message);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(message);
            Long deviceId = node.get("deviceId").asLong();
            String status = node.get("status").asText();
            // 输出解析后的内容
            System.out.println("解析后 deviceId: " + deviceId + ", status: " + status);
            if ("online".equals(status)) {
                deviceStatusService.deviceOnline(deviceId);
            } else if ("heartbeat".equals(status)) {
                deviceStatusService.deviceHeartbeat(deviceId);
            } else if ("offline".equals(status)) {
                deviceStatusService.deviceOffline(deviceId);
            }
            // 其它业务处理
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("WebSocket发生错误：" + session.getId());
        error.printStackTrace();
    }

    // 推送消息给所有前端
    public static void broadcast(String message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
} 