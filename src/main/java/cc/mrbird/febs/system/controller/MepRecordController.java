package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.system.domain.vo.MepRecordVO;
import cc.mrbird.febs.system.service.MepRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MEP记录控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("mepRecord")
public class MepRecordController {

    @Autowired
    private MepRecordService mepRecordService;

    /**
     * 根据患者唯一标识查询MEP记录列表（包括关联的和独立的）
     */
    @GetMapping("patient/{patientIdentifier}")
    public FebsResponse getMepRecordsByPatientIdentifier(@PathVariable String patientIdentifier) {
        List<MepRecordVO> mepRecords = mepRecordService.getMepRecordVOByPatientIdentifier(patientIdentifier);
        return new FebsResponse().put("success", true).put("data", mepRecords);
    }

    /**
     * 根据ID查询MEP记录详情（包含MEP数据）
     */
    @GetMapping("{id}")
    public FebsResponse getMepRecordDetail(@PathVariable Long id) {
        MepRecordVO mepRecord = mepRecordService.getMepRecordDetailById(id);
        return new FebsResponse().put("success", true).put("data", mepRecord);
    }
}

