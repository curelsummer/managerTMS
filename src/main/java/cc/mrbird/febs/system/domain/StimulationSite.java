package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

@Data
@TableName("stimulation_site")
public class StimulationSite implements Serializable {
    
    @TableId(value = "site_id", type = IdType.AUTO)
    private Integer siteId;
    
    private String siteName;
    private String siteCategory;
    private String siteDescription;
    private Integer isActive;
    private Integer sortOrder;
    private Date createdTime;
    private Date updatedTime;
} 