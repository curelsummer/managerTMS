package cc.mrbird.febs.common.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;

@Configuration
@IntegrationComponentScan
public class DeviceMqttConfig {

    private final DeviceMqttProperties prop;
    private final DeviceMqttInboundMessageHandler deviceMqttInboundMessageHandler;

    public DeviceMqttConfig(DeviceMqttProperties prop,
                            @Lazy DeviceMqttInboundMessageHandler deviceMqttInboundMessageHandler) {
        this.prop = prop;
        this.deviceMqttInboundMessageHandler = deviceMqttInboundMessageHandler;
    }

    @Bean("toiletMqttClientFactory")
    @ConditionalOnProperty(prefix = "mqtt-device", name = "url")
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setServerURIs(new String[]{prop.getUrl()});
        if (prop.getUsername() != null && !prop.getUsername().isEmpty()) {
            mqttConnectOptions.setUserName(prop.getUsername());
        }
        if (prop.getPassword() != null) {
            mqttConnectOptions.setPassword(prop.getPassword().toCharArray());
        }
        // 客户端断线时暂时不清除，直到超时注销
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setAutomaticReconnect(true);
        factory.setConnectionOptions(mqttConnectOptions);
        return factory;
    }

    @Bean("toiletMqttOutboundChannel")
    public DirectChannel toiletMqttOutboundChannel() {
        DirectChannel channel = new DirectChannel();
        // 手动订阅消息处理器，避免 @ServiceActivator 导致的循环依赖
        channel.subscribe(mqttOutbound(mqttClientFactory()));
        return channel;
    }

    @Bean("toiletMqttOutbound")
    @ConditionalOnProperty(prefix = "mqtt-device", name = "url")
    public MessageHandler mqttOutbound(@Qualifier("toiletMqttClientFactory") MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                prop.getClientId() + "-pub-" + Instant.now().toEpochMilli(), mqttClientFactory);
        messageHandler.setAsync(true);
        messageHandler.setDefaultRetained(false);
        messageHandler.setAsyncEvents(false);
        // Exactly Once
        messageHandler.setDefaultQos(2);
        messageHandler.setDefaultTopic(prop.getDefaultTopic());
        return messageHandler;
    }

    @Bean("toiletMqttInboundChannel")
    public DirectChannel toiletMqttInboundChannel() {
        DirectChannel channel = new DirectChannel();
        // 手动订阅消息处理器，避免 @ServiceActivator 导致的循环依赖
        channel.subscribe(deviceMqttInboundMessageHandler);
        return channel;
    }

    @Bean("toiletMqttInbound")
    @ConditionalOnProperty(prefix = "mqtt-device", name = "url")
    public MessageProducerSupport mqttInbound(@Qualifier("toiletMqttClientFactory") MqttPahoClientFactory mqttClientFactory) {
        String topics = prop.getTopics();
        String[] subscribeTopics;
        if (topics == null || topics.trim().isEmpty()) {
            // 默认仅订阅 patient-info-up，避免未配置导致启动失败
            subscribeTopics = new String[]{"+/+/patient-info-up"};
        } else {
            // 兼容逗号分隔或单主题
            subscribeTopics = topics.contains(",") ?
                java.util.Arrays.stream(topics.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new)
                : new String[]{topics.trim()};
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(prop.getClientId() + "-sub-" + Instant.now().toEpochMilli(), mqttClientFactory,
                        subscribeTopics);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        adapter.setOutputChannel(toiletMqttInboundChannel());
        return adapter;
    }
}
