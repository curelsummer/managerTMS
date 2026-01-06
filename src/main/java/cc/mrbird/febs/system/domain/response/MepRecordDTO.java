package cc.mrbird.febs.system.domain.response;

import lombok.Data;
import java.util.List;

/**
 * MEP记录DTO（用于同步响应）
 */
@Data
public class MepRecordDTO {
    
    /**
     * 服务器记录ID
     */
    private String serverRecordId;
    
    /**
     * 来源设备编号
     */
    private Integer sourceDeviceNo;
    
    /**
     * 患者姓名
     */
    private String patientName;
    
    /**
     * 患者性别
     */
    private String patientSex;
    
    /**
     * 患者年龄字符串
     */
    private String patientAgeStr;
    
    /**
     * 患者出生日期
     */
    private String patientBirthday;
    
    /**
     * 记录时间
     */
    private String recordTime;
    
    /**
     * 数据类型
     */
    private Integer dType;
    
    /**
     * MEP数据列表
     */
    private List<MepDataItemDTO> mepDataList;
    
    @Data
    public static class MepDataItemDTO {
        private Integer mt;
        private String ch;
        private Double maxValue;
        private Double maxTime;
        private Double minValue;
        private Double minTime;
        private Double amplitude;
        private String part;
        private String recordPart;
    }
}





