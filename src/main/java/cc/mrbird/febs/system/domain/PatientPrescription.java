package cc.mrbird.febs.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("patient_prescription")
public class PatientPrescription implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private Long prescriptionId;

    /** 首次建立关系时间（由应用写入） */
    private Date createdAt;
    /** 最近一次使用时间（发送处方成功后更新） */
    private Date lastUsedAt;
}


