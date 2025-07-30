package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

@Data
@TableName("prescription")
public class Prescription implements Serializable {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long patientId;
    private Long hospitalId;
    private Long doctorId;
    private Long deviceId; // 执行时使用的设备ID
    
    // 处方类型和状态
    private Integer presType;
    private Integer status;
    
    // 基础参数
    private Integer presStrength;
    private BigDecimal presFreq;
    private BigDecimal lastTime;
    private Integer pauseTime;
    private Integer repeatCount;
    private Integer totalCount;
    private Integer totalTime;
    
    // 治疗部位
    private Integer presPartId;
    private String presPartName;
    
    // 标准处方关联
    private Integer standardPresId;
    private String standardPresName;
    
    // TBS专用参数
    private Integer tbsType;
    private Integer innerCount;
    private BigDecimal interFreq;
    private Integer interCount;
    
    // 其他参数
    private Integer periods;
    
    // 审计字段
    private Long createdBy;
    private Date createdAt;
    private Long updatedBy;
    private Date updatedAt;
} 