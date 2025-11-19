package cc.mrbird.febs.cos.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 设备指标数据
 *
 * @author FanK
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("device_metrics")
public class DeviceMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备编号
     */
    private String deviceId;

    /**
     * 线圈温度（℃）
     */
    private java.math.BigDecimal coilTemperature;

    /**
     * 机温（℃）
     */
    private java.math.BigDecimal machineTemperature;

    /**
     * 水泵流速（L/min）
     */
    private java.math.BigDecimal pumpFlowRate;

    /**
     * 消息ID，用于去重
     */
    private String msgId;

    /**
     * 创建时间（数据上报时间）
     */
    private LocalDateTime createTime;

    /**
     * 服务器接收时间
     */
    private LocalDateTime serverTime;
}

