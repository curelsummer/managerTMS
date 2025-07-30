package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

@Data
@TableName("prescription_execution")
public class PrescriptionExecution implements Serializable {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long patientId;
    private Long hospitalId; // 医院ID
    private Long deviceId;
    private Long prescriptionId;
    private Long executorId;
    private Integer status; // 0-草稿/1-已下发/2-执行中/3-完成/4-异常
    private String progress;
    private String exception;
    
    // 审计字段
    private Long createdBy;
    private Date createdAt;
    private Long updatedBy;
    private Date updatedAt;
} 