package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

@Data
@TableName("standard_prescription")
public class StandardPrescription implements Serializable {
    
    @TableId(value = "standard_pres_id", type = IdType.AUTO)
    private Integer standardPresId;
    
    private String indication1;
    private String indication2;
    private String indication3;
    private String indicationKeyword;
    private Float presFreq;
    private Float lastTime;
    private Integer lastCount;
    private Integer pauseTime;
    private Integer repeatCount;
    private Integer totalTime;
    private String presPartId;
    private String presPart;
    private Integer isDeleted;
    private String evidenceLevel;
    private String provenance;
    private Integer builtIn;
} 