package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.service.RecordSyncService;
import cc.mrbird.febs.system.domain.TreatmentRecord;
import cc.mrbird.febs.system.domain.MepRecord;
import cc.mrbird.febs.system.domain.response.SyncResponse;
import cc.mrbird.febs.system.domain.response.TreatmentRecordDTO;
import cc.mrbird.febs.system.domain.response.MepRecordDTO;
import cc.mrbird.febs.system.domain.vo.PrescriptionRecordVO;
import cc.mrbird.febs.system.domain.vo.TbsPrescriptionVO;
import cc.mrbird.febs.system.domain.vo.MepDataVO;
import cc.mrbird.febs.system.dao.TreatmentRecordMapper;
import cc.mrbird.febs.system.dao.MepRecordMapper;
import cc.mrbird.febs.system.dao.PrescriptionRecordMapper;
import cc.mrbird.febs.system.dao.TbsPrescriptionMapper;
import cc.mrbird.febs.system.dao.MepDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记录同步服务实现类
 */
@Slf4j
@Service
public class RecordSyncServiceImpl implements RecordSyncService {
    
    @Autowired
    private TreatmentRecordMapper treatmentRecordMapper;
    
    @Autowired
    private MepRecordMapper mepRecordMapper;
    
    @Autowired
    private PrescriptionRecordMapper prescriptionRecordMapper;
    
    @Autowired
    private TbsPrescriptionMapper tbsPrescriptionMapper;
    
    @Autowired
    private MepDataMapper mepDataMapper;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public SyncResponse syncPatientRecords(String patientIdentifier, String syncType, Integer requestDeviceNo) {
        try {
            log.info("开始同步患者记录，patientIdentifier: {}, syncType: {}, requestDeviceNo: {}", 
                     patientIdentifier, syncType, requestDeviceNo);
            
            SyncResponse.SyncData syncData = new SyncResponse.SyncData();
            syncData.setPatientIdentifier(patientIdentifier);
            
            // 根据syncType查询不同类型的记录
            List<TreatmentRecordDTO> treatmentRecords = new ArrayList<>();
            List<MepRecordDTO> mepRecords = new ArrayList<>();
            
            if ("ALL".equals(syncType) || "TREATMENT".equals(syncType)) {
                // 查询治疗记录（不过滤设备，服务器返回所有数据）
                treatmentRecords = syncTreatmentRecords(patientIdentifier);
            }
            
            if ("ALL".equals(syncType) || "MEP".equals(syncType)) {
                // 查询MEP记录（不过滤设备，服务器返回所有数据）
                mepRecords = syncMepRecords(patientIdentifier);
            }
            
            syncData.setTreatmentRecords(treatmentRecords);
            syncData.setMepRecords(mepRecords);
            syncData.setTotalTreatmentCount(treatmentRecords.size());
            syncData.setTotalMepCount(mepRecords.size());
            
            log.info("同步完成，治疗记录: {} 条，MEP记录: {} 条", treatmentRecords.size(), mepRecords.size());
            
            return SyncResponse.success(syncData);
            
        } catch (Exception e) {
            log.error("同步患者记录失败", e);
            return SyncResponse.error("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步治疗记录
     */
    private List<TreatmentRecordDTO> syncTreatmentRecords(String patientIdentifier) {
        List<TreatmentRecord> records = treatmentRecordMapper.selectByPatientIdentifier(patientIdentifier);
        
        return records.stream().map(record -> {
            TreatmentRecordDTO dto = new TreatmentRecordDTO();
            
            dto.setServerRecordId(record.getServerRecordId());
            dto.setSourceDeviceNo(record.getSourceDeviceNo());
            dto.setPatientName(record.getPatientName());
            dto.setPatientSex(record.getPatientSex());
            dto.setPatientHeight(record.getPatientHeight());
            dto.setPatientWeight(record.getPatientWeight());
            dto.setPatientAgeStr(record.getPatientAgeStr());
            dto.setPatientBirthday(record.getPatientBirthday());
            dto.setPatientRoom(record.getPatientRoom());
            dto.setPatientNo(record.getPatientNo());
            dto.setPatientBed(record.getPatientBed());
            dto.setPresDate(record.getPresDate() != null ? dateFormat.format(record.getPresDate()) : null);
            dto.setPresTime(record.getPresTime());
            dto.setDoctorName(record.getDoctorName());
            dto.setMepValue(record.getMepValue());
            dto.setMedicalRecordRemark(record.getMedicalRecordRemark());
            
            // 查询关联的处方记录
            List<PrescriptionRecordVO> prescriptions = prescriptionRecordMapper.selectByTreatmentRecordId(record.getId());
            dto.setPrescription_record(prescriptions.stream().map(this::convertPrescriptionRecord).collect(Collectors.toList()));
            
            // 查询关联的TBS处方记录
            List<TbsPrescriptionVO> tbsPrescriptions = tbsPrescriptionMapper.selectByTreatmentRecordId(record.getId());
            dto.setTbsPrescriptions(tbsPrescriptions.stream().map(this::convertTbsPrescription).collect(Collectors.toList()));
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * 同步MEP记录
     */
    private List<MepRecordDTO> syncMepRecords(String patientIdentifier) {
        LambdaQueryWrapper<MepRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MepRecord::getPatientIdentifier, patientIdentifier)
               .orderByDesc(MepRecord::getRecordTime);
        List<MepRecord> records = mepRecordMapper.selectList(wrapper);
        
        return records.stream().map(record -> {
            MepRecordDTO dto = new MepRecordDTO();
            
            dto.setServerRecordId(record.getServerRecordId());
            dto.setSourceDeviceNo(record.getSourceDeviceNo());
            dto.setPatientName(record.getPatientName());
            dto.setPatientSex(record.getPatientSex());
            dto.setPatientAgeStr(record.getPatientAgeStr());
            dto.setPatientBirthday(record.getPatientBirthday());
            dto.setRecordTime(record.getRecordTime() != null ? dateTimeFormat.format(record.getRecordTime()) : null);
            dto.setDType(record.getDType());
            
            // 查询关联的MEP数据
            List<MepDataVO> mepDataList = mepDataMapper.selectByMepRecordId(record.getId());
            dto.setMepDataList(mepDataList.stream().map(this::convertMepData).collect(Collectors.toList()));
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * 转换处方记录
     */
    private TreatmentRecordDTO.PrescriptionRecordDTO convertPrescriptionRecord(PrescriptionRecordVO record) {
        TreatmentRecordDTO.PrescriptionRecordDTO dto = new TreatmentRecordDTO.PrescriptionRecordDTO();
        dto.setPatientPresId(record.getPatientPresId());
        dto.setPresStrength(record.getPresStrength());
        dto.setPresFreq(record.getPresFreq() != null ? record.getPresFreq().doubleValue() : null);
        dto.setLastTime(record.getLastTime() != null ? record.getLastTime().doubleValue() : null);
        dto.setPauseTime(record.getPauseTime());
        dto.setRepeatCount(record.getRepeatCount());
        dto.setTotalCount(record.getTotalCount());
        dto.setTotalTimeStr(record.getTotalTimeStr());
        dto.setPresPart(record.getPresPart());
        dto.setStandardPresName(record.getStandardPresName());
        dto.setPresDate(record.getPresDate() != null ? record.getPresDate().toString() : null);
        dto.setPresTime(record.getPresTime());
        dto.setPeriods(record.getPeriods());
        return dto;
    }
    
    /**
     * 转换TBS处方记录
     */
    private TreatmentRecordDTO.TbsPrescriptionDTO convertTbsPrescription(TbsPrescriptionVO record) {
        TreatmentRecordDTO.TbsPrescriptionDTO dto = new TreatmentRecordDTO.TbsPrescriptionDTO();
        dto.setPatientPresTBSId(record.getPatientPresTbsId());
        dto.setPresStrength(record.getPresStrength());
        dto.setInnerFreq(record.getInnerFreq() != null ? record.getInnerFreq().doubleValue() : null);
        dto.setInnerCount(record.getInnerCount());
        dto.setInterFreq(record.getInterFreq() != null ? record.getInterFreq().doubleValue() : null);
        dto.setInterCount(record.getInterCount());
        dto.setPauseTime(record.getPauseTime());
        dto.setRepeatCount(record.getRepeatCount());
        dto.setTotalCount(record.getTotalCount());
        dto.setTotalTimeStr(record.getTotalTimeStr());
        dto.setPresPart(record.getPresPart());
        dto.setTbsType(record.getTbsType());
        dto.setPresDate(record.getPresDate() != null ? record.getPresDate().toString() : null);
        dto.setPresTime(record.getPresTime());
        dto.setPeriods(record.getPeriods());
        return dto;
    }
    
    /**
     * 转换MEP数据
     */
    private MepRecordDTO.MepDataItemDTO convertMepData(MepDataVO data) {
        MepRecordDTO.MepDataItemDTO dto = new MepRecordDTO.MepDataItemDTO();
        dto.setMt(data.getMt());
        dto.setCh(data.getCh());
        dto.setMaxValue(data.getMaxValue() != null ? data.getMaxValue().doubleValue() : null);
        dto.setMaxTime(data.getMaxTime() != null ? data.getMaxTime().doubleValue() : null);
        dto.setMinValue(data.getMinValue() != null ? data.getMinValue().doubleValue() : null);
        dto.setMinTime(data.getMinTime() != null ? data.getMinTime().doubleValue() : null);
        dto.setAmplitude(data.getAmplitude() != null ? data.getAmplitude().doubleValue() : null);
        dto.setPart(data.getPart());
        dto.setRecordPart(data.getRecordPart());
        return dto;
    }
}

