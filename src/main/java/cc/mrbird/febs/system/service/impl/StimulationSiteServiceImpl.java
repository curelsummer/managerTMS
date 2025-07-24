package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.StimulationSite;
import cc.mrbird.febs.system.dao.StimulationSiteMapper;
import cc.mrbird.febs.system.service.StimulationSiteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.stream.Collectors;

@Service("stimulationSiteService")
public class StimulationSiteServiceImpl extends ServiceImpl<StimulationSiteMapper, StimulationSite> implements StimulationSiteService {

    @Override
    public IPage<StimulationSite> findStimulationSites(IPage<StimulationSite> page, StimulationSite site) {
        QueryWrapper<StimulationSite> wrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (site != null) {
            if (StringUtils.hasText(site.getSiteName())) {
                wrapper.like("site_name", site.getSiteName());
            }
            if (StringUtils.hasText(site.getSiteCategory())) {
                wrapper.eq("site_category", site.getSiteCategory());
            }
            if (site.getIsActive() != null) {
                wrapper.eq("is_active", site.getIsActive());
            }
        }
        
        // 按排序顺序和ID排序
        wrapper.orderByAsc("sort_order", "site_id");
        
        return this.baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<StimulationSite> findByCategory(String category) {
        QueryWrapper<StimulationSite> wrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(category)) {
            wrapper.eq("site_category", category);
        }
        
        // 只查询启用的记录
        wrapper.eq("is_active", 1);
        
        // 按排序顺序和ID排序
        wrapper.orderByAsc("sort_order", "site_id");
        
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<String> getAllCategories() {
        List<StimulationSite> sites = this.baseMapper.selectList(
            new QueryWrapper<StimulationSite>()
                .select("DISTINCT site_category")
                .isNotNull("site_category")
                .ne("site_category", "")
                .eq("is_active", 1)
        );
        
        return sites.stream()
            .map(StimulationSite::getSiteCategory)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }
} 