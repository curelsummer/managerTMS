package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.PrescriptionExecution;
import cc.mrbird.febs.system.dao.PrescriptionExecutionMapper;
import cc.mrbird.febs.system.service.PrescriptionExecutionService;
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

import java.util.List;

@Service("prescriptionExecutionService")
public class PrescriptionExecutionServiceImpl extends ServiceImpl<PrescriptionExecutionMapper, PrescriptionExecution> implements PrescriptionExecutionService {

    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public IPage<PrescriptionExecution> findPrescriptionExecutions(IPage<PrescriptionExecution> page, PrescriptionExecution execution, Long userId) {
        QueryWrapper<PrescriptionExecution> wrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (execution != null) {
            if (execution.getPatientId() != null) {
                wrapper.eq("patient_id", execution.getPatientId());
            }
            if (execution.getHospitalId() != null) {
                wrapper.eq("hospital_id", execution.getHospitalId());
            }
            if (execution.getDeviceId() != null) {
                wrapper.eq("device_id", execution.getDeviceId());
            }
            if (execution.getPrescriptionId() != null) {
                wrapper.eq("prescription_id", execution.getPrescriptionId());
            }
            if (execution.getExecutorId() != null) {
                wrapper.eq("executor_id", execution.getExecutorId());
            }
            if (execution.getStatus() != null) {
                wrapper.eq("status", execution.getStatus());
            }
            if (StringUtils.hasText(execution.getProgress())) {
                wrapper.like("progress", execution.getProgress());
            }
        }
        
        // 权限控制逻辑 - 与处方表相同的逻辑
        // 1. 查用户角色
        List<UserRole> userRoles = userRoleService.list(
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

    @Override
    public List<PrescriptionExecution> getByPatientId(Long patientId) {
        return this.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrescriptionExecution>()
                .eq(PrescriptionExecution::getPatientId, patientId)
                .orderByDesc(PrescriptionExecution::getCreatedAt)
        );
    }

    @Override
    public List<PrescriptionExecution> getByPrescriptionId(Long prescriptionId) {
        return this.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrescriptionExecution>()
                .eq(PrescriptionExecution::getPrescriptionId, prescriptionId)
                .orderByDesc(PrescriptionExecution::getCreatedAt)
        );
    }

    @Override
    public List<PrescriptionExecution> getByExecutorId(Long executorId) {
        return this.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrescriptionExecution>()
                .eq(PrescriptionExecution::getExecutorId, executorId)
                .orderByDesc(PrescriptionExecution::getCreatedAt)
        );
    }

    @Override
    public List<PrescriptionExecution> getByStatus(Integer status) {
        return this.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrescriptionExecution>()
                .eq(PrescriptionExecution::getStatus, status)
                .orderByDesc(PrescriptionExecution::getCreatedAt)
        );
    }


} 