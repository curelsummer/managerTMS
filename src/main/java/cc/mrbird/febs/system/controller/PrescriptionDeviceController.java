package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.system.domain.Prescription;
import cc.mrbird.febs.system.service.PrescriptionService;
import cc.mrbird.febs.system.service.PrescriptionDeviceService;
import cc.mrbird.febs.system.dto.PrescriptionDeviceRequest;
import cc.mrbird.febs.system.dto.PrescriptionDeviceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 处方设备通信控制器
 * 负责通过POST方法发送处方信息到设备，并通过MQTT进行通信
 */
@Slf4j
@RestController
@RequestMapping("/prescription-device")
@RequiredArgsConstructor
public class PrescriptionDeviceController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionDeviceService prescriptionDeviceService;

    /**
     * 发送处方到设备
     * POST /prescription-device/send
     */
    @PostMapping("/send")
    public FebsResponse sendPrescriptionToDevice(@Valid @RequestBody PrescriptionDeviceRequest request) {
        try {
            log.info("接收到发送处方到设备的请求: {}", request);
            
            // 验证处方是否存在
            Prescription prescription = prescriptionService.getById(request.getPrescriptionId());
            if (prescription == null) {
                return new FebsResponse().put("code", 404).message("处方不存在");
            }
            
            // 验证设备ID是否匹配
            if (!prescription.getDeviceId().equals(request.getDeviceId())) {
                return new FebsResponse().put("code", 400).message("处方与设备不匹配");
            }
            
            // 发送处方到设备
            PrescriptionDeviceResponse response = prescriptionDeviceService.sendPrescriptionToDevice(request);
            
            if (response.isSuccess()) {
                log.info("处方发送成功，处方ID: {}, 设备ID: {}", request.getPrescriptionId(), request.getDeviceId());
                return new FebsResponse().put("code", 200).message("处方发送成功").data(response);
            } else {
                log.error("处方发送失败，处方ID: {}, 设备ID: {}, 错误: {}", 
                    request.getPrescriptionId(), request.getDeviceId(), response.getErrorMessage());
                return new FebsResponse().put("code", 500).message("处方发送失败: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("发送处方到设备时发生异常", e);
            return new FebsResponse().put("code", 500).message("系统异常: " + e.getMessage());
        }
    }

    /**
     * 查询处方发送状态
     * GET /prescription-device/status/{prescriptionId}
     */
    @GetMapping("/status/{prescriptionId}")
    public FebsResponse getPrescriptionStatus(@PathVariable Long prescriptionId) {
        try {
            PrescriptionDeviceResponse status = prescriptionDeviceService.getPrescriptionStatus(prescriptionId);
            return new FebsResponse().put("code", 200).message("查询成功").data(status);
        } catch (Exception e) {
            log.error("查询处方状态时发生异常，处方ID: {}", prescriptionId, e);
            return new FebsResponse().put("code", 500).message("查询失败: " + e.getMessage());
        }
    }

    /**
     * 取消处方发送
     * POST /prescription-device/cancel/{prescriptionId}
     */
    @PostMapping("/cancel/{prescriptionId}")
    public FebsResponse cancelPrescription(@PathVariable Long prescriptionId) {
        try {
            boolean success = prescriptionDeviceService.cancelPrescription(prescriptionId);
            if (success) {
                return new FebsResponse().put("code", 200).message("处方取消成功");
            } else {
                return new FebsResponse().put("code", 400).message("处方取消失败");
            }
        } catch (Exception e) {
            log.error("取消处方时发生异常，处方ID: {}", prescriptionId, e);
            return new FebsResponse().put("code", 500).message("取消失败: " + e.getMessage());
        }
    }

    /**
     * 批量发送处方到设备
     * POST /prescription-device/batch-send
     */
    @PostMapping("/batch-send")
    public FebsResponse batchSendPrescriptions(@RequestBody java.util.List<PrescriptionDeviceRequest> requests) {
        try {
            log.info("接收到批量发送处方请求，数量: {}", requests.size());
            
            java.util.List<PrescriptionDeviceResponse> results = prescriptionDeviceService.batchSendPrescriptions(requests);
            
            // 统计成功和失败数量
            long successCount = results.stream().filter(PrescriptionDeviceResponse::isSuccess).count();
            long failCount = results.size() - successCount;
            
            return new FebsResponse()
                .put("code", 200)
                .message("批量发送完成")
                .put("totalCount", results.size())
                .put("successCount", successCount)
                .put("failCount", failCount)
                .put("results", results);
                
        } catch (Exception e) {
            log.error("批量发送处方时发生异常", e);
            return new FebsResponse().put("code", 500).message("批量发送失败: " + e.getMessage());
        }
    }
}

