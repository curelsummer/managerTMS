package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.PrescriptionExecution;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PrescriptionExecutionService extends IService<PrescriptionExecution> {
    
    /**
     * 分页查询处方执行记录
     */
    IPage<PrescriptionExecution> findPrescriptionExecutions(IPage<PrescriptionExecution> page, PrescriptionExecution execution, Long userId);
    
    /**
     * 根据患者ID查询处方执行记录
     */
    List<PrescriptionExecution> getByPatientId(Long patientId);
    
    /**
     * 根据处方ID查询执行记录
     */
    List<PrescriptionExecution> getByPrescriptionId(Long prescriptionId);
    
    /**
     * 根据执行人ID查询执行记录
     */
    List<PrescriptionExecution> getByExecutorId(Long executorId);
    
    /**
     * 根据状态查询执行记录
     */
    List<PrescriptionExecution> getByStatus(Integer status);
} 