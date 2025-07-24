package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;

@Data
@TableName("device")
public class Device implements Serializable {
    @TableId(value = "device_id")
    private Long deviceId;
    private Long hospitalId;
    private String deviceType;
    private String sn;
    private String status;
    private Date lastHeartbeat;
    private Long createdBy;
    private Date createdAt;
    private Long updatedBy;
    private Date updatedAt;
    private String extendField1;
    private String extendField2;
    private String extendField3;
} 