package cc.mrbird.febs.common.mqtt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 基于 Redis 的幂等去重：mqtt:msg:{msgId}
 */
@Service
@RequiredArgsConstructor
public class MsgDedupService {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean checkAndMark(String msgId, Duration ttl) {
        if (msgId == null || msgId.isEmpty()) {
            return true; // 无 msgId，放行
        }
        String key = "mqtt:msg:" + msgId;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }
}


