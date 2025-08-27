package cc.mrbird.febs.common.config;

import cc.mrbird.febs.system.websocket.PrescriptionExecutionWebSocketServer;
import cc.mrbird.febs.system.websocket.TreatmentRecordWebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import javax.annotation.PostConstruct;

@Configuration
public class WebSocketConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnMissingBean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 初始化处方执行WebSocket服务器的ApplicationContext
     */
    @PostConstruct
    public void initPrescriptionExecutionWebSocketServer() {
        PrescriptionExecutionWebSocketServer.setApplicationContext(applicationContext);
        System.out.println("=== 处方执行WebSocket服务器ApplicationContext初始化完成 ===");
    }
    
    /**
     * 初始化治疗记录WebSocket服务器的ApplicationContext
     */
    @PostConstruct
    public void initTreatmentRecordWebSocketServer() {
        TreatmentRecordWebSocketServer.setApplicationContext(applicationContext);
        System.out.println("=== 治疗记录WebSocket服务器ApplicationContext初始化完成 ===");
    }
} 