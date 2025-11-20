package cc.mrbird.febs.cos.service;

import cc.mrbird.febs.cos.entity.DeviceMetrics;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 设备指标数据 service层
 *
 * @author FanK
 */
public interface IDeviceMetricsService extends IService<DeviceMetrics> {

    /**
     * 批量插入设备指标数据
     *
     * @param metricsList 设备指标数据列表
     * @return 是否成功
     */
    boolean batchInsert(List<DeviceMetrics> metricsList);
}


