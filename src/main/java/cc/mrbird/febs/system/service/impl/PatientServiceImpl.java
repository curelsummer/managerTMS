package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.Patient;
import cc.mrbird.febs.system.dao.PatientMapper;
import cc.mrbird.febs.system.service.PatientService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import cc.mrbird.febs.system.dao.DepartmentMapper;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.domain.UserRole;
import cc.mrbird.febs.system.service.UserRoleService;
import cc.mrbird.febs.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@Service("patientService")
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UserService userService;
    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public IPage<Patient> findPatients(IPage<Patient> page, Patient patient, Long userId) {
        QueryWrapper<Patient> wrapper = new QueryWrapper<>();
        if (patient.getName() != null) {
            wrapper.like("name", patient.getName());
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

    @Override
    public IPage<Patient> findPatients(IPage<Patient> page, Patient patient) {
        // 兼容旧接口，默认查全部
        return findPatients(page, patient, null);
    }
} 