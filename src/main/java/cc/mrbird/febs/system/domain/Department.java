package cc.mrbird.febs.system.domain;

import lombok.Data;
import java.io.Serializable;

@Data
public class Department implements Serializable {
    private Long departmentId;
    private Long hospitalId;
    private String name;
    private Long createdBy;
    private java.util.Date createdAt;
    private Long updatedBy;
    private java.util.Date updatedAt;
} 