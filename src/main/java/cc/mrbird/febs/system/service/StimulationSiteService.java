package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.StimulationSite;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

public interface StimulationSiteService extends IService<StimulationSite> {
    // 分页查询刺激部位
    IPage<StimulationSite> findStimulationSites(IPage<StimulationSite> page, StimulationSite site);
    
    // 根据分类查询刺激部位
    List<StimulationSite> findByCategory(String category);
    
    // 获取所有分类
    List<String> getAllCategories();
} 