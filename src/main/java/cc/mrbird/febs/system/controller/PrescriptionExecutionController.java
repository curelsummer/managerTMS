package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
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
import cc.mrbird.febs.system.service.DeviceService;
import cc.mrbird.febs.system.domain.Device;

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

    @Autowired
    private DeviceService deviceService;

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
            // ========== 1. 验证必填字段 ==========
            if (execution.getDeviceId() == null) {
                return new FebsResponse().put("success", false).message("设备ID不能为空");
            }
            if (execution.getPatientId() == null) {
                return new FebsResponse().put("success", false).message("患者ID不能为空");
            }
            if (execution.getPrescriptionId() == null) {
                return new FebsResponse().put("success", false).message("处方ID不能为空");
            }
            
            // ========== 2. 验证设备有效性 ==========
            Device device = deviceService.getById(execution.getDeviceId());
            if (device == null) {
                return new FebsResponse()
                    .put("success", false)
                    .message("设备不存在，设备ID: " + execution.getDeviceId());
            }
            
            if (device.getDeviceNo() == null) {
                return new FebsResponse()
                    .put("success", false)
                    .message("设备编号未设置，无法下发到上位机");
            }
            
            // ========== 3. 设置审计字段 ==========
            String realToken = FebsUtil.decryptToken(token);
            Long userId = JWTUtil.getUserId(realToken);
            
            execution.setCreatedBy(userId);
            execution.setCreatedAt(new Date());
            execution.setUpdatedBy(userId);
            execution.setUpdatedAt(new Date());
            
            // 设置执行人ID为当前登录用户
            execution.setExecutorId(userId);
            
            // ========== 4. 设置医院ID ==========
            if (execution.getHospitalId() == null) {
                User user = userService.getById(userId);
                if (user != null && user.getDeptId() != null) {
                    Long hospitalId = departmentMapper.findHospitalIdByDepartmentId(user.getDeptId());
                    if (hospitalId != null) {
                        execution.setHospitalId(hospitalId);
                    }
                }
            }
            
            // ========== 5. 强制设置初始状态为待下发 ==========
            execution.setStatus(0); // 0-待下发，等待上位机确认
            execution.setProgress("等待下发"); // 设置初始进度描述
            
            // ========== 6. 保存记录 ==========
            boolean success = prescriptionExecutionService.save(execution);
            
            if (!success) {
                return new FebsResponse().put("success", false).message("保存执行记录失败");
            }
            
            System.out.println("=== 处方执行记录创建成功 ===");
            System.out.println("执行记录ID: " + execution.getId());
            System.out.println("患者ID: " + execution.getPatientId());
            System.out.println("处方ID: " + execution.getPrescriptionId());
            System.out.println("设备ID: " + execution.getDeviceId());
            System.out.println("设备编号: " + device.getDeviceNo());
            System.out.println("状态: 0 (待下发)");
            System.out.println("执行人ID: " + execution.getExecutorId());
            System.out.println("医院ID: " + execution.getHospitalId());
            
            // ========== 7. 广播给在线上位机（不抛出异常） ==========
            try {
                notificationService.notifyPrescriptionExecutionCreated(execution);
                System.out.println("WebSocket广播成功");
            } catch (Exception e) {
                System.err.println("WebSocket广播失败（不影响业务）: " + e.getMessage());
                e.printStackTrace();
                // 记录保持 status=0，上位机重连时会收到
            }
            
            // ========== 8. 返回成功响应 ==========
            return new FebsResponse()
                .put("success", true)
                .put("executionId", execution.getId())
                .put("deviceNo", device.getDeviceNo())
                .put("deviceId", device.getDeviceId())
                .message("处方执行记录创建成功，等待上位机确认");
                
        } catch (Exception e) {
            System.err.println("创建处方执行记录异常: " + e.getMessage());
            e.printStackTrace();
            return new FebsResponse()
                .put("success", false)
                .message("系统错误：" + e.getMessage());
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