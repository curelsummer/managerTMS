package cc.mrbird.febs.system.service;

public interface ThresholdService {

    /**
     * 判断患者是否存在阈值（范围 0-100，只用于数值校验；此处仅检查是否存在记录）
     */
    boolean existsPatientThreshold(String patientId);
}


