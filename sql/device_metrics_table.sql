-- 设备指标数据表
-- 用于存储设备上报的实时指标数据（线圈温度、机温、水泵流速）

CREATE TABLE IF NOT EXISTS `device_metrics` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型',
  `device_id` VARCHAR(50) NOT NULL COMMENT '设备编号',
  `coil_temperature` DECIMAL(10,2) DEFAULT NULL COMMENT '线圈温度（℃）',
  `machine_temperature` DECIMAL(10,2) DEFAULT NULL COMMENT '机温（℃）',
  `pump_flow_rate` DECIMAL(10,2) DEFAULT NULL COMMENT '水泵流速（L/min）',
  `msg_id` VARCHAR(100) DEFAULT NULL COMMENT '消息ID，用于去重',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（数据上报时间）',
  `server_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务器接收时间',
  PRIMARY KEY (`id`),
  KEY `idx_device` (`device_type`, `device_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_msg_id` (`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备指标数据表';

-- 查看表结构
-- DESC device_metrics;

-- 查看表注释
-- SHOW CREATE TABLE device_metrics;

