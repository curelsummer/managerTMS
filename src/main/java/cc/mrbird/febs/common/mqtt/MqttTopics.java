package cc.mrbird.febs.common.mqtt;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT 主题构造与解析工具
 * 规范：设备类型/设备编号/消息类型
 */
public final class MqttTopics {

    private MqttTopics() {}

    public static String buildDownTopic(String deviceType, String deviceId, String msgType) {
        return StrUtil.join("/", deviceType, deviceId, msgType);
    }

    public static TopicParts parse(String topic) {
        if (StrUtil.isBlank(topic)) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length < 3) {
            return null;
        }
        return new TopicParts(parts[0], parts[1], parts[2]);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicParts {
        private String deviceType;
        private String deviceId;
        private String msgType;
    }
}


