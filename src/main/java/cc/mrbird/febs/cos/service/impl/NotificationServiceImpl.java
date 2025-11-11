package cc.mrbird.febs.cos.service.impl;

import cc.mrbird.febs.cos.service.IMailService;
import cc.mrbird.febs.cos.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final IMailService mailService;

    @Value("${febs.notify.doctor.email:}")
    private String doctorEmail;

    @Override
    public void notifyDoctor(String patientId, String message) {
        log.info("通知医生患者({}) 需要设置处方: {}", patientId, message);
        if (doctorEmail != null && !doctorEmail.isEmpty()) {
            try {
                mailService.sendSimpleMail(doctorEmail, "处方待设置提醒", "患者ID: " + patientId + "，" + message);
            } catch (Exception e) {
                log.warn("发送医生通知邮件失败: {}", e.getMessage());
            }
        }
    }
}


