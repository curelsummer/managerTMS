package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.system.domain.Patient;
import cc.mrbird.febs.system.service.PatientService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.SecurityUtils;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cc.mrbird.febs.common.authentication.JWTUtil;
import cc.mrbird.febs.common.utils.FebsUtil;

@RestController
@RequestMapping("/patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private UserService userService;

    @GetMapping
    public List<Patient> list() {
        return patientService.list();
    }

    @GetMapping("/page")
    public IPage<Patient> page(@RequestHeader("Authentication") String token,
                               @RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize,
                               Patient patient) {
        // 先解密token再解析userId
        String realToken = FebsUtil.decryptToken(token);
        Long userId = JWTUtil.getUserId(realToken);
        return patientService.findPatients(new Page<>(pageNum, pageSize), patient, userId);
    }

    @GetMapping("/{id}")
    public Patient get(@PathVariable Long id) {
        return patientService.getById(id);
    }

    @PostMapping
    public boolean add(@RequestBody Patient patient) {
        return patientService.save(patient);
    }

    @PutMapping
    public boolean update(@RequestBody Patient patient) {
        return patientService.updateById(patient);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return patientService.removeById(id);
    }
} 