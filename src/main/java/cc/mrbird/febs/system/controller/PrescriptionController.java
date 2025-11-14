package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.authentication.JWTUtil;
import cc.mrbird.febs.common.utils.FebsUtil;
import cc.mrbird.febs.system.domain.Prescription;
import cc.mrbird.febs.system.service.PrescriptionService;
import cc.mrbird.febs.system.service.PatientPrescriptionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/prescription")
public class PrescriptionController {
    
    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired(required = false)
    private PatientPrescriptionService patientPrescriptionService;

    /**
     * 获取所有处方列表
     */
    @GetMapping
    public List<Prescription> list() {
        return prescriptionService.list();
    }

    /**
     * 分页查询处方
     */
    @GetMapping("/page")
    public IPage<Prescription> page(@RequestHeader("Authentication") String token,
                                   @RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "10") int pageSize,
                                   Prescription prescription) {
        // 先解密token再解析userId
        String realToken = FebsUtil.decryptToken(token);
        Long userId = JWTUtil.getUserId(realToken);
        // 调用自定义的findPrescriptions方法
        return prescriptionService.findPrescriptions(new Page<>(pageNum, pageSize), prescription, userId);
    }

    /**
     * 根据ID获取处方详情
     */
    @GetMapping("/{id}")
    public Prescription get(@PathVariable Long id) {
        return prescriptionService.getById(id);
    }

    /**
     * 新增处方
     */
    @PostMapping
    public boolean add(@RequestHeader("Authentication") String token, @RequestBody Prescription prescription) {
        // 设置创建时间和创建人
        String realToken = FebsUtil.decryptToken(token);
        Long userId = JWTUtil.getUserId(realToken);
        
        prescription.setCreatedBy(userId);
        prescription.setCreatedAt(new Date());
        prescription.setUpdatedBy(userId);
        prescription.setUpdatedAt(new Date());
        
        // 设置默认值
        if (prescription.getPresType() == null) {
            prescription.setPresType(0); // 默认普通处方
        }
        if (prescription.getStatus() == null) {
            prescription.setStatus(0); // 默认草稿状态
        }
        if (prescription.getPauseTime() == null) {
            prescription.setPauseTime(0);
        }
        if (prescription.getPeriods() == null) {
            prescription.setPeriods(1);
        }
        
        boolean ok = prescriptionService.save(prescription);
        // 若网页端设置了 patientId，则建立患者-处方绑定关系（去重由 Service 处理）
        if (ok && prescription.getPatientId() != null && patientPrescriptionService != null) {
            try {
                patientPrescriptionService.recordBind(prescription.getPatientId(), prescription.getId());
            } catch (Exception ignore) {
            }
        }
        return ok;
    }

    /**
     * 更新处方
     */
    @PutMapping
    public boolean update(@RequestHeader("Authentication") String token, @RequestBody Prescription prescription) {
        // 设置更新时间和更新人
        String realToken = FebsUtil.decryptToken(token);
        Long userId = JWTUtil.getUserId(realToken);
        
        prescription.setUpdatedBy(userId);
        prescription.setUpdatedAt(new Date());
        
        boolean ok = prescriptionService.updateById(prescription);
        // 若网页端更新时带有 patientId，则建立患者-处方绑定关系（去重由 Service 处理）
        if (ok && prescription.getId() != null && prescription.getPatientId() != null && patientPrescriptionService != null) {
            try {
                patientPrescriptionService.recordBind(prescription.getPatientId(), prescription.getId());
            } catch (Exception ignore) {
            }
        }
        return ok;
    }

    /**
     * 删除处方
     */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return prescriptionService.removeById(id);
    }

    /**
     * 批量删除处方
     */
    @DeleteMapping("/batch")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return prescriptionService.removeByIds(ids);
    }

    /**
     * 根据患者ID查询处方
     */
    @GetMapping("/patient/{patientId}")
    public List<Prescription> getByPatientId(@PathVariable Long patientId) {
        return prescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getPatientId, patientId)
                .orderByDesc(Prescription::getCreatedAt)
        );
    }

    /**
     * 根据医生ID查询处方
     */
    @GetMapping("/doctor/{doctorId}")
    public List<Prescription> getByDoctorId(@PathVariable Long doctorId) {
        return prescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getDoctorId, doctorId)
                .orderByDesc(Prescription::getCreatedAt)
        );
    }
} 