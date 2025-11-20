package cc.mrbird.febs.cos.service.impl;

import cc.mrbird.febs.cos.dao.DeviceMetricsMapper;
import cc.mrbird.febs.cos.entity.DeviceMetrics;
import cc.mrbird.febs.cos.service.IDeviceMetricsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备指标数据 实现层
 *
 * @author FanK
 */
@Slf4j
@Service
public class DeviceMetricsServiceImpl extends ServiceImpl<DeviceMetricsMapper, DeviceMetrics> implements IDeviceMetricsService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchInsert(List<DeviceMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return true;
        }
        try {
            return this.saveBatch(metricsList, 500);
        } catch (Exception e) {
            log.error("批量插入设备指标数据失败", e);
            throw e;
        }
    }
}


