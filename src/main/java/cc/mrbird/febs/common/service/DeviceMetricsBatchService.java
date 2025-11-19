package cc.mrbird.febs.common.service;

import cc.mrbird.febs.cos.entity.DeviceMetrics;
import cc.mrbird.febs.cos.service.IDeviceMetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备指标数据批量处理服务
 *
 * @author FanK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceMetricsBatchService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IDeviceMetricsService deviceMetricsService;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_KEY = "mqtt:queue:device-metrics";
    private static final String ERROR_QUEUE_KEY = "mqtt:queue:device-metrics:error";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${device-metrics.batch.size:500}")
    private int batchSize;

    @Value("${device-metrics.batch.interval:1000}")
    private long batchInterval;

    /**
     * 定时批量处理设备指标数据
     * 每1秒执行一次，批量拉取并写入数据库
     */
    @Scheduled(fixedDelayString = "${device-metrics.batch.interval:1000}")
    public void processBatch() {
        try {
            List<DeviceMetrics> metricsList = new ArrayList<>();
            int processedCount = 0;

            // 批量从Redis队列拉取数据
            for (int i = 0; i < batchSize; i++) {
                String messageJson = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY, 1, TimeUnit.SECONDS);
                if (messageJson == null || messageJson.isEmpty()) {
                    break;
                }

                try {
                    DeviceMetrics metrics = parseMessage(messageJson);
                    if (metrics != null) {
                        metricsList.add(metrics);
                        processedCount++;
                    } else {
                        // 解析失败，记录到错误队列
                        stringRedisTemplate.opsForList().leftPush(ERROR_QUEUE_KEY, messageJson);
                    }
                } catch (Exception e) {
                    log.warn("解析设备指标数据失败: {}", messageJson, e);
                    // 解析失败，记录到错误队列
                    stringRedisTemplate.opsForList().leftPush(ERROR_QUEUE_KEY, messageJson);
                }
            }

            // 批量写入数据库
            if (!metricsList.isEmpty()) {
                try {
                    deviceMetricsService.batchInsert(metricsList);
                    log.debug("批量插入设备指标数据成功: 数量={}", metricsList.size());
                } catch (Exception e) {
                    log.error("批量插入设备指标数据失败: 数量={}", metricsList.size(), e);
                    // 写入失败，重新入队
                    for (DeviceMetrics metrics : metricsList) {
                        try {
                            String messageJson = objectMapper.writeValueAsString(metrics);
                            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, messageJson);
                        } catch (Exception ex) {
                            log.error("重新入队失败", ex);
                        }
                    }
                }
            }

            // 记录处理统计
            if (processedCount > 0) {
                log.debug("设备指标数据处理完成: 处理数量={}", processedCount);
            }

        } catch (Exception e) {
            log.error("批量处理设备指标数据异常", e);
        }
    }

    /**
     * 解析消息JSON为DeviceMetrics对象
     */
    private DeviceMetrics parseMessage(String messageJson) {
        try {
            JsonNode root = objectMapper.readTree(messageJson);

            DeviceMetrics metrics = new DeviceMetrics();
            metrics.setDeviceType(getText(root, "deviceType"));
            metrics.setDeviceId(getText(root, "deviceId"));
            metrics.setMsgId(getText(root, "msgId"));

            // 解析温度数据
            String coilTempStr = getText(root, "coilTemperature");
            if (coilTempStr != null && !coilTempStr.isEmpty()) {
                try {
                    metrics.setCoilTemperature(new BigDecimal(coilTempStr));
                } catch (NumberFormatException e) {
                    log.warn("线圈温度格式错误: {}", coilTempStr);
                }
            }

            String machineTempStr = getText(root, "machineTemperature");
            if (machineTempStr != null && !machineTempStr.isEmpty()) {
                try {
                    metrics.setMachineTemperature(new BigDecimal(machineTempStr));
                } catch (NumberFormatException e) {
                    log.warn("机温格式错误: {}", machineTempStr);
                }
            }

            // 解析流速数据
            String pumpFlowRateStr = getText(root, "pumpFlowRate");
            if (pumpFlowRateStr != null && !pumpFlowRateStr.isEmpty()) {
                try {
                    metrics.setPumpFlowRate(new BigDecimal(pumpFlowRateStr));
                } catch (NumberFormatException e) {
                    log.warn("水泵流速格式错误: {}", pumpFlowRateStr);
                }
            }

            // 解析时间
            String createTimeStr = getText(root, "createTime");
            if (createTimeStr != null && !createTimeStr.isEmpty()) {
                try {
                    metrics.setCreateTime(LocalDateTime.parse(createTimeStr, DATETIME_FORMATTER));
                } catch (Exception e) {
                    log.warn("创建时间格式错误: {}", createTimeStr);
                    metrics.setCreateTime(LocalDateTime.now());
                }
            } else {
                metrics.setCreateTime(LocalDateTime.now());
            }

            String serverTimeStr = getText(root, "serverTime");
            if (serverTimeStr != null && !serverTimeStr.isEmpty()) {
                try {
                    metrics.setServerTime(LocalDateTime.parse(serverTimeStr, DATETIME_FORMATTER));
                } catch (Exception e) {
                    log.warn("服务器时间格式错误: {}", serverTimeStr);
                    metrics.setServerTime(LocalDateTime.now());
                }
            } else {
                metrics.setServerTime(LocalDateTime.now());
            }

            return metrics;
        } catch (Exception e) {
            log.error("解析消息失败: {}", messageJson, e);
            return null;
        }
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode val = node.get(field);
        if (val == null) return null;
        if (val.isNull()) return null;
        if (val.isTextual()) return val.asText();
        return val.toString();
    }
}

