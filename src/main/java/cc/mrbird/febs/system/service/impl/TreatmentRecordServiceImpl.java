package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.service.TreatmentRecordService;
import cc.mrbird.febs.system.domain.*;
import cc.mrbird.febs.system.dao.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import cc.mrbird.febs.common.domain.QueryRequest;
import cc.mrbird.febs.system.domain.vo.TreatmentRecordVO;
import cc.mrbird.febs.system.domain.vo.PrescriptionRecordVO;
import cc.mrbird.febs.system.domain.vo.TbsPrescriptionVO;
import cc.mrbird.febs.system.domain.vo.MepRecordVO;
import cc.mrbird.febs.system.domain.vo.MepDataVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 治疗记录服务实现类
 * 
 * @author MrBird
 */
@Slf4j
@Service
public class TreatmentRecordServiceImpl implements TreatmentRecordService {
    
    @Autowired
    private TreatmentRecordMapper treatmentRecordMapper;
    
    @Autowired
    private PrescriptionRecordMapper prescriptionRecordMapper;
    
    @Autowired
    private TbsPrescriptionMapper tbsPrescriptionMapper;
    
    @Autowired
    private MepRecordMapper mepRecordMapper;
    
    @Autowired
    private MepDataMapper mepDataMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTreatmentRecord(JsonNode jsonNode) {
        try {
            log.info("开始保存治疗记录到数据库");
            
            // 1. 提取基本信息
            Integer deviceNo = jsonNode.get("deviceNo").asInt();
            Long localMedicalRecordId = jsonNode.get("localMedicalRecordId").asLong();
            String patientName = jsonNode.get("patientName").asText();
            String patientBirthday = jsonNode.has("patientBirthday") ? jsonNode.get("patientBirthday").asText() : null;
            
            // 2. 生成患者唯一标识
            String patientIdentifier = cc.mrbird.febs.system.utils.ServerRecordIdGenerator.generatePatientIdentifier(patientName, patientBirthday);
            
            // 3. 生成服务器记录ID
            String serverRecordId = cc.mrbird.febs.system.utils.ServerRecordIdGenerator.generateTreatmentRecordId(deviceNo, localMedicalRecordId);
            
            // 4. 检查是否已存在相同的服务器记录ID（防止重复上传）
            TreatmentRecord existingRecord = treatmentRecordMapper.selectByServerRecordId(serverRecordId);
            if (existingRecord != null) {
                log.warn("治疗记录已存在，serverRecordId: {}", serverRecordId);
                return true; // 已存在也算成功
            }
            
            // 4.1 检查是否已存在相同 patientIdentifier + deviceNo 的治疗记录（避免重复创建）
            // 如果已存在，复用该记录，但仍需要保存处方数据
            LambdaQueryWrapper<TreatmentRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TreatmentRecord::getPatientIdentifier, patientIdentifier)
                   .eq(TreatmentRecord::getDeviceNo, deviceNo)
                   .orderByDesc(TreatmentRecord::getCreateTime)
                   .last("LIMIT 1");
            existingRecord = treatmentRecordMapper.selectOne(wrapper);
            
            Long treatmentRecordId;
            if (existingRecord != null) {
                log.warn("治疗记录已存在（patientIdentifier相同），复用现有记录: treatmentRecordId={}, patientIdentifier={}, deviceNo={}, serverRecordId={}", 
                        existingRecord.getId(), patientIdentifier, deviceNo, existingRecord.getMessageId());
                treatmentRecordId = existingRecord.getId();
            } else {
                // 5. 保存治疗记录主表
                TreatmentRecord treatmentRecord = buildTreatmentRecord(jsonNode, patientIdentifier, serverRecordId);
                treatmentRecordMapper.insert(treatmentRecord);
                log.info("治疗记录主表保存成功，ID: {}, serverRecordId: {}", treatmentRecord.getId(), serverRecordId);
                treatmentRecordId = treatmentRecord.getId();
            }
            
            // 6. 保存标准处方记录（无论治疗记录是新创建还是复用，都需要保存处方数据）
            if (jsonNode.has("prescriptions") && jsonNode.get("prescriptions").isArray()) {
                savePrescriptionRecords(jsonNode.get("prescriptions"), treatmentRecordId);
            } else if (jsonNode.has("prescription_record") && jsonNode.get("prescription_record").isArray()) {
                // 兼容旧字段名
                savePrescriptionRecords(jsonNode.get("prescription_record"), treatmentRecordId);
            }
            
            // 7. 保存TBS处方记录（无论治疗记录是新创建还是复用，都需要保存处方数据）
            if (jsonNode.has("tbsPrescriptions") && jsonNode.get("tbsPrescriptions").isArray()) {
                saveTbsPrescriptions(jsonNode.get("tbsPrescriptions"), treatmentRecordId);
            }
            
            log.info("治疗记录保存完成，serverRecordId: {}", serverRecordId);
            return true;
            
        } catch (Exception e) {
            log.error("保存治疗记录失败", e);
            throw new RuntimeException("保存治疗记录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public IPage<TreatmentRecordVO> findTreatmentRecordPage(QueryRequest request, Object query) {
        Page<TreatmentRecordVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        return treatmentRecordMapper.selectTreatmentRecordPage(page, query);
    }

    @Override
    public TreatmentRecordVO findTreatmentRecordById(Long id) {
        return treatmentRecordMapper.selectTreatmentRecordById(id);
    }

    @Override
    public List<TreatmentRecordVO> findTreatmentRecordList(Object query) {
        return treatmentRecordMapper.selectTreatmentRecordList(query);
    }

    @Override
    public boolean deleteTreatmentRecord(Long id) {
        try {
            int result = treatmentRecordMapper.deleteById(id);
            return result > 0;
        } catch (Exception e) {
            log.error("删除治疗记录失败，ID: {}", id, e);
            return false;
        }
    }

    @Override
    public boolean deleteTreatmentRecords(String[] ids) {
        try {
            for (String id : ids) {
                treatmentRecordMapper.deleteById(Long.valueOf(id));
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除治疗记录失败", e);
            return false;
        }
    }
    
    @Override
    public TreatmentRecordVO findTreatmentRecordDetailById(Long id) {
        // 查询主记录
        TreatmentRecordVO treatmentRecord = treatmentRecordMapper.selectTreatmentRecordById(id);
        if (treatmentRecord == null) {
            return null;
        }
        
        // 查询标准处方记录
        List<PrescriptionRecordVO> prescriptionRecords = prescriptionRecordMapper.selectByTreatmentRecordId(id);
        treatmentRecord.setPrescriptionRecords(prescriptionRecords);
        
        // 查询TBS处方记录
        List<TbsPrescriptionVO> tbsPrescriptions = tbsPrescriptionMapper.selectByTreatmentRecordId(id);
        treatmentRecord.setTbsPrescriptions(tbsPrescriptions);
        
        // 查询MEP记录及数据
        List<MepRecordVO> mepRecords = mepRecordMapper.selectByTreatmentRecordId(id);
        for (MepRecordVO mepRecord : mepRecords) {
            List<MepDataVO> mepDataList = mepDataMapper.selectByMepRecordId(mepRecord.getId());
            mepRecord.setMepDataList(mepDataList);
        }
        treatmentRecord.setMepRecords(mepRecords);
        
        return treatmentRecord;
    }
    
    /**
     * 构建治疗记录主表数据
     */
    private TreatmentRecord buildTreatmentRecord(JsonNode jsonNode, String patientIdentifier, String serverRecordId) throws Exception {
        TreatmentRecord record = new TreatmentRecord();
        
        // 服务器生成的字段
        record.setPatientIdentifier(patientIdentifier);
        record.setServerRecordId(serverRecordId);
        
        // 设备上报的字段
        record.setLocalMedicalRecordId(jsonNode.get("localMedicalRecordId").asLong());
        // 统一处理 deviceId：支持数字和字符串两种类型，如果不存在则使用 deviceNo
        Long deviceId = 0L;
        if (jsonNode.has("deviceId") && !jsonNode.get("deviceId").isNull()) {
            try {
                if (jsonNode.get("deviceId").isTextual()) {
                    // 字符串类型：提取数字
                    deviceId = Long.parseLong(jsonNode.get("deviceId").asText().replaceAll("[^0-9]", ""));
                } else if (jsonNode.get("deviceId").isNumber()) {
                    // 数字类型：直接转换
                    deviceId = jsonNode.get("deviceId").asLong();
                }
            } catch (Exception e) {
                log.warn("解析deviceId失败，使用默认值0: {}", e.getMessage());
                deviceId = 0L;
            }
        } else {
            // 如果deviceId不存在，使用deviceNo作为默认值
            deviceId = Long.valueOf(jsonNode.get("deviceNo").asInt());
        }
        record.setDeviceId(deviceId);
        record.setDeviceNo(jsonNode.get("deviceNo").asInt());
        record.setSourceDeviceNo(jsonNode.get("deviceNo").asInt()); // 来源设备号等于上报设备号
        
        // 患者信息
        record.setPatientId(0L); // 设备端可能没有服务器的患者ID，设为0
        record.setPatientName(jsonNode.get("patientName").asText());
        record.setPatientSex(getStringValue(jsonNode, "patientSex"));
        record.setPatientAgeStr(getStringValue(jsonNode, "patientAgeStr"));
        record.setPatientBirthday(getStringValue(jsonNode, "patientBirthday"));
        record.setPatientHeight(getIntValue(jsonNode, "patientHeight"));
        record.setPatientWeight(getIntValue(jsonNode, "patientWeight"));
        record.setPatientRoom(getStringValue(jsonNode, "patientRoom"));
        record.setPatientNo(getStringValue(jsonNode, "patientNo"));
        record.setPatientBed(getStringValue(jsonNode, "patientBed"));
        
        // 处方信息
        record.setPresDate(dateFormat.parse(jsonNode.get("presDate").asText()));
        record.setPresTime(jsonNode.get("presTime").asText());
        record.setDoctorName(getStringValue(jsonNode, "doctorName"));
        record.setMepValue(getIntValue(jsonNode, "mepValue"));
        record.setMedicalRecordRemark(getStringValue(jsonNode, "medicalRecordRemark"));
        
        // 其他字段
        record.setMedicalRecordId(0L); // 设备端可能没有服务器的病历ID
        record.setMessageId(serverRecordId); // 使用serverRecordId作为messageId
        record.setTimestamp(jsonNode.has("timestamp") ? jsonNode.get("timestamp").asLong() : System.currentTimeMillis());
        record.setMessageType(getStringValue(jsonNode, "messageType"));
        
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        
        return record;
    }
    
    /**
     * 保存标准处方记录
     */
    private void savePrescriptionRecords(JsonNode prescriptionArray, Long treatmentRecordId) throws Exception {
        for (JsonNode prescription : prescriptionArray) {
            PrescriptionRecord record = new PrescriptionRecord();
            
            record.setTreatmentRecordId(treatmentRecordId);
            record.setPatientPresId(prescription.get("patientPresId").asLong());
            record.setPresStrength(prescription.get("presStrength").asInt());
            record.setPresFreq(new BigDecimal(prescription.get("presFreq").asText()));
            record.setLastTime(new BigDecimal(prescription.get("lastTime").asText()));
            record.setPauseTime(prescription.get("pauseTime").asInt());
            record.setRepeatCount(prescription.get("repeatCount").asInt());
            record.setTotalCount(prescription.get("totalCount").asInt());
            record.setTotalTimeStr(prescription.get("totalTimeStr").asText());
            record.setPresPart(prescription.get("presPart").asText());
            record.setStandardPresName(getStringValue(prescription, "standardPresName"));
            record.setPeriods(prescription.get("periods").asInt());
            record.setPresDate(dateFormat.parse(prescription.get("presDate").asText()));
            record.setPresTime(prescription.get("presTime").asText());
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            
            prescriptionRecordMapper.insert(record);
            log.info("标准处方记录保存成功，ID: {}", record.getId());
        }
    }
    
    /**
     * 保存TBS处方记录
     */
    private void saveTbsPrescriptions(JsonNode tbsArray, Long treatmentRecordId) throws Exception {
        for (JsonNode tbs : tbsArray) {
            TbsPrescription record = new TbsPrescription();
            
            record.setTreatmentRecordId(treatmentRecordId);
            record.setPatientPresTbsId(tbs.get("patientPresTBSId").asLong());
            record.setPresStrength(tbs.get("presStrength").asInt());
            record.setInnerFreq(new BigDecimal(tbs.get("innerFreq").asText()));
            record.setInnerCount(tbs.get("innerCount").asInt());
            record.setInterFreq(new BigDecimal(tbs.get("interFreq").asText()));
            record.setInterCount(tbs.get("interCount").asInt());
            record.setPauseTime(tbs.get("pauseTime").asInt());
            record.setRepeatCount(tbs.get("repeatCount").asInt());
            record.setTotalCount(tbs.get("totalCount").asInt());
            record.setTotalTimeStr(tbs.get("totalTimeStr").asText());
            record.setPresPart(tbs.get("presPart").asText());
            record.setPeriods(tbs.get("periods").asInt());
            record.setTbsType(tbs.get("tbsType").asText());
            record.setPresDate(dateFormat.parse(tbs.get("presDate").asText()));
            record.setPresTime(tbs.get("presTime").asText());
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            
            tbsPrescriptionMapper.insert(record);
            log.info("TBS处方记录保存成功，ID: {}", record.getId());
        }
    }
    
    
    /**
     * 保存MEP数据
     */
    private void saveMepData(JsonNode mepDataArray, Long mepRecordId) throws Exception {
        for (JsonNode data : mepDataArray) {
            MepData mepData = new MepData();
            
            mepData.setMepRecordId(mepRecordId);
            mepData.setMt(data.get("mt").asInt());
            mepData.setCh(data.get("ch").asText());
            mepData.setMaxValue(new BigDecimal(data.get("maxValue").asText()));
            mepData.setMaxTime(new BigDecimal(data.get("maxTime").asText()));
            mepData.setMinValue(new BigDecimal(data.get("minValue").asText()));
            mepData.setMinTime(new BigDecimal(data.get("minTime").asText()));
            mepData.setAmplitude(new BigDecimal(data.get("amplitude").asText()));
            mepData.setPart(data.get("part").asText());
            mepData.setRecordPart(data.get("recordPart").asText());
            mepData.setCreateTime(new Date());
            mepData.setUpdateTime(new Date());
            
            mepDataMapper.insert(mepData);
            log.info("MEP数据保存成功，ID: {}", mepData.getId());
        }
    }
    
    /**
     * 获取字符串值，如果不存在返回null
     */
    private String getStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }
    
    /**
     * 获取整数值，如果不存在返回null
     */
    private Integer getIntValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asInt() : null;
    }
}
