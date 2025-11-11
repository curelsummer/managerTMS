package cc.mrbird.febs.common.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageHandler;

import java.time.Instant;

/**
 * 处方下发（mqtt-tms）MQTT 出站配置
 */
@Configuration
public class PrescriptionMqttConfig {

    private final PrescriptionMqttProperties prop;

    public PrescriptionMqttConfig(PrescriptionMqttProperties prop) {
        this.prop = prop;
    }

    @Bean("tmsMqttClientFactory")
    public MqttPahoClientFactory tmsMqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setServerURIs(new String[]{prop.getUrl()});
        opts.setUserName(prop.getUsername());
        opts.setPassword(prop.getPassword().toCharArray());
        opts.setCleanSession(false);
        opts.setAutomaticReconnect(true);
        factory.setConnectionOptions(opts);
        return factory;
    }

    @Bean("tmsMqttOutboundChannel")
    public DirectChannel tmsMqttOutboundChannel() {
        DirectChannel channel = new DirectChannel();
        // 手动订阅消息处理器，避免 @ServiceActivator 导致的循环依赖
        channel.subscribe(tmsMqttOutbound(tmsMqttClientFactory()));
        return channel;
    }

    @Bean("tmsMqttOutbound")
    public MessageHandler tmsMqttOutbound(@Qualifier("tmsMqttClientFactory") MqttPahoClientFactory clientFactory) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
            prop.getClientId() + "-pub-" + Instant.now().toEpochMilli(), clientFactory);
        handler.setAsync(true);
        handler.setDefaultRetained(false);
        handler.setAsyncEvents(false);
        handler.setDefaultQos(2);
        handler.setDefaultTopic(prop.getDefaultTopic());
        return handler;
    }
}


