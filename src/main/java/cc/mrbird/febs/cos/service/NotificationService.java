package cc.mrbird.febs.cos.service;

public interface NotificationService {

    /**
     * 通知医生为患者设置处方
     */
    void notifyDoctor(String patientId, String message);
}


