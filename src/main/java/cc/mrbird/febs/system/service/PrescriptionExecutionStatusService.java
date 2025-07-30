package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.PrescriptionExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;

/**
 * 处方执行记录状态更新服务
 */
@Service
public class PrescriptionExecutionStatusService {

    @Autowired
    private PrescriptionExecutionService prescriptionExecutionService;

    /**
     * 更新处方执行记录状态
     */
    public boolean updateExecutionStatus(Long executionId, String status, String clientInfo, Date receivedTime) {
        try {
            System.out.println("=== 开始更新处方执行记录状态 ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("新状态: " + status);
            System.out.println("客户端信息: " + clientInfo);
            System.out.println("接收时间: " + receivedTime);

            // 查询现有的执行记录
            PrescriptionExecution execution = prescriptionExecutionService.getById(executionId);
            if (execution == null) {
                System.err.println("处方执行记录不存在，ID: " + executionId);
                return false;
            }

            // 更新状态
            execution.setStatus(parseStatus(status));
            execution.setUpdatedAt(new Date());
            
            // 可以添加额外的字段来存储客户端信息
            // 如果有扩展字段，可以在这里设置
            // execution.setExtendField1(clientInfo);
            // execution.setExtendField2(receivedTime.toString());

            // 保存更新
            boolean success = prescriptionExecutionService.updateById(execution);
            
            if (success) {
                System.out.println("=== 处方执行记录状态更新成功 ===");
                System.out.println("执行记录ID: " + executionId);
                System.out.println("更新后状态: " + execution.getStatus());
            } else {
                System.err.println("=== 处方执行记录状态更新失败 ===");
            }

            return success;

        } catch (Exception e) {
            System.err.println("更新处方执行记录状态异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 更新处方执行记录状态（包含进度信息）
     */
    public boolean updateExecutionStatusWithProgress(Long executionId, String status, Long deviceId, Integer progress, String message, Date updateTime) {
        try {
            System.out.println("=== 开始更新处方执行记录状态（含进度） ===");
            System.out.println("执行记录ID: " + executionId);
            System.out.println("设备ID: " + deviceId);
            System.out.println("新状态: " + status);
            System.out.println("进度: " + progress);
            System.out.println("消息: " + message);
            System.out.println("更新时间: " + updateTime);

            // 查询现有的执行记录
            PrescriptionExecution execution = prescriptionExecutionService.getById(executionId);
            if (execution == null) {
                System.err.println("处方执行记录不存在，ID: " + executionId);
                return false;
            }

            // 更新状态
            execution.setStatus(parseStatus(status));
            execution.setUpdatedAt(updateTime);
            
            // 更新进度（如果有progress字段）
            if (progress != null) {
                execution.setProgress(progress.toString());
            }
            
            // 可以添加额外的字段来存储设备ID和消息
            // 如果有扩展字段，可以在这里设置
            // execution.setExtendField1(deviceId != null ? deviceId.toString() : null);
            // execution.setExtendField2(message);

            // 保存更新
            boolean success = prescriptionExecutionService.updateById(execution);
            
            if (success) {
                System.out.println("=== 处方执行记录状态更新成功（含进度） ===");
                System.out.println("执行记录ID: " + executionId);
                System.out.println("更新后状态: " + execution.getStatus());
                System.out.println("更新后进度: " + execution.getProgress());
            } else {
                System.err.println("=== 处方执行记录状态更新失败（含进度） ===");
            }

            return success;

        } catch (Exception e) {
            System.err.println("更新处方执行记录状态异常（含进度）: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 解析状态字符串为整数
     * 状态定义：0-待下发/1-已下发/2-执行中/3-完成/4-异常
     */
    private Integer parseStatus(String status) {
        try {
            switch (status.toUpperCase()) {
                case "PENDING":
                case "PENDING_DISPATCH":
                    return 0; // 待下发
                case "DISPATCHED":
                case "RECEIVED":
                case "RECEIVED_SUCCESS":
                    return 1; // 已下发
                case "EXECUTING":
                case "PROCESSING":
                    return 2; // 执行中
                case "COMPLETED":
                case "FINISHED":
                    return 3; // 完成
                case "FAILED":
                case "ERROR":
                case "EXCEPTION":
                    return 4; // 异常
                default:
                    return 1; // 默认已下发
            }
        } catch (Exception e) {
            System.err.println("状态解析失败: " + status);
            return 1; // 默认已下发
        }
    }
} 