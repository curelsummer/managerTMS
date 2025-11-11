package cc.mrbird.febs.common.mqtt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mqtt-tms")
public class PrescriptionMqttProperties {

    private String url;
    private String username;
    private String password;
    private String clientId;
    private String topics;
    private String defaultTopic;

}




