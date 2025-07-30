package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.common.domain.QueryRequest;
import cc.mrbird.febs.common.utils.FebsUtil;
import cc.mrbird.febs.common.authentication.JWTUtil;
import cc.mrbird.febs.system.domain.PrescriptionExecution;
import cc.mrbird.febs.system.service.PrescriptionExecutionService;
import cc.mrbird.febs.system.service.PrescriptionExecutionNotificationService;
import cc.mrbird.febs.system.service.UserService;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.dao.DepartmentMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/prescription-execution")
public class PrescriptionExecutionController {

    @Autowired
    private PrescriptionExecutionService prescriptionExecutionService;
    
    @Autowired
    private PrescriptionExecutionNotificationService notificationService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 获取所有处方执行记录
     */
    @GetMapping
    public List<PrescriptionExecution> list() {
        return prescriptionExecutionService.list();
    }

    /**
     * 分页查询处方执行记录
     */
    @GetMapping("/page")
    public IPage<PrescriptionExecution> page(@RequestHeader("Authentication") String token,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            PrescriptionExecution execution) {
        String realToken = FebsUtil.decryptToken(token);
        Long userId = JWTUtil.getUserId(realToken);
        return prescriptionExecutionService.findPrescriptionExecutions(new Page<>(pageNum, pageSize), execution, userId);
    }

    /**
     * 根据ID获取处方执行记录详情
     */
    @GetMapping("/{id}")
    public PrescriptionExecution get(@PathVariable Long id) {
        return prescriptionExecutionService.getById(id);
    }

    /**
     * 新增处方执行记录
     */
    @PostMapping
    public FebsResponse add(@RequestHeader("Authentication") String token, @RequestBody PrescriptionExecution execution) {
        try {
            // 设置创建时间和创建人
            String realToken = FebsUtil.decryptToken(token);
            Long userId = JWTUtil.getUserId(realToken);
            
            execution.setCreatedBy(userId);
            execution.setCreatedAt(new Date());
            execution.setUpdatedBy(userId);
            execution.setUpdatedAt(new Date());
            
            // 设置执行人ID为当前登录用户
            execution.setExecutorId(userId);
            
            // 设置医院ID - 从当前用户的科室信息获取
            if (execution.getHospitalId() == null) {
                User user = userService.getById(userId);
                if (user != null && user.getDeptId() != null) {
                    Long hospitalId = departmentMapper.findHospitalIdByDepartmentId(user.getDeptId());
                    if (hospitalId != null) {
                        execution.setHospitalId(hospitalId);
                    }
                }
            }
            
            // 设置默认值
            if (execution.getStatus() == null) {
                execution.setStatus(0); // 默认草稿状态
            }
            
            boolean success = prescriptionExecutionService.save(execution);
            
            if (success) {
                System.out.println("=== 处方执行记录保存成功，准备发送WebSocket通知 ===");
                System.out.println("执行记录ID: " + execution.getId());
                System.out.println("患者ID: " + execution.getPatientId());
                System.out.println("处方ID: " + execution.getPrescriptionId());
                System.out.println("设备ID: " + execution.getDeviceId());
                System.out.println("执行人ID: " + execution.getExecutorId());
                System.out.println("医院ID: " + execution.getHospitalId());
                
                // 发送WebSocket通知
                notificationService.notifyPrescriptionExecutionCreated(execution);
                
                System.out.println("=== WebSocket通知发送完成 ===");
                return new FebsResponse().put("success", true).message("处方执行记录创建成功");
            } else {
                System.out.println("=== 处方执行记录保存失败，不发送WebSocket通知 ===");
                return new FebsResponse().put("success", false).message("处方执行记录创建失败");
            }
        } catch (Exception e) {
            return new FebsResponse().put("success", false).message("系统错误：" + e.getMessage());
        }
    }

    /**
     * 更新处方执行记录
     */
    @PutMapping
    public FebsResponse update(@RequestHeader("Authentication") String token, @RequestBody PrescriptionExecution execution) {
        try {
            // 设置更新时间和更新人
            String realToken = FebsUtil.decryptToken(token);
            Long userId = JWTUtil.getUserId(realToken);
            
            execution.setUpdatedBy(userId);
            execution.setUpdatedAt(new Date());
            
            // 设置执行人ID为当前登录用户（如果为空）
            if (execution.getExecutorId() == null) {
                execution.setExecutorId(userId);
            }
            
            // 设置医院ID（如果为空）- 从当前用户的科室信息获取
            if (execution.getHospitalId() == null) {
                User user = userService.getById(userId);
                if (user != null && user.getDeptId() != null) {
                    Long hospitalId = departmentMapper.findHospitalIdByDepartmentId(user.getDeptId());
                    if (hospitalId != null) {
                        execution.setHospitalId(hospitalId);
                    }
                }
            }
            
            boolean success = prescriptionExecutionService.updateById(execution);
            
            if (success) {
                return new FebsResponse().put("success", true).message("处方执行记录更新成功");
            } else {
                return new FebsResponse().put("success", false).message("处方执行记录更新失败");
            }
        } catch (Exception e) {
            return new FebsResponse().put("success", false).message("系统错误：" + e.getMessage());
        }
    }

    /**
     * 删除处方执行记录
     */
    @DeleteMapping("/{id}")
    public FebsResponse delete(@PathVariable Long id) {
        try {
            boolean success = prescriptionExecutionService.removeById(id);
            
            if (success) {
                return new FebsResponse().put("success", true).message("处方执行记录删除成功");
            } else {
                return new FebsResponse().put("success", false).message("处方执行记录删除失败");
            }
        } catch (Exception e) {
            return new FebsResponse().put("success", false).message("系统错误：" + e.getMessage());
        }
    }

    /**
     * 批量删除处方执行记录
     */
    @DeleteMapping("/batch")
    public FebsResponse batchDelete(@RequestBody List<Long> ids) {
        try {
            boolean success = prescriptionExecutionService.removeByIds(ids);
            
            if (success) {
                return new FebsResponse().put("success", true).message("批量删除成功");
            } else {
                return new FebsResponse().put("success", false).message("批量删除失败");
            }
        } catch (Exception e) {
            return new FebsResponse().put("success", false).message("系统错误：" + e.getMessage());
        }
    }

    /**
     * 根据患者ID查询处方执行记录
     */
    @GetMapping("/patient/{patientId}")
    public List<PrescriptionExecution> getByPatientId(@PathVariable Long patientId) {
        return prescriptionExecutionService.getByPatientId(patientId);
    }

    /**
     * 根据处方ID查询执行记录
     */
    @GetMapping("/prescription/{prescriptionId}")
    public List<PrescriptionExecution> getByPrescriptionId(@PathVariable Long prescriptionId) {
        return prescriptionExecutionService.getByPrescriptionId(prescriptionId);
    }

    /**
     * 根据执行人ID查询执行记录
     */
    @GetMapping("/executor/{executorId}")
    public List<PrescriptionExecution> getByExecutorId(@PathVariable Long executorId) {
        return prescriptionExecutionService.getByExecutorId(executorId);
    }

    /**
     * 根据状态查询执行记录
     */
    @GetMapping("/status/{status}")
    public List<PrescriptionExecution> getByStatus(@PathVariable Integer status) {
        return prescriptionExecutionService.getByStatus(status);
    }

} 