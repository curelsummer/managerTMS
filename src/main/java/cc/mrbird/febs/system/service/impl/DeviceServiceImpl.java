package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.Device;
import cc.mrbird.febs.system.dao.DeviceMapper;
import cc.mrbird.febs.system.service.DeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.domain.UserRole;
import cc.mrbird.febs.system.service.UserRoleService;
import cc.mrbird.febs.system.service.UserService;
import cc.mrbird.febs.system.dao.DepartmentMapper;
import org.springframework.beans.factory.annotation.Autowired;

@Service("deviceService")
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
    // 可扩展自定义方法

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UserService userService;
    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public IPage<Device> findDevices(IPage<Device> page, Device device, Long userId) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        if (device.getSn() != null) {
            wrapper.like("sn", device.getSn());
        }
        // 1. 查用户角色
        java.util.List<UserRole> userRoles = userRoleService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        boolean isSuperAdmin = userRoles.stream().anyMatch(r -> r.getRoleId() == 1L);
        boolean isAdminOrDoctor = userRoles.stream().anyMatch(r -> r.getRoleId() == 2L || r.getRoleId() == 3L);
        if (isSuperAdmin) {
            // 超级管理员不过滤
        } else if (isAdminOrDoctor) {
            User user = userService.getById(userId);
            if (user != null && user.getDeptId() != null) {
                Long hospitalId = departmentMapper.findHospitalIdByDepartmentId(user.getDeptId());
                if (hospitalId != null) {
                    wrapper.eq("hospital_id", hospitalId);
                } else {
                    // 没查到医院，返回空
                    wrapper.eq("hospital_id", -1);
                }
            } else {
                // 没查到科室，返回空
                wrapper.eq("hospital_id", -1);
            }
        } else {
            // 其他角色自定义处理
            wrapper.eq("hospital_id", -1);
        }
        return this.baseMapper.selectPage(page, wrapper);
    }
} 