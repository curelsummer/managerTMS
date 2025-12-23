package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableField;

@Data
public class Patient implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 患者ID（主键） */
    private Long id;
    /** 所属医院ID */
    private Long hospitalId;
    /** 所属科室ID */
    private Long departmentId;
    /** 姓名 */
    private String name;
    /** 身份证号 */
    private String idCard;
    /** 性别 */
    private String gender;
    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthday;
    /** HIS系统ID */
    private String hisId;
    /** 患者唯一码 */
    private String code;
    /** 创建人ID */
    private Long createdBy;
    /** 创建时间 */
    private Date createdAt;
    /** 最近操作人ID */
    private Long updatedBy;
    /** 最近操作时间 */
    private Date updatedAt;

    /** 阈值(0-100) */
    @TableField("threshold_value")
    private Integer thresholdValue;
    /** 阈值设置时间 */
    @TableField("threshold_set_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date thresholdSetAt;
    /** 阈值设置人ID */
    @TableField("threshold_set_by")
    private Long thresholdSetBy;
    /** 阈值来源: manual/measure */
    @TableField("threshold_source")
    private String thresholdSource;
    /** MEP值 */
    @TableField("mep_value")
    private Integer mepValue;
} 