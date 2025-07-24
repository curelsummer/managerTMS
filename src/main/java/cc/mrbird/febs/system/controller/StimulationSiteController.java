package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.common.domain.FebsResponse;
import cc.mrbird.febs.system.domain.StimulationSite;
import cc.mrbird.febs.system.service.StimulationSiteService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/stimulation-site")
public class StimulationSiteController {
    
    @Autowired
    private StimulationSiteService stimulationSiteService;

    /**
     * 获取所有刺激部位（不分页）
     */
    @GetMapping("/list")
    public FebsResponse list() {
        List<StimulationSite> list = stimulationSiteService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StimulationSite>()
                .eq(StimulationSite::getIsActive, 1)
                .orderByAsc(StimulationSite::getSortOrder)
                .orderByAsc(StimulationSite::getSiteId)
        );
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 分页查询刺激部位
     */
    @GetMapping("/page")
    public FebsResponse page(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            StimulationSite site) {
        IPage<StimulationSite> page = stimulationSiteService.findStimulationSites(
            new Page<>(pageNum, pageSize), site
        );
        return new FebsResponse().put("code", 200).message("success").data(page);
    }

    /**
     * 根据ID获取刺激部位详情
     */
    @GetMapping("/{id}")
    public FebsResponse get(@PathVariable Integer id) {
        StimulationSite site = stimulationSiteService.getById(id);
        if (site != null) {
            return new FebsResponse().put("code", 200).message("success").data(site);
        } else {
            return new FebsResponse().put("code", 404).message("刺激部位不存在");
        }
    }

    /**
     * 新增刺激部位
     */
    @PostMapping
    public FebsResponse add(@RequestBody StimulationSite site) {
        // 设置默认值
        if (site.getIsActive() == null) {
            site.setIsActive(1);
        }
        if (site.getSortOrder() == null) {
            site.setSortOrder(0);
        }
        
        Date now = new Date();
        site.setCreatedTime(now);
        site.setUpdatedTime(now);
        
        boolean success = stimulationSiteService.save(site);
        if (success) {
            return new FebsResponse().put("code", 200).message("success");
        } else {
            return new FebsResponse().put("code", 500).message("新增失败");
        }
    }

    /**
     * 更新刺激部位
     */
    @PutMapping
    public FebsResponse update(@RequestBody StimulationSite site) {
        site.setUpdatedTime(new Date());
        
        boolean success = stimulationSiteService.updateById(site);
        if (success) {
            return new FebsResponse().put("code", 200).message("success");
        } else {
            return new FebsResponse().put("code", 500).message("更新失败");
        }
    }

    /**
     * 删除刺激部位
     */
    @DeleteMapping("/{id}")
    public FebsResponse delete(@PathVariable Integer id) {
        boolean success = stimulationSiteService.removeById(id);
        if (success) {
            return new FebsResponse().put("code", 200).message("success");
        } else {
            return new FebsResponse().put("code", 500).message("删除失败");
        }
    }

    /**
     * 根据分类查询刺激部位
     */
    @GetMapping("/by-category/{category}")
    public FebsResponse getByCategory(@PathVariable String category) {
        List<StimulationSite> list = stimulationSiteService.findByCategory(category);
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    public FebsResponse getCategories() {
        List<String> categories = stimulationSiteService.getAllCategories();
        return new FebsResponse().put("code", 200).message("success").data(categories);
    }

    /**
     * 启用/禁用刺激部位
     */
    @PutMapping("/{id}/toggle-status")
    public FebsResponse toggleStatus(@PathVariable Integer id) {
        StimulationSite site = stimulationSiteService.getById(id);
        if (site == null) {
            return new FebsResponse().put("code", 404).message("刺激部位不存在");
        }
        
        // 切换状态
        site.setIsActive(site.getIsActive() == 1 ? 0 : 1);
        site.setUpdatedTime(new Date());
        
        boolean success = stimulationSiteService.updateById(site);
        if (success) {
            return new FebsResponse().put("code", 200).message("success").data(site.getIsActive());
        } else {
            return new FebsResponse().put("code", 500).message("状态更新失败");
        }
    }

    /**
     * 批量删除刺激部位
     */
    @DeleteMapping("/batch")
    public FebsResponse batchDelete(@RequestBody List<Integer> ids) {
        boolean success = stimulationSiteService.removeByIds(ids);
        if (success) {
            return new FebsResponse().put("code", 200).message("success");
        } else {
            return new FebsResponse().put("code", 500).message("批量删除失败");
        }
    }

    /**
     * 根据名称模糊查询
     */
    @GetMapping("/search")
    public FebsResponse search(@RequestParam String keyword) {
        List<StimulationSite> list = stimulationSiteService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StimulationSite>()
                .like(StimulationSite::getSiteName, keyword)
                .eq(StimulationSite::getIsActive, 1)
                .orderByAsc(StimulationSite::getSortOrder)
                .orderByAsc(StimulationSite::getSiteId)
        );
        return new FebsResponse().put("code", 200).message("success").data(list);
    }

    /**
     * 获取启用的刺激部位
     */
    @GetMapping("/active")
    public FebsResponse getActiveSites() {
        List<StimulationSite> list = stimulationSiteService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StimulationSite>()
                .eq(StimulationSite::getIsActive, 1)
                .orderByAsc(StimulationSite::getSortOrder)
                .orderByAsc(StimulationSite::getSiteId)
        );
        return new FebsResponse().put("code", 200).message("success").data(list);
    }
} 