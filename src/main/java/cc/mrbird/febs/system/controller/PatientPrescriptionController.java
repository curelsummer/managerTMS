package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.system.dto.PatientPrescriptionBindRequest;
import cc.mrbird.febs.system.service.PatientPrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/patient-prescription")
@RequiredArgsConstructor
public class PatientPrescriptionController {

    private final PatientPrescriptionService patientPrescriptionService;

    /**
     * 绑定患者与处方（网页端调用）
     * POST /patient-prescription/bind
     */
    @PostMapping("/bind")
    public FebsResponse bind(@Valid @RequestBody PatientPrescriptionBindRequest req) {
        try {
            patientPrescriptionService.recordBind(req.getPatientId(), req.getPrescriptionId());
            return new FebsResponse().put("code", 200).message("绑定成功");
        } catch (Exception e) {
            log.warn("绑定患者与处方失败 patientId={}, prescriptionId={}, err={}", req.getPatientId(), req.getPrescriptionId(), e.getMessage());
            return new FebsResponse().put("code", 500).message("绑定失败: " + e.getMessage());
        }
    }
}


