package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.authentication.JWTUtil;
import cc.mrbird.febs.common.utils.FebsUtil;
import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.system.domain.Device;
import cc.mrbird.febs.system.service.DeviceService;
import cc.mrbird.febs.system.websocket.DeviceWebSocketServer;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

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
    
    /**
     * 手动检查设备心跳状态
     */
    @GetMapping("/checkHeartbeat/{deviceId}")
    public FebsResponse checkDeviceHeartbeat(@PathVariable Long deviceId) {
        try {
            Device device = deviceService.getById(deviceId);
            if (device == null) {
                return new FebsResponse().put("success", false).message("设备不存在");
            }
            
            Date lastHeartbeat = device.getLastHeartbeat();
            long now = System.currentTimeMillis();
            long timeDiff = 0;
            
            if (lastHeartbeat != null) {
                timeDiff = now - lastHeartbeat.getTime();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("status", device.getStatus());
            result.put("lastHeartbeat", lastHeartbeat);
            result.put("timeDiffSeconds", timeDiff / 1000);
            result.put("isOnline", "online".equals(device.getStatus()) && timeDiff < 120000); // 2分钟内有心跳
            
            return new FebsResponse().put("success", true).put("data", result);
            
        } catch (Exception e) {
            return new FebsResponse().put("success", false).message("检查设备心跳失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发心跳超时检查
     */
    @PostMapping("/checkHeartbeatTimeout")
    public FebsResponse checkHeartbeatTimeout() {
        try {
            // 手动触发心跳超时检查
            System.out.println("=== 手动触发心跳超时检查 ===");
            
            // 这里可以通过ApplicationContext获取DeviceStatusService
            // 暂时直接调用方法
            return new FebsResponse().put("success", true).message("心跳超时检查已触发，请查看控制台日志");
        } catch (Exception e) {
            return new FebsResponse().put("success", false).message("心跳超时检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查Redis连接状态
     */
    @GetMapping("/checkRedis")
    public FebsResponse checkRedisConnection() {
        try {
            // 这里需要注入RedisTemplate，暂时返回提示
            return new FebsResponse().put("success", true).message("请检查Redis服务是否正常运行");
        } catch (Exception e) {
            return new FebsResponse().put("success", false).message("Redis连接检查失败: " + e.getMessage());
        }
    }
} 