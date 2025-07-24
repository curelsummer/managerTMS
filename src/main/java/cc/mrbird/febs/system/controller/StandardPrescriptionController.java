package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.system.domain.StandardPrescription;
import cc.mrbird.febs.system.service.StandardPrescriptionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/standard-prescription")
public class StandardPrescriptionController {
    
    @Autowired
    private StandardPrescriptionService standardPrescriptionService;

    /**
     * 获取所有标准处方（不分页）
     */
    @GetMapping("/list")
    public FebsResponse list() {
        List<StandardPrescription> list = standardPrescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StandardPrescription>()
                .eq(StandardPrescription::getIsDeleted, 0)
                .orderByAsc(StandardPrescription::getStandardPresId)
        );
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 分页查询标准处方
     */
    @GetMapping("/page")
    public FebsResponse page(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            StandardPrescription prescription) {
        IPage<StandardPrescription> page = standardPrescriptionService.findStandardPrescriptions(
            new Page<>(pageNum, pageSize), prescription
        );
        return new FebsResponse().put("code", 200).message("success").data(page);
    }

    /**
     * 根据治疗部位查询标准处方
     */
    @GetMapping("/by-part")
    public FebsResponse getByPartName(@RequestParam String partName) {
        List<StandardPrescription> list = standardPrescriptionService.findByPartName(partName);
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 根据ID获取标准处方详情
     */
    @GetMapping("/{id}")
    public FebsResponse get(@PathVariable Integer id) {
        StandardPrescription prescription = standardPrescriptionService.getById(id);
        if (prescription != null && prescription.getIsDeleted() == 0) {
            return new FebsResponse().put("code", 200).message("success").data(prescription);
        } else {
            return new FebsResponse().put("code", 404).message("标准处方不存在");
        }
    }

    /**
     * 新增标准处方
     */
    @PostMapping
    public FebsResponse add(@RequestBody StandardPrescription prescription) {
        // 设置默认值
        if (prescription.getIsDeleted() == null) {
            prescription.setIsDeleted(0);
        }
        if (prescription.getBuiltIn() == null) {
            prescription.setBuiltIn(0); // 默认用户自定义
        }
        
        boolean success = standardPrescriptionService.save(prescription);
        if (success) {
            return new FebsResponse().put("code", 200).message("success");
        } else {
            return new FebsResponse().put("code", 500).message("新增失败");
        }
    }

    /**
     * 更新标准处方
     */
    @PutMapping
    public FebsResponse update(@RequestBody StandardPrescription prescription) {
        boolean success = standardPrescriptionService.updateById(prescription);
        if (success) {
            return new FebsResponse().put("code", 200).message("success");
        } else {
            return new FebsResponse().put("code", 500).message("更新失败");
        }
    }

    /**
     * 删除标准处方（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public FebsResponse delete(@PathVariable Integer id) {
        StandardPrescription prescription = new StandardPrescription();
        prescription.setStandardPresId(id);
        prescription.setIsDeleted(1);
        
        boolean success = standardPrescriptionService.updateById(prescription);
        if (success) {
            return new FebsResponse().put("code", 200).message("success");
        } else {
            return new FebsResponse().put("code", 500).message("删除失败");
        }
    }

    /**
     * 根据适应症分类查询
     */
    @GetMapping("/by-indication1/{indication1}")
    public FebsResponse getByIndication1(@PathVariable String indication1) {
        List<StandardPrescription> list = standardPrescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StandardPrescription>()
                .eq(StandardPrescription::getIndication1, indication1)
                .eq(StandardPrescription::getIsDeleted, 0)
                .orderByAsc(StandardPrescription::getStandardPresId)
        );
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 根据具体疾病查询
     */
    @GetMapping("/by-indication2/{indication2}")
    public FebsResponse getByIndication2(@PathVariable String indication2) {
        List<StandardPrescription> list = standardPrescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StandardPrescription>()
                .eq(StandardPrescription::getIndication2, indication2)
                .eq(StandardPrescription::getIsDeleted, 0)
                .orderByAsc(StandardPrescription::getStandardPresId)
        );
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 获取所有适应症分类
     */
    @GetMapping("/indication1-list")
    public FebsResponse getIndication1List() {
        List<String> list = standardPrescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StandardPrescription>()
                .select(StandardPrescription::getIndication1)
                .eq(StandardPrescription::getIsDeleted, 0)
                .isNotNull(StandardPrescription::getIndication1)
                .ne(StandardPrescription::getIndication1, "")
        ).stream().map(StandardPrescription::getIndication1).distinct().collect(Collectors.toList());
        
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 获取所有具体疾病
     */
    @GetMapping("/indication2-list")
    public FebsResponse getIndication2List() {
        List<String> list = standardPrescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StandardPrescription>()
                .select(StandardPrescription::getIndication2)
                .eq(StandardPrescription::getIsDeleted, 0)
                .isNotNull(StandardPrescription::getIndication2)
                .ne(StandardPrescription::getIndication2, "")
        ).stream().map(StandardPrescription::getIndication2).distinct().collect(Collectors.toList());
        
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 获取所有治疗部位
     */
    @GetMapping("/pres-part-list")
    public FebsResponse getPresPartList() {
        List<String> list = standardPrescriptionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StandardPrescription>()
                .select(StandardPrescription::getPresPart)
                .eq(StandardPrescription::getIsDeleted, 0)
                .isNotNull(StandardPrescription::getPresPart)
                .ne(StandardPrescription::getPresPart, "")
        ).stream().map(StandardPrescription::getPresPart).distinct().collect(Collectors.toList());
        
        return new FebsResponse().put("code", 200).message("success").data(list);
    }
} 