package cc.mrbird.febs.system.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 服务器记录ID生成器
 * 格式：{类型}-{日期}-{时间}-{设备号}-{本地记录ID}
 * 示例：MEP-20241211-143000-001-156
 */
public class ServerRecordIdGenerator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    /**
     * 生成MEP记录的服务器ID
     * 
     * @param deviceNo 设备编号
     * @param localMepRecordId 设备本地MEP记录ID
     * @return 服务器记录ID，格式：MEP-20241211-143000-001-156
     */
    public static String generateMepRecordId(Integer deviceNo, Long localMepRecordId) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DATE_FORMATTER);
        String time = now.format(TIME_FORMATTER);
        String deviceNoStr = String.format("%03d", deviceNo); // 设备号固定3位，不足补0
        
        return String.format("MEP-%s-%s-%s-%d", date, time, deviceNoStr, localMepRecordId);
    }
    
    /**
     * 生成治疗记录的服务器ID
     * 
     * @param deviceNo 设备编号
     * @param localMedicalRecordId 设备本地病历ID
     * @return 服务器记录ID，格式：TR-20241211-150000-001-289
     */
    public static String generateTreatmentRecordId(Integer deviceNo, Long localMedicalRecordId) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DATE_FORMATTER);
        String time = now.format(TIME_FORMATTER);
        String deviceNoStr = String.format("%03d", deviceNo); // 设备号固定3位，不足补0
        
        return String.format("TR-%s-%s-%s-%d", date, time, deviceNoStr, localMedicalRecordId);
    }
    
    /**
     * 生成患者唯一标识
     * 
     * @param patientName 患者姓名
     * @param patientBirthday 患者出生日期（格式：yyyy-MM-dd）
     * @return 患者唯一标识，格式：张三_1990-05-15 或 张三_unknown
     */
    public static String generatePatientIdentifier(String patientName, String patientBirthday) {
        if (patientName == null || patientName.trim().isEmpty()) {
            throw new IllegalArgumentException("患者姓名不能为空");
        }
        
        String birthday = patientBirthday;
        if (birthday == null || birthday.trim().isEmpty()) {
            birthday = "unknown";
        }
        
        return patientName.trim() + "_" + birthday.trim();
    }
}





