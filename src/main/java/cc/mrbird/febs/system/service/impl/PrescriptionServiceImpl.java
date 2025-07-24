package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.Prescription;
import cc.mrbird.febs.system.dao.PrescriptionMapper;
import cc.mrbird.febs.system.service.PrescriptionService;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.domain.UserRole;
import cc.mrbird.febs.system.service.UserRoleService;
import cc.mrbird.febs.system.service.UserService;
import cc.mrbird.febs.system.dao.DepartmentMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("prescriptionService")
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements PrescriptionService {

    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public IPage<Prescription> findPrescriptions(IPage<Prescription> page, Prescription prescription, Long userId) {
        QueryWrapper<Prescription> wrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (prescription != null) {
            if (prescription.getPatientId() != null) {
                wrapper.eq("patient_id", prescription.getPatientId());
            }
            if (prescription.getDoctorId() != null) {
                wrapper.eq("doctor_id", prescription.getDoctorId());
            }
            if (prescription.getPresType() != null) {
                wrapper.eq("pres_type", prescription.getPresType());
            }
            if (prescription.getStatus() != null) {
                wrapper.eq("status", prescription.getStatus());
            }
            if (StringUtils.hasText(prescription.getPresPartName())) {
                wrapper.like("pres_part_name", prescription.getPresPartName());
            }
            if (StringUtils.hasText(prescription.getStandardPresName())) {
                wrapper.like("standard_pres_name", prescription.getStandardPresName());
            }
        }
        
        // 权限控制逻辑
        // 1. 查用户角色
        java.util.List<UserRole> userRoles = userRoleService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
        );
        
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
        
        // 按创建时间倒序排列
        wrapper.orderByDesc("created_at");
        
        return this.baseMapper.selectPage(page, wrapper);
    }
} 