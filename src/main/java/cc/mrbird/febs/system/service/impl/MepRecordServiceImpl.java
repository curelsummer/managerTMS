package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.service.MepRecordService;
import cc.mrbird.febs.system.domain.MepRecord;
import cc.mrbird.febs.system.domain.MepData;
import cc.mrbird.febs.system.domain.TreatmentRecord;
import cc.mrbird.febs.system.domain.Patient;
import cc.mrbird.febs.system.domain.vo.MepRecordVO;
import cc.mrbird.febs.system.domain.vo.MepDataVO;
import cc.mrbird.febs.system.dao.MepRecordMapper;
import cc.mrbird.febs.system.dao.MepDataMapper;
import cc.mrbird.febs.system.dao.TreatmentRecordMapper;
import cc.mrbird.febs.system.dao.PatientMapper;
import cc.mrbird.febs.system.utils.ServerRecordIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * MEP记录服务实现类
 */
@Slf4j
@Service
public class MepRecordServiceImpl implements MepRecordService {
    
    @Autowired
    private MepRecordMapper mepRecordMapper;
    
    @Autowired
    private MepDataMapper mepDataMapper;
    
    @Autowired
    private TreatmentRecordMapper treatmentRecordMapper;
    
    @Autowired
    private PatientMapper patientMapper;
    
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveMepRecord(JsonNode jsonNode) {
        try {
            log.info("开始保存MEP记录到数据库");
            
            // 1. 提取基本信息
            Integer deviceNo = jsonNode.get("deviceNo").asInt();
            Long localMepRecordId = jsonNode.get("localMepRecordId").asLong();
            String patientName = jsonNode.get("patientName").asText();
            String patientBirthday = jsonNode.has("patientBirthday") ? jsonNode.get("patientBirthday").asText() : null;
            
            // 2. 获取或生成患者唯一标识（优先使用下位机提供的）
            String patientIdentifier;
            if (jsonNode.has("patientIdentifier") && !jsonNode.get("patientIdentifier").isNull()) {
                patientIdentifier = jsonNode.get("patientIdentifier").asText();
                log.info("使用下位机提供的patientIdentifier: {}", patientIdentifier);
            } else {
                patientIdentifier = ServerRecordIdGenerator.generatePatientIdentifier(patientName, patientBirthday);
                log.info("生成patientIdentifier: {}", patientIdentifier);
            }
            
            // 3. 生成服务器记录ID
            String serverRecordId = ServerRecordIdGenerator.generateMepRecordId(deviceNo, localMepRecordId);
            
            // 4. 检查是否已存在相同的服务器记录ID（防止重复上传）
            MepRecord existingRecord = getByServerRecordId(serverRecordId);
            if (existingRecord != null) {
                log.warn("MEP记录已存在，serverRecordId: {}", serverRecordId);
                return serverRecordId; // 返回已存在的ID
            }
            
            // 5. 尝试查找或创建关联的治疗记录
            // 如果提供了 relatedMedicalRecordId，则尝试关联；如果找不到，则自动创建
            Long treatmentRecordId = findOrCreateTreatmentRecord(jsonNode, patientIdentifier, deviceNo);
            if (treatmentRecordId != null) {
                log.info("找到或创建了关联的治疗记录: treatmentRecordId={}", treatmentRecordId);
            } else {
                log.info("未找到关联的治疗记录，将独立保存MEP记录");
            }
            
            // 6. 构建MEP记录实体
            MepRecord mepRecord = buildMepRecord(jsonNode, patientIdentifier, serverRecordId, treatmentRecordId);
            
            // 7. 保存MEP记录主表
            mepRecordMapper.insert(mepRecord);
            if (treatmentRecordId != null) {
                log.info("MEP记录主表保存成功，ID: {}, serverRecordId: {}, treatmentRecordId: {}", 
                         mepRecord.getId(), serverRecordId, treatmentRecordId);
            } else {
                log.info("MEP记录主表保存成功（独立保存），ID: {}, serverRecordId: {}", 
                         mepRecord.getId(), serverRecordId);
            }
            
            // 8. 保存MEP数据
            if (jsonNode.has("mepDataList") && jsonNode.get("mepDataList").isArray()) {
                saveMepDataList(jsonNode.get("mepDataList"), mepRecord.getId());
                
                // 8.1 从mepData中计算MEP值并更新治疗主表和患者表
                if (treatmentRecordId != null) {
                    updateMepValueFromMepData(mepRecord.getId(), treatmentRecordId, mepRecord.getPatientIdentifier());
                }
            }
            
            log.info("MEP记录保存完成，serverRecordId: {}", serverRecordId);
            return serverRecordId;
            
        } catch (Exception e) {
            log.error("保存MEP记录失败", e);
            throw new RuntimeException("保存MEP记录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MepRecord> getByPatientIdentifier(String patientIdentifier) {
        LambdaQueryWrapper<MepRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MepRecord::getPatientIdentifier, patientIdentifier)
               .orderByDesc(MepRecord::getRecordTime);
        return mepRecordMapper.selectList(wrapper);
    }
    
    @Override
    public MepRecord getByServerRecordId(String serverRecordId) {
        LambdaQueryWrapper<MepRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MepRecord::getServerRecordId, serverRecordId);
        return mepRecordMapper.selectOne(wrapper);
    }
    
    @Override
    public List<MepRecordVO> getMepRecordVOByPatientIdentifier(String patientIdentifier) {
        List<MepRecordVO> mepRecords = mepRecordMapper.selectByPatientIdentifier(patientIdentifier);
        // 为每个MEP记录加载MEP数据
        for (MepRecordVO mepRecord : mepRecords) {
            List<MepDataVO> mepDataList = mepDataMapper.selectByMepRecordId(mepRecord.getId());
            mepRecord.setMepDataList(mepDataList);
        }
        return mepRecords;
    }
    
    @Override
    public MepRecordVO getMepRecordDetailById(Long id) {
        MepRecord mepRecord = mepRecordMapper.selectById(id);
        if (mepRecord == null) {
            return null;
        }
        
        // 转换为VO
        MepRecordVO vo = new MepRecordVO();
        vo.setId(mepRecord.getId());
        vo.setTreatmentRecordId(mepRecord.getTreatmentRecordId());
        vo.setPatientIdentifier(mepRecord.getPatientIdentifier());
        vo.setServerRecordId(mepRecord.getServerRecordId());
        vo.setLocalMepRecordId(mepRecord.getLocalMepRecordId());
        vo.setDeviceId(mepRecord.getDeviceId());
        vo.setDeviceNo(mepRecord.getDeviceNo());
        vo.setSourceDeviceNo(mepRecord.getSourceDeviceNo());
        vo.setPatientName(mepRecord.getPatientName());
        vo.setPatientSex(mepRecord.getPatientSex());
        vo.setPatientAgeStr(mepRecord.getPatientAgeStr());
        vo.setPatientBirthday(mepRecord.getPatientBirthday());
        vo.setRecordTime(mepRecord.getRecordTime());
        vo.setDType(mepRecord.getDType());
        vo.setTimestamp(mepRecord.getTimestamp());
        vo.setCreateTime(mepRecord.getCreateTime());
        vo.setUpdateTime(mepRecord.getUpdateTime());
        
        // 加载MEP数据
        List<MepDataVO> mepDataList = mepDataMapper.selectByMepRecordId(id);
        vo.setMepDataList(mepDataList);
        
        return vo;
    }
    
    /**
     * 查找或创建治疗记录ID
     * 优先顺序：
     * 1. 如果提供了 relatedMedicalRecordId，尝试通过它查找
     * 2. 如果提供了 treatmentServerRecordId，使用它查找（通过message_id字段）
     * 3. 如果提供了 patientIdentifier，使用患者唯一标识和设备号查找最新的治疗记录
     * 4. 如果都没有，尝试通过患者姓名和设备号查找最新的治疗记录（兼容旧数据）
     * 5. 如果都找不到，自动创建一个治疗记录（使用MEP记录中的患者信息）
     * 
     * 注意：如果找不到治疗记录，会自动创建一个，返回新创建的治疗记录ID
     */
    private Long findOrCreateTreatmentRecord(JsonNode jsonNode, String patientIdentifier, Integer deviceNo) {
        // 先尝试查找
        Long treatmentRecordId = findTreatmentRecordId(jsonNode, patientIdentifier, deviceNo);
        if (treatmentRecordId != null) {
            return treatmentRecordId;
        }
        
        // 如果找不到，自动创建治疗记录
        log.info("未找到关联的治疗记录，将自动创建一个治疗记录");
        return createTreatmentRecordFromMep(jsonNode, patientIdentifier, deviceNo);
    }
    
    /**
     * 查找治疗记录ID（不创建）
     */
    private Long findTreatmentRecordId(JsonNode jsonNode, String patientIdentifier, Integer deviceNo) {
        // 方式1：通过 relatedMedicalRecordId 查找（新字段，支持关联治疗记录）
        if (jsonNode.has("relatedMedicalRecordId") && !jsonNode.get("relatedMedicalRecordId").isNull()) {
            // relatedMedicalRecordId 可能是服务器记录ID（message_id）或本地病历ID
            String relatedMedicalRecordId = jsonNode.get("relatedMedicalRecordId").asText();
            
            // 先尝试作为服务器记录ID查找（通过message_id字段）
            TreatmentRecord treatmentRecord = treatmentRecordMapper.selectByServerRecordId(relatedMedicalRecordId);
            if (treatmentRecord != null) {
                log.info("通过relatedMedicalRecordId（作为serverRecordId）找到治疗记录: {}", treatmentRecord.getId());
                return treatmentRecord.getId();
            }
            
            log.warn("通过relatedMedicalRecordId未找到治疗记录: {}", relatedMedicalRecordId);
        }
        
        // 方式2：通过服务器记录ID查找（使用message_id字段，兼容旧字段名）
        if (jsonNode.has("treatmentServerRecordId") && !jsonNode.get("treatmentServerRecordId").isNull()) {
            String treatmentServerRecordId = jsonNode.get("treatmentServerRecordId").asText();
            TreatmentRecord treatmentRecord = treatmentRecordMapper.selectByServerRecordId(treatmentServerRecordId);
            if (treatmentRecord != null) {
                log.info("通过treatmentServerRecordId找到治疗记录: {}", treatmentRecord.getId());
                return treatmentRecord.getId();
            }
            log.warn("通过treatmentServerRecordId未找到治疗记录: {}", treatmentServerRecordId);
        }
        
        // 方式3：通过患者唯一标识和设备号查找最新的治疗记录
        if (patientIdentifier != null && !patientIdentifier.isEmpty()) {
            LambdaQueryWrapper<TreatmentRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TreatmentRecord::getPatientIdentifier, patientIdentifier)
                   .eq(TreatmentRecord::getDeviceNo, deviceNo)
                   .orderByDesc(TreatmentRecord::getCreateTime)
                   .last("LIMIT 1");
            TreatmentRecord treatmentRecord = treatmentRecordMapper.selectOne(wrapper);
            if (treatmentRecord != null) {
                log.info("通过患者唯一标识和设备号找到最新的治疗记录: patientIdentifier={}, deviceNo={}, treatmentRecordId={}", 
                        patientIdentifier, deviceNo, treatmentRecord.getId());
                return treatmentRecord.getId();
            }
            log.warn("通过患者唯一标识和设备号未找到治疗记录: patientIdentifier={}, deviceNo={}", 
                    patientIdentifier, deviceNo);
        }
        
        // 方式4：如果patientIdentifier为空，尝试通过患者姓名和设备号查找（兼容旧数据）
        String patientName = jsonNode.has("patientName") ? jsonNode.get("patientName").asText() : null;
        if (patientName != null && !patientName.isEmpty()) {
            LambdaQueryWrapper<TreatmentRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TreatmentRecord::getPatientName, patientName)
                   .eq(TreatmentRecord::getDeviceNo, deviceNo)
                   .orderByDesc(TreatmentRecord::getCreateTime)
                   .last("LIMIT 1");
            TreatmentRecord treatmentRecord = treatmentRecordMapper.selectOne(wrapper);
            if (treatmentRecord != null) {
                log.info("通过患者姓名和设备号找到最新的治疗记录: patientName={}, deviceNo={}, treatmentRecordId={}", 
                        patientName, deviceNo, treatmentRecord.getId());
                return treatmentRecord.getId();
            }
            log.warn("通过患者姓名和设备号未找到治疗记录: patientName={}, deviceNo={}", patientName, deviceNo);
        }
        
        log.warn("未找到对应的治疗记录 - patientIdentifier: {}, patientName: {}, deviceNo: {}", 
                patientIdentifier, patientName, deviceNo);
        return null;
    }
    
    /**
     * 构建MEP记录实体
     */
    private MepRecord buildMepRecord(JsonNode jsonNode, String patientIdentifier, String serverRecordId, Long treatmentRecordId) throws Exception {
        MepRecord record = new MepRecord();
        
        // 服务器生成的字段
        record.setPatientIdentifier(patientIdentifier);
        record.setServerRecordId(serverRecordId);
        record.setTreatmentRecordId(treatmentRecordId); // 可选，如果提供则关联治疗记录
        
        // 设备上报的字段
        record.setLocalMepRecordId(jsonNode.get("localMepRecordId").asLong());
        record.setDeviceId(jsonNode.get("deviceId").asText());
        record.setDeviceNo(jsonNode.get("deviceNo").asInt());
        record.setSourceDeviceNo(jsonNode.get("deviceNo").asInt()); // 来源设备号等于上报设备号
        
        record.setPatientName(jsonNode.get("patientName").asText());
        record.setPatientSex(getStringValue(jsonNode, "patientSex"));
        record.setPatientAgeStr(getStringValue(jsonNode, "patientAgeStr"));
        record.setPatientBirthday(getStringValue(jsonNode, "patientBirthday"));
        
        record.setRecordTime(dateTimeFormat.parse(jsonNode.get("recordTime").asText()));
        record.setDType(jsonNode.has("dType") ? jsonNode.get("dType").asInt() : 0);
        record.setTimestamp(jsonNode.has("timestamp") ? jsonNode.get("timestamp").asLong() : System.currentTimeMillis());
        
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        
        return record;
    }
    
    /**
     * 保存MEP数据列表
     */
    private void saveMepDataList(JsonNode mepDataArray, Long mepRecordId) throws Exception {
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
     * 从MEP记录自动创建治疗记录
     * 使用MEP记录中的患者信息创建一个基础的治疗记录
     * 
     * 冲突处理：
     * 1. 先通过 patientIdentifier + deviceNo 查找是否已存在任何治疗记录（包括正常上传和自动创建的）
     * 2. 如果存在，直接返回（避免重复创建）
     * 3. 如果不存在，创建一个新的，使用时间戳作为虚拟的 localMedicalRecordId
     */
    private Long createTreatmentRecordFromMep(JsonNode jsonNode, String patientIdentifier, Integer deviceNo) {
        try {
            // 1. 先通过 patientIdentifier + deviceNo 查找是否已存在任何治疗记录（不限制messageType）
            // 这样可以避免重复创建，即使之前已经通过正常方式上传了治疗记录
            LambdaQueryWrapper<TreatmentRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TreatmentRecord::getPatientIdentifier, patientIdentifier)
                   .eq(TreatmentRecord::getDeviceNo, deviceNo)
                   .orderByDesc(TreatmentRecord::getCreateTime)
                   .last("LIMIT 1");
            TreatmentRecord existingRecord = treatmentRecordMapper.selectOne(wrapper);
            if (existingRecord != null) {
                log.info("找到已存在的治疗记录（patientIdentifier相同），treatmentRecordId: {}, patientIdentifier: {}, messageType: {}", 
                        existingRecord.getId(), patientIdentifier, existingRecord.getMessageType());
                return existingRecord.getId();
            }
            
            // 2. 生成虚拟的localMedicalRecordId（使用时间戳确保唯一性）
            Long virtualLocalMedicalRecordId = System.currentTimeMillis();
            
            // 3. 生成服务器记录ID
            String serverRecordId = ServerRecordIdGenerator.generateTreatmentRecordId(deviceNo, virtualLocalMedicalRecordId);
            
            // 4. 再次检查是否已存在（防止并发创建）
            existingRecord = treatmentRecordMapper.selectByServerRecordId(serverRecordId);
            if (existingRecord != null) {
                log.info("治疗记录已存在（可能是并发创建的），serverRecordId: {}, treatmentRecordId: {}", 
                        serverRecordId, existingRecord.getId());
                return existingRecord.getId();
            }
            
            // 4. 从MEP记录中提取患者信息
            String patientName = jsonNode.get("patientName").asText();
            String patientSex = getStringValue(jsonNode, "patientSex");
            String patientAgeStr = getStringValue(jsonNode, "patientAgeStr");
            
            // 5. 从recordTime提取presDate和presTime
            Date recordTime = dateTimeFormat.parse(jsonNode.get("recordTime").asText());
            Date presDate = recordTime; // 使用MEP记录时间作为处方日期
            String presTime = new SimpleDateFormat("HH:mm:ss").format(recordTime);
            
            // 6. 构建治疗记录
            TreatmentRecord treatmentRecord = new TreatmentRecord();
            
            // 服务器生成的字段
            treatmentRecord.setPatientIdentifier(patientIdentifier);
            treatmentRecord.setMessageId(serverRecordId); // 使用serverRecordId作为messageId
            
            // 设备信息
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
                    log.warn("解析deviceId失败，使用deviceNo: {}", e.getMessage());
                    deviceId = Long.valueOf(deviceNo);
                }
            } else {
                // 如果deviceId不存在，使用deviceNo作为默认值
                deviceId = Long.valueOf(deviceNo);
            }
            treatmentRecord.setDeviceId(deviceId);
            treatmentRecord.setDeviceNo(deviceNo);
            
            // 患者信息
            String patientBirthday = getStringValue(jsonNode, "patientBirthday");
            Long patientId = findPatientIdByNameAndBirthday(patientName, patientBirthday);
            treatmentRecord.setPatientId(patientId);
            treatmentRecord.setPatientName(patientName);
            treatmentRecord.setPatientSex(patientSex);
            treatmentRecord.setPatientAgeStr(patientAgeStr);
            treatmentRecord.setPatientRoom(getStringValue(jsonNode, "patientRoom"));
            treatmentRecord.setPatientNo(getStringValue(jsonNode, "patientNo"));
            treatmentRecord.setPatientBed(getStringValue(jsonNode, "patientBed"));
            
            // 处方信息（使用MEP记录时间）
            treatmentRecord.setPresDate(presDate);
            treatmentRecord.setPresTime(presTime);
            treatmentRecord.setDoctorName(null); // MEP记录中没有医生信息
            treatmentRecord.setMepValue(null);
            treatmentRecord.setMedicalRecordRemark("自动创建（来自MEP记录）");
            
            // 其他字段
            treatmentRecord.setMedicalRecordId(0L);
            treatmentRecord.setTimestamp(jsonNode.has("timestamp") ? jsonNode.get("timestamp").asLong() : System.currentTimeMillis());
            treatmentRecord.setMessageType("AUTO_CREATED_FROM_MEP");
            treatmentRecord.setCreateTime(new Date());
            treatmentRecord.setUpdateTime(new Date());
            
            // 7. 保存治疗记录
            treatmentRecordMapper.insert(treatmentRecord);
            log.info("自动创建治疗记录成功，ID: {}, serverRecordId: {}, patientIdentifier: {}", 
                    treatmentRecord.getId(), serverRecordId, patientIdentifier);
            
            return treatmentRecord.getId();
            
        } catch (Exception e) {
            log.error("自动创建治疗记录失败", e);
            return null; // 创建失败，返回null，允许MEP记录独立保存
        }
    }
    
    /**
     * 根据患者姓名和生日查找患者ID
     * 1. 先用姓名查找患者表
     * 2. 如果只有一条记录，返回该患者ID
     * 3. 如果有多条记录，再用姓名和生日年份匹配
     * 4. 都找不到返回0L
     * 
     * @param patientName 患者姓名
     * @param patientBirthday 患者生日（可能是年份如"1990"，或完整日期如"1990-05-15"），可为null
     * @return 患者ID，找不到返回0L
     */
    private Long findPatientIdByNameAndBirthday(String patientName, String patientBirthday) {
        if (patientName == null || patientName.trim().isEmpty()) {
            return 0L;
        }
        
        try {
            // 1. 先用姓名查找患者表
            LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Patient::getName, patientName.trim());
            List<Patient> patients = patientMapper.selectList(wrapper);
            
            if (patients == null || patients.isEmpty()) {
                log.debug("未找到患者，姓名: {}", patientName);
                return 0L;
            }
            
            // 2. 如果只有一条记录，直接返回
            if (patients.size() == 1) {
                Long patientId = patients.get(0).getId();
                log.debug("通过姓名找到唯一患者，姓名: {}, 患者ID: {}", patientName, patientId);
                return patientId;
            }
            
            // 3. 如果有多条记录，且有生日信息，用姓名+生日年份匹配
            if (patientBirthday != null && !patientBirthday.trim().isEmpty()) {
                try {
                    // 提取年份：如果传入的是完整日期（如"1990-05-15"），提取年份部分；如果只有年份（如"1990"），直接使用
                    String inputYear = extractYear(patientBirthday.trim());
                    if (inputYear != null) {
                        for (Patient patient : patients) {
                            if (patient.getBirthday() != null) {
                                String patientYear = yearFormat.format(patient.getBirthday());
                                if (patientYear.equals(inputYear)) {
                                    Long patientId = patient.getId();
                                    log.debug("通过姓名和生日年份找到患者，姓名: {}, 生日年份: {}, 患者ID: {}", 
                                            patientName, inputYear, patientId);
                                    return patientId;
                                }
                            }
                        }
                        log.debug("通过姓名找到多条记录，但生日年份不匹配，姓名: {}, 生日年份: {}", patientName, inputYear);
                    }
                } catch (Exception e) {
                    log.warn("解析生日年份失败，姓名: {}, 生日: {}, 错误: {}", patientName, patientBirthday, e.getMessage());
                }
            } else {
                log.debug("通过姓名找到多条记录，但没有生日信息，姓名: {}", patientName);
            }
            
            // 4. 都找不到，返回0L
            return 0L;
            
        } catch (Exception e) {
            log.error("查找患者ID失败，姓名: {}, 生日: {}, 错误: {}", patientName, patientBirthday, e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 从生日字符串中提取年份
     * 支持格式：yyyy（如"1990"）或 yyyy-MM-dd（如"1990-05-15"）
     * 
     * @param birthdayStr 生日字符串
     * @return 年份字符串，如果解析失败返回null
     */
    private String extractYear(String birthdayStr) {
        if (birthdayStr == null || birthdayStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 如果只有4位数字，直接返回
            if (birthdayStr.matches("^\\d{4}$")) {
                return birthdayStr;
            }
            
            // 如果是完整日期格式（yyyy-MM-dd），提取年份部分
            if (birthdayStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return birthdayStr.substring(0, 4);
            }
            
            // 尝试解析为日期，然后提取年份
            Date date = dateFormat.parse(birthdayStr);
            return yearFormat.format(date);
        } catch (Exception e) {
            log.warn("提取生日年份失败，生日字符串: {}, 错误: {}", birthdayStr, e.getMessage());
            return null;
        }
    }
    
    /**
     * 从MEP数据中计算MEP值并更新治疗主表和患者表
     * 
     * @param mepRecordId MEP记录ID
     * @param treatmentRecordId 治疗记录ID
     * @param patientIdentifier 患者唯一标识
     */
    private void updateMepValueFromMepData(Long mepRecordId, Long treatmentRecordId, String patientIdentifier) {
        try {
            // 1. 查询MEP数据
            List<MepDataVO> mepDataList = mepDataMapper.selectByMepRecordId(mepRecordId);
            if (mepDataList == null || mepDataList.isEmpty()) {
                log.warn("MEP记录没有数据，无法计算MEP值，mepRecordId: {}", mepRecordId);
                return;
            }
            
            // 2. 计算MEP值（取mt值，如果有多个mepData，取第一个有效的mt值）
            Integer mepValue = null;
            for (MepDataVO mepData : mepDataList) {
                if (mepData.getMt() != null) {
                    mepValue = mepData.getMt();
                    break; // 取第一个有效的mt值
                }
            }
            
            if (mepValue == null) {
                log.warn("无法从MEP数据中获取MEP值（mt字段为空），mepRecordId: {}", mepRecordId);
                return;
            }
            
            // 3. 更新治疗主表的MEP值
            TreatmentRecord treatmentRecord = treatmentRecordMapper.selectById(treatmentRecordId);
            if (treatmentRecord != null) {
                treatmentRecord.setMepValue(mepValue);
                treatmentRecord.setUpdateTime(new Date());
                treatmentRecordMapper.updateById(treatmentRecord);
                log.info("更新治疗主表MEP值成功，treatmentRecordId: {}, mepValue: {}", treatmentRecordId, mepValue);
                
                // 4. 更新患者表的MEP值
                updatePatientMepValue(treatmentRecord.getPatientId(), mepValue);
            } else {
                log.warn("未找到治疗记录，无法更新MEP值，treatmentRecordId: {}", treatmentRecordId);
            }
            
        } catch (Exception e) {
            log.error("从MEP数据更新MEP值失败，mepRecordId: {}, treatmentRecordId: {}", mepRecordId, treatmentRecordId, e);
        }
    }
    
    /**
     * 更新患者表的MEP值
     * 
     * @param patientId 患者ID
     * @param mepValue MEP值
     */
    private void updatePatientMepValue(Long patientId, Integer mepValue) {
        if (patientId == null || patientId <= 0) {
            log.debug("患者ID无效，跳过更新患者表MEP值，patientId: {}", patientId);
            return;
        }
        
        try {
            Patient patient = patientMapper.selectById(patientId);
            if (patient != null) {
                patient.setMepValue(mepValue);
                patient.setUpdatedAt(new Date());
                patientMapper.updateById(patient);
                log.info("更新患者表MEP值成功，patientId: {}, mepValue: {}", patientId, mepValue);
            } else {
                log.warn("未找到患者，无法更新MEP值，patientId: {}", patientId);
            }
        } catch (Exception e) {
            log.error("更新患者表MEP值失败，patientId: {}, mepValue: {}", patientId, mepValue, e);
        }
    }
    
    /**
     * 获取字符串值，如果不存在返回null
     */
    private String getStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : null;
    }
}


