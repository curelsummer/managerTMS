package cc.mrbird.febs.cos.service;

import cc.mrbird.febs.cos.entity.DeviceAlertInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.LinkedHashMap;

/**
 * 设备报警配置 service层
 *
 * @author FanK
 */
public interface IDeviceAlertInfoService extends IService<DeviceAlertInfo> {

    /**
     * 分页获取设备报警配置信息
     *
     * @param page            分页对象
     * @param deviceAlertInfo 设备报警配置信息
     * @return 结果
     */
    IPage<LinkedHashMap<String, Object>> selectDeviceAlertPage(Page<DeviceAlertInfo> page, DeviceAlertInfo deviceAlertInfo);
}
