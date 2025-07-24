package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.Device;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface DeviceService extends IService<Device> {
    // 可扩展自定义方法
    IPage<Device> findDevices(IPage<Device> page, Device device, Long userId);
} 