package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.PrescriptionExecution;
import cc.mrbird.febs.system.domain.PrescriptionExecutionNotification;
import cc.mrbird.febs.system.websocket.PrescriptionExecutionWebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 处方执行记录通知服务
 */
@Service
public class PrescriptionExecutionNotificationService {
    
    @Autowired
    private PrescriptionService prescriptionService;
    
    @Autowired
    private PatientService patientService;
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private HospitalService hospitalService;
    
    /**
     * 处理处方执行记录创建后的通知
     */
    public void notifyPrescriptionExecutionCreated(PrescriptionExecution execution) {
        try {
            System.out.println("=== 开始构建WebSocket通知数据 ===");
            System.out.println("执行记录ID: " + execution.getId());
            
            // 构建通知数据
            PrescriptionExecutionNotification notification = buildNotification(execution);
            
            System.out.println("=== 通知数据构建完成 ===");
            System.out.println("消息类型: " + notification.getMessageType());
            System.out.println("时间戳: " + notification.getTimestamp());
            System.out.println("处方信息: " + (notification.getPrescriptionInfo() != null ? "已获取" : "未获取"));
            System.out.println("患者信息: " + (notification.getPatientInfo() != null ? "已获取" : "未获取"));
            System.out.println("设备信息: " + (notification.getDeviceInfo() != null ? "已获取" : "未获取"));
            System.out.println("执行人信息: " + (notification.getExecutorInfo() != null ? "已获取" : "未获取"));
            System.out.println("医院信息: " + (notification.getHospitalInfo() != null ? "已获取" : "未获取"));
            
            // 通过WebSocket推送通知
            System.out.println("=== 开始WebSocket广播 ===");
            PrescriptionExecutionWebSocketServer.broadcastPrescriptionExecutionCreated(notification);
            
            System.out.println("=== WebSocket广播完成 ===");
            System.out.println("处方执行记录创建通知已发送，执行记录ID: " + execution.getId());
        } catch (Exception e) {
            System.err.println("=== WebSocket通知发送失败 ===");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("执行记录ID: " + execution.getId());
            e.printStackTrace();
        }
    }
    
    /**
     * 构建通知数据
     * 注意：此方法需要 public，供 WebSocketServer 调用
     */
    public PrescriptionExecutionNotification buildNotification(PrescriptionExecution execution) {
        System.out.println("=== 开始构建通知数据 ===");
        
        PrescriptionExecutionNotification notification = new PrescriptionExecutionNotification();
        notification.setTimestamp(new java.util.Date());
        notification.setExecutionId(execution.getId());
        notification.setExecutionStatus(execution.getStatus());
        
        System.out.println("基础信息设置完成 - 执行ID: " + execution.getId() + ", 状态: " + execution.getStatus());
        
        // 获取处方信息
        if (execution.getPrescriptionId() != null) {
            System.out.println("正在查询处方信息，处方ID: " + execution.getPrescriptionId());
            cc.mrbird.febs.system.domain.Prescription prescription = prescriptionService.getById(execution.getPrescriptionId());
            if (prescription != null) {
                System.out.println("处方信息查询成功: " + prescription.getPresPartName());
                PrescriptionExecutionNotification.PrescriptionInfo prescriptionInfo = new PrescriptionExecutionNotification.PrescriptionInfo();
                prescriptionInfo.setId(prescription.getId());
                prescriptionInfo.setPresType(prescription.getPresType());
                prescriptionInfo.setStatus(prescription.getStatus());
                prescriptionInfo.setPresStrength(prescription.getPresStrength());
                prescriptionInfo.setPresFreq(prescription.getPresFreq() != null ? prescription.getPresFreq().toString() : null);
                prescriptionInfo.setLastTime(prescription.getLastTime() != null ? prescription.getLastTime().toString() : null);
                prescriptionInfo.setPauseTime(prescription.getPauseTime());
                prescriptionInfo.setRepeatCount(prescription.getRepeatCount());
                prescriptionInfo.setTotalCount(prescription.getTotalCount());
                prescriptionInfo.setTotalTime(prescription.getTotalTime());
                prescriptionInfo.setPresPartId(prescription.getPresPartId());
                prescriptionInfo.setPresPartName(prescription.getPresPartName());
                prescriptionInfo.setStandardPresId(prescription.getStandardPresId());
                prescriptionInfo.setStandardPresName(prescription.getStandardPresName());
                prescriptionInfo.setTbsType(prescription.getTbsType());
                prescriptionInfo.setInnerCount(prescription.getInnerCount());
                prescriptionInfo.setInterFreq(prescription.getInterFreq() != null ? prescription.getInterFreq().toString() : null);
                prescriptionInfo.setInterCount(prescription.getInterCount());
                prescriptionInfo.setPeriods(prescription.getPeriods());
                prescriptionInfo.setCreatedAt(prescription.getCreatedAt());
                notification.setPrescriptionInfo(prescriptionInfo);
            } else {
                System.out.println("处方信息查询失败，处方ID: " + execution.getPrescriptionId());
            }
        } else {
            System.out.println("处方ID为空，跳过处方信息查询");
        }
        
        // 获取患者信息
        if (execution.getPatientId() != null) {
            System.out.println("正在查询患者信息，患者ID: " + execution.getPatientId());
            cc.mrbird.febs.system.domain.Patient patient = patientService.getById(execution.getPatientId());
            if (patient != null) {
                System.out.println("患者信息查询成功: " + patient.getName());
                PrescriptionExecutionNotification.PatientInfo patientInfo = new PrescriptionExecutionNotification.PatientInfo();
                patientInfo.setId(patient.getId());
                patientInfo.setName(patient.getName());
                patientInfo.setIdCard(patient.getIdCard());
                patientInfo.setGender(patient.getGender());
                patientInfo.setBirthday(patient.getBirthday());
                patientInfo.setHisId(patient.getHisId());
                patientInfo.setCode(patient.getCode());
                notification.setPatientInfo(patientInfo);
            } else {
                System.out.println("患者信息查询失败，患者ID: " + execution.getPatientId());
            }
        } else {
            System.out.println("患者ID为空，跳过患者信息查询");
        }
        
        // 获取设备信息
        if (execution.getDeviceId() != null) {
            System.out.println("正在查询设备信息，设备ID: " + execution.getDeviceId());
            cc.mrbird.febs.system.domain.Device device = deviceService.getById(execution.getDeviceId());
            if (device != null) {
                System.out.println("设备信息查询成功: " + device.getSn());
                PrescriptionExecutionNotification.DeviceInfo deviceInfo = new PrescriptionExecutionNotification.DeviceInfo();
                deviceInfo.setDeviceId(device.getDeviceId());
                deviceInfo.setDeviceNo(device.getDeviceNo());        // 设置设备编号
                deviceInfo.setDeviceType(device.getDeviceType());
                deviceInfo.setSn(device.getSn());
                deviceInfo.setStatus(device.getStatus());
                deviceInfo.setLastHeartbeat(device.getLastHeartbeat());
                notification.setDeviceInfo(deviceInfo);
                
                System.out.println("设备编号已设置: " + device.getDeviceNo());
            } else {
                System.out.println("设备信息查询失败，设备ID: " + execution.getDeviceId());
            }
        } else {
            System.out.println("设备ID为空，跳过设备信息查询");
        }
        
        // 获取执行人信息
        if (execution.getExecutorId() != null) {
            System.out.println("正在查询执行人信息，执行人ID: " + execution.getExecutorId());
            cc.mrbird.febs.system.domain.User user = userService.getById(execution.getExecutorId());
            if (user != null) {
                System.out.println("执行人信息查询成功: " + user.getUsername());
                PrescriptionExecutionNotification.UserInfo userInfo = new PrescriptionExecutionNotification.UserInfo();
                userInfo.setUserId(user.getUserId());
                userInfo.setUsername(user.getUsername());
                userInfo.setEmail(user.getEmail());
                userInfo.setMobile(user.getMobile());
                notification.setExecutorInfo(userInfo);
            } else {
                System.out.println("执行人信息查询失败，执行人ID: " + execution.getExecutorId());
            }
        } else {
            System.out.println("执行人ID为空，跳过执行人信息查询");
        }
        
        // 获取医院信息
        if (execution.getHospitalId() != null) {
            System.out.println("正在查询医院信息，医院ID: " + execution.getHospitalId());
            cc.mrbird.febs.system.domain.Hospital hospital = hospitalService.getById(execution.getHospitalId());
            if (hospital != null) {
                System.out.println("医院信息查询成功: " + hospital.getName());
                PrescriptionExecutionNotification.HospitalInfo hospitalInfo = new PrescriptionExecutionNotification.HospitalInfo();
                hospitalInfo.setHospitalId(hospital.getHospitalId());
                hospitalInfo.setName(hospital.getName());
                hospitalInfo.setAddress(hospital.getAddress());
                hospitalInfo.setContact(hospital.getContact());
                notification.setHospitalInfo(hospitalInfo);
            } else {
                System.out.println("医院信息查询失败，医院ID: " + execution.getHospitalId());
            }
        } else {
            System.out.println("医院ID为空，跳过医院信息查询");
        }
        
        System.out.println("=== 通知数据构建完成 ===");
        return notification;
    }
} 