package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.dao.PrescriptionMapper;
import cc.mrbird.febs.system.domain.Prescription;
import cc.mrbird.febs.system.service.PrescriptionService;
import cc.mrbird.febs.system.domain.PatientPrescription;
import cc.mrbird.febs.system.service.PatientPrescriptionService;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.domain.UserRole;
import cc.mrbird.febs.system.service.UserRoleService;
import cc.mrbird.febs.system.service.UserService;
import cc.mrbird.febs.system.dao.DepartmentMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service("prescriptionService")
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements PrescriptionService {

    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired(required = false)
    private PatientPrescriptionService patientPrescriptionService;

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

    @Override
    public Map<String, Object> getByPatient(String patientId) {
        if (!StringUtils.hasText(patientId)) {
            return null;
        }
        Long pid;
        try {
            pid = Long.valueOf(patientId);
        } catch (NumberFormatException e) {
            return null;
        }
        Prescription p = null;
        // 1) 优先从患者-处方关联表按最近使用选择（last_used_at 最大，NULL 视为最早）
        if (patientPrescriptionService != null) {
            PatientPrescription pp = patientPrescriptionService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PatientPrescription>()
                    .eq(PatientPrescription::getPatientId, pid)
                    .last("order by (last_used_at is null), last_used_at desc limit 1")
            );
            if (pp != null && pp.getPrescriptionId() != null) {
                p = this.baseMapper.selectById(pp.getPrescriptionId());
            }
        }
        // 2) 回退：仍按处方表中 patient_id 最新一条
        if (p == null) {
            LambdaQueryWrapper<Prescription> qw = new LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getPatientId, pid)
                .orderByDesc(Prescription::getUpdatedAt)
                .last("limit 1");
            p = this.baseMapper.selectOne(qw);
        }
        if (p == null) {
            return null;
        }
        Map<String, Object> dto = new HashMap<>();
        
        // 基础信息
        dto.put("prescriptionId", p.getId());
        dto.put("patientId", p.getPatientId());
        dto.put("hospitalId", p.getHospitalId());
        dto.put("doctorId", p.getDoctorId());
        dto.put("presType", p.getPresType());  // 处方类型：1=标准TMS, 2=TBS
        
        // 部位信息
        dto.put("presPartName", p.getPresPartName());
        dto.put("standardPresName", p.getStandardPresName());
        
        // 治疗参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("presStrength", p.getPresStrength());
        parameters.put("presFreq", p.getPresFreq());
        parameters.put("lastTime", p.getLastTime());
        parameters.put("pauseTime", p.getPauseTime());
        parameters.put("repeatCount", p.getRepeatCount());
        parameters.put("totalCount", p.getTotalCount());
        parameters.put("totalTime", p.getTotalTime());
        parameters.put("periods", p.getPeriods());  // 周期数（所有处方都需要）
        
        // TBS专用参数（如果是TBS处方）
        if (p.getTbsType() != null) {
            parameters.put("tbsType", p.getTbsType());
            parameters.put("innerCount", p.getInnerCount());
            parameters.put("interFreq", p.getInterFreq());
            parameters.put("interCount", p.getInterCount());
        }
        
        dto.put("parameters", parameters);
        return dto;
    }
    
    @Override
    public boolean incrementUsageCount(Long prescriptionId) {
        if (prescriptionId == null) {
            return false;
        }
        try {
            // 使用数据库原子操作增加使用次数
            Prescription prescription = this.baseMapper.selectById(prescriptionId);
            if (prescription == null) {
                return false;
            }
            
            // 构建更新对象
            Prescription update = new Prescription();
            update.setId(prescriptionId);
            update.setUsageCount(prescription.getUsageCount() == null ? 1 : prescription.getUsageCount() + 1);
            
            return this.updateById(update);
        } catch (Exception e) {
            // 记录异常但不影响主流程
            return false;
        }
    }
} 