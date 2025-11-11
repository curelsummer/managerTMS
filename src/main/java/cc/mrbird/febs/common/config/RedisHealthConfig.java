package cc.mrbird.febs.common.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.JedisClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Configuration
@ConditionalOnClass({JedisPoolConfig.class, JedisClientConfigurationBuilderCustomizer.class})
public class RedisHealthConfig {

    @Bean
    public JedisPoolConfig jedisPoolConfig(
            @Value("${spring.redis.jedis.pool.max-active:200}") int maxActive,
            @Value("${spring.redis.jedis.pool.max-idle:50}") int maxIdle,
            @Value("${spring.redis.jedis.pool.min-idle:8}") int minIdle,
            @Value("${spring.redis.jedis.pool.max-wait:3s}") Duration maxWait
    ) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        // 兼容不同 commons-pool2 版本的 API
        try {
            GenericObjectPoolConfig.class.getMethod("setMaxWait", Duration.class).invoke(config, maxWait);
        } catch (Exception ignore) {
            config.setMaxWaitMillis(maxWait.toMillis());
        }
        // 健康检查与空闲连接回收
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRunsMillis(60000);     // 60s 检查一次空闲连接
        config.setMinEvictableIdleTimeMillis(600000);       // 10min 最小逐出空闲时长
        config.setNumTestsPerEvictionRun(3);
        return config;
    }

    @Bean
    public JedisClientConfigurationBuilderCustomizer jedisClientCustomizer(
            JedisPoolConfig poolConfig,
            @Value("${spring.redis.timeout:3s}") Duration readTimeout,
            @Value("${spring.redis.connect-timeout:3s}") Duration connectTimeout
    ) {
        return builder -> builder
                .usePooling().poolConfig(poolConfig)
                .and()
                .readTimeout(readTimeout)
                .connectTimeout(connectTimeout);
    }
}


