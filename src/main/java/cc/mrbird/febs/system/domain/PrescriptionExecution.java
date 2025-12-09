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
    
    // 状态定义：0-待领取(PENDING) / 1-已领取/待执行(CLAIMED) / 2-执行中(IN_PROGRESS) / 3-完成(COMPLETED) / 4-异常(ERROR)
    private Integer status;
    private String progress;
    private String exception;
    
    // ========== 新增：认领相关字段 ==========
    private Integer claimedDeviceNo;  // 领取设备编号
    private Date claimedTime;         // 领取时间
    private Date broadcastTime;       // 广播时间
    
    // 审计字段
    private Long createdBy;
    private Date createdAt;
    private Long updatedBy;
    private Date updatedAt;
} 