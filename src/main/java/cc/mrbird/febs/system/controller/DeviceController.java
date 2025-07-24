package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.authentication.JWTUtil;
import cc.mrbird.febs.common.utils.FebsUtil;
import cc.mrbird.febs.system.domain.Device;
import cc.mrbird.febs.system.service.DeviceService;
import cc.mrbird.febs.system.websocket.DeviceWebSocketServer;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceController {
    @Autowired
    private DeviceService deviceService;

    @GetMapping
    public List<Device> list() {
        return deviceService.list();
    }

    @GetMapping("/page")
    public IPage<Device> page(@RequestHeader("Authentication") String token,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          Device device) {
    // 先解密token再解析userId
    String realToken = FebsUtil.decryptToken(token);
    Long userId = JWTUtil.getUserId(realToken);
    // 这里应调用自定义的findDevices方法
    return deviceService.findDevices(new Page<>(pageNum, pageSize), device, userId);
}

    @GetMapping("/{id}")
    public Device get(@PathVariable Long id) {
        return deviceService.getById(id);
    }

    @PostMapping
    public boolean add(@RequestBody Device device) {
        return deviceService.save(device);
    }

    @PutMapping
    public boolean update(@RequestBody Device device) {
        boolean updated = deviceService.updateById(device);
        if (updated && device.getStatus() != null) {
            // 推送WebSocket状态变更
            String msg = String.format("{\"deviceId\":%d,\"status\":\"%s\"}", device.getDeviceId(), device.getStatus());
            DeviceWebSocketServer.broadcast(msg);
        }
        return updated;
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return deviceService.removeById(id);
    }
} 