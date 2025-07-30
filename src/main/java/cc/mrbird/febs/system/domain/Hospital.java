package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@TableName("t_hospital")
public class Hospital implements Serializable {
    private Long hospitalId;
    private String name;
    private String address;
    private String contact;
    private Long createdBy;
    private Date createdAt;
    private Long updatedBy;
    private Date updatedAt;
} 