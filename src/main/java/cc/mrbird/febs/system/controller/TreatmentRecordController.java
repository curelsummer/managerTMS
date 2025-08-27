package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.common.domain.QueryRequest;
import cc.mrbird.febs.system.domain.query.TreatmentRecordQuery;
import cc.mrbird.febs.system.domain.vo.TreatmentRecordVO;
import cc.mrbird.febs.system.service.TreatmentRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 治疗记录控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("treatmentRecord")
public class TreatmentRecordController {

    @Autowired
    private TreatmentRecordService treatmentRecordService;

    /**
     * 分页查询治疗记录
     */
    @GetMapping
    public FebsResponse treatmentRecordList(QueryRequest request, TreatmentRecordQuery query) {
        IPage<TreatmentRecordVO> page = treatmentRecordService.findTreatmentRecordPage(request, query);
        return new FebsResponse().put("success", true).put("data", page);
    }

    /**
     * 根据ID查询治疗记录详情（包含关联数据）
     */
    @GetMapping("{id}")
    public FebsResponse getTreatmentRecord(@PathVariable Long id) {
        TreatmentRecordVO treatmentRecord = treatmentRecordService.findTreatmentRecordById(id);
        return new FebsResponse().put("success", true).put("data", treatmentRecord);
    }

    /**
     * 根据ID查询治疗记录完整详情（包含所有关联数据）
     */
    @GetMapping("detail/{id}")
    public FebsResponse getTreatmentRecordDetail(@PathVariable Long id) {
        TreatmentRecordVO treatmentRecord = treatmentRecordService.findTreatmentRecordDetailById(id);
        return new FebsResponse().put("success", true).put("data", treatmentRecord);
    }

    /**
     * 查询治疗记录列表（不分页）
     */
    @GetMapping("list")
    public FebsResponse treatmentRecordList(TreatmentRecordQuery query) {
        List<TreatmentRecordVO> list = treatmentRecordService.findTreatmentRecordList(query);
        return new FebsResponse().put("success", true).put("data", list);
    }

    /**
     * 删除治疗记录
     */
    @DeleteMapping("{id}")
    public FebsResponse deleteTreatmentRecord(@PathVariable Long id) {
        boolean success = treatmentRecordService.deleteTreatmentRecord(id);
        return new FebsResponse().put("success", success);
    }

    /**
     * 批量删除治疗记录
     */
    @DeleteMapping("batch/{ids}")
    public FebsResponse deleteTreatmentRecords(@PathVariable String[] ids) {
        boolean success = treatmentRecordService.deleteTreatmentRecords(ids);
        return new FebsResponse().put("success", success);
    }
}
