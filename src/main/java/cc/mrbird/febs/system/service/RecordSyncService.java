package cc.mrbird.febs.system.service;

import cc.mrbird.febs.system.domain.response.SyncResponse;

/**
 * 记录同步服务接口
 */
public interface RecordSyncService {
    
    /**
     * 根据同步请求查询患者的所有相关记录
     * 
     * @param patientIdentifier 患者唯一标识
     * @param syncType 同步类型：ALL、TREATMENT、MEP
     * @param requestDeviceNo 请求设备编号（用于过滤，服务器返回所有数据，设备端自己过滤）
     * @return 同步响应
     */
    SyncResponse syncPatientRecords(String patientIdentifier, String syncType, Integer requestDeviceNo);
}



