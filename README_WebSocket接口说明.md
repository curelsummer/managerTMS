# TMS管理系统 处方执行记录WebSocket接口说明

 概述

本系统实现了处方执行记录创建成功后，通过WebSocket实时推送相关数据给客户端的功能。当后端创建新的处方执行记录时，会自动将处方信息、患者信息、设备信息等完整数据通过WebSocket发送给所有连接的客户端。

 系统架构

 后端组件

1. PrescriptionExecutionWebSocketServer WebSocket服务器 
   端点：`/ws/prescriptionexecution`
   功能：管理客户端连接，广播处方执行记录通知
   位置：`src/main/java/cc/mrbird/febs/system/websocket/PrescriptionExecutionWebSocketServer.java`

2. PrescriptionExecutionNotificationService 通知服务 
   功能：构建通知数据，调用WebSocket广播
   位置：`src/main/java/cc/mrbird/febs/system/service/PrescriptionExecutionNotificationService.java`

3. PrescriptionExecutionNotification 数据传输对象 
   功能：封装发送给客户端的数据结构
   位置：`src/main/java/cc/mrbird/febs/system/domain/PrescriptionExecutionNotification.java`

4. PrescriptionExecutionStatusService 状态更新服务 
   功能：处理客户端确认消息，更新数据库状态
   位置：`src/main/java/cc/mrbird/febs/system/service/PrescriptionExecutionStatusService.java`

 触发机制

 处方执行记录创建触发 

触发位置：`src/main/java/cc/mrbird/febs/system/controller/PrescriptionExecutionController.java`

触发条件：当通过POST `/prescription-execution` 接口创建新的处方执行记录时

触发流程：
1. 保存处方执行记录到数据库
2. 如果保存成功，调用通知服务
3. 通知服务构建完整数据
4. 通过WebSocket广播给所有连接的客户端

```java
// 触发代码位置：PrescriptionExecutionController.add() 方法
if (success) {
    // 发送WebSocket通知
    notificationService.notifyPrescriptionExecutionCreated(execution);
    return new FebsResponse().put("success", true).message("处方执行记录创建成功");
}
```

 数据构建规则

 已实现的数据查询逻辑 

1. 处方信息: 根据`execution.prescriptionId`查询处方表获取
   查询服务：`PrescriptionService.getById()`
   包含字段：处方类型、状态、强度、频率、时间、部位等

2. 患者信息: 根据`execution.patientId`查询患者表获取
   查询服务：`PatientService.getById()`
   包含字段：姓名、身份证、性别、生日、HIS ID等

3. 设备信息: 根据`execution.deviceId`查询设备表获取
   查询服务：`DeviceService.getById()`
   包含字段：设备ID、类型、序列号、状态、最后心跳等

4. 执行人信息: 根据`execution.executorId`查询用户表获取
   查询服务：`UserService.getById()`
   包含字段：用户ID、用户名、邮箱、手机等

5. 医院信息: 根据`execution.hospitalId`查询医院表获取
   查询服务：`HospitalService.getById()`
   包含字段：医院ID、名称、地址、联系方式等

 数据格式

 发送给客户端的完整数据结构 

```json
{
  "messageType": "PRESCRIPTION_EXECUTION_CREATED",
  "timestamp": "2025-01-20T15:45:30.456+08:00",
  "executionId": 1002,
  "executionStatus": 1,
  "prescriptionInfo": {
    "id": 2002,
    "presType": 0,
    "status": 0,
    "presStrength": 60,
    "presFreq": "8.0",
    "lastTime": "2.5",
    "pauseTime": 10,
    "repeatCount": 1,
    "totalCount": 30,
    "totalTime": 1200,
    "presPartId": 12,
    "presPartName": "运动皮层",
    "standardPresId": null,
    "standardPresName": null,
    "tbsType": null,
    "innerCount": null,
    "interFreq": null,
    "interCount": null,
    "periods": 1,
    "createdAt": "2025-01-20T15:40:00.000+08:00"
  },
  "patientInfo": {
    "id": 3002,
    "name": "李四",
    "idCard": "110101199205151234",
    "gender": "女",
    "birthday": "1992-05-15T00:00:00.000+08:00",
    "hisId": null,
    "code": null
  },
  "deviceInfo": {
    "deviceId": 4002,
    "deviceType": "TMS设备",
    "sn": "TMS2025002",
    "status": "offline",
    "lastHeartbeat": null
  },
  "executorInfo": {
    "userId": 5002,
    "username": "doctor002",
    "email": null,
    "mobile": null
  },
  "hospitalInfo": {
    "hospitalId": 6002,
    "name": "北京大学第一医院",
    "address": null,
    "contact": null
  }
}
```

 客户端接收确认机制

 客户端发送确认消息 

消息类型: `PRESCRIPTION_EXECUTION_RECEIVED`

JSON格式:
```json
{
  "messageType": "PRESCRIPTION_EXECUTION_RECEIVED",
  "executionId": 1001,
  "status": "RECEIVED_SUCCESS",
  "clientInfo": "WPF客户端-v1.0.0",
  "timestamp": "2025-01-20T14:30:25.123+08:00"
}
```

 服务器状态更新 

处理服务: `PrescriptionExecutionStatusService`

状态映射:
`PENDING` → 0 (待下发)
`DISPATCHED` → 1 (已下发)
`EXECUTING` → 2 (执行中)
`COMPLETED` → 3 (完成)
`FAILED` → 4 (异常)

 服务器确认回复 

消息类型: `PRESCRIPTION_EXECUTION_CONFIRMATION`

JSON格式:
```json
{
  "messageType": "PRESCRIPTION_EXECUTION_CONFIRMATION",
  "executionId": 1001,
  "result": "SUCCESS",
  "timestamp": "2025-01-20T14:30:25.456+08:00",
  "serverInfo": "TMS Backend Server"
}
```

 客户端接收示例

 JavaScript客户端
```javascript
const websocket = new WebSocket('ws://localhost:9527/ws/prescriptionexecution');

websocket.onmessage = function(event) {
    const data = JSON.parse(event.data);
    
    if (data.messageType === 'PRESCRIPTION_EXECUTION_CREATED') {
        console.log('收到新的处方执行记录:', data.executionId);
        console.log('患者姓名:', data.patientInfo?.name);
        console.log('治疗部位:', data.prescriptionInfo?.presPartName);
        console.log('执行人:', data.executorInfo?.username);
        
        // 发送确认消息
        sendReceiptConfirmation(data.executionId);
    }
};

function sendReceiptConfirmation(executionId) {
    const confirmationMessage = {
        messageType: "PRESCRIPTION_EXECUTION_RECEIVED",
        executionId: executionId,
        status: "RECEIVED_SUCCESS",
        clientInfo: "Web客户端-v1.0.0",
        timestamp: new Date().toISOString()
    };
    
    websocket.send(JSON.stringify(confirmationMessage));
}
```

 C#客户端
```csharp
using System.Net.WebSockets;
using System.Text;
using Newtonsoft.Json;

var websocket = new ClientWebSocket();
await websocket.ConnectAsync(new Uri("ws://localhost:9527/ws/prescriptionexecution"), CancellationToken.None);

var buffer = new byte[4096];
while (websocket.State == WebSocketState.Open)
{
    var result = await websocket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
    if (result.MessageType == WebSocketMessageType.Text)
    {
        var message = Encoding.UTF8.GetString(buffer, 0, result.Count);
        var data = JsonConvert.DeserializeObject<dynamic>(message);
        
        if (data.messageType == "PRESCRIPTION_EXECUTION_CREATED")
        {
            Console.WriteLine($"收到新的处方执行记录: {data.executionId}");
            
            // 发送确认消息
            await SendReceiptConfirmation(websocket, data.executionId);
        }
    }
}

private async Task SendReceiptConfirmation(ClientWebSocket websocket, long executionId)
{
    var confirmationMessage = new
    {
        messageType = "PRESCRIPTION_EXECUTION_RECEIVED",
        executionId = executionId,
        status = "RECEIVED_SUCCESS",
        clientInfo = "WPF客户端-v1.0.0",
        timestamp = DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss.fffzzz")
    };

    string jsonMessage = JsonConvert.SerializeObject(confirmationMessage);
    var messageBytes = Encoding.UTF8.GetBytes(jsonMessage);
    await websocket.SendAsync(new ArraySegment<byte>(messageBytes), WebSocketMessageType.Text, true, CancellationToken.None);
}
```

 配置和部署

 后端配置 

1. WebSocket端点配置
位置：`src/main/java/cc/mrbird/febs/common/authentication/ShiroConfig.java`
配置：允许匿名访问WebSocket端点
```java
filterChainDefinitionMap.put("/ws/prescriptionexecution", "anon");
```

2. WebSocket服务器配置
位置：`src/main/java/cc/mrbird/febs/common/config/WebSocketConfig.java`
功能：初始化ApplicationContext，支持依赖注入

3. 服务器端口配置
位置：`src/main/resources/application.yml`
端口：9527

 客户端集成
1. 使用任何支持WebSocket的客户端库
2. 连接到正确的WebSocket端点：`ws://localhost:9527/ws/prescriptionexecution`
3. 解析JSON格式的消息数据
4. 根据messageType字段判断消息类型
5. 实现确认消息发送机制

 错误处理和日志记录

 已实现的错误处理机制 

1. 详细的日志记录
WebSocket连接/断开日志
消息处理日志
数据构建日志
广播发送日志
状态更新日志

2. 异常处理
数据库查询异常处理
WebSocket发送异常处理
消息解析异常处理
状态更新异常处理

3. 错误恢复机制
连接断开自动重连
消息发送失败重试
状态更新失败回滚

 扩展功能

 支持的消息类型 

已实现的消息类型：
`PRESCRIPTION_EXECUTION_CREATED`: 处方执行记录创建 
`PRESCRIPTION_EXECUTION_RECEIVED`: 客户端接收确认 
`PRESCRIPTION_EXECUTION_CONFIRMATION`: 服务器确认回复 
`PRESCRIPTION_STATUS_UPDATE`: 处方状态更新 
`PRESCRIPTION_STATUS_UPDATE_CONFIRMATION`: 状态更新确认 

可以扩展支持的消息类型：
`PRESCRIPTION_EXECUTION_UPDATED`: 处方执行记录更新
`PRESCRIPTION_EXECUTION_COMPLETED`: 处方执行记录完成
`PRESCRIPTION_EXECUTION_CANCELLED`: 处方执行记录取消

 数据过滤功能
可以根据需要添加数据过滤功能：
按医院过滤
按设备类型过滤
按执行状态过滤

 性能优化

 已实现的优化措施 

1. 连接管理
使用CopyOnWriteArraySet管理WebSocket会话
自动清理断开的连接
连接状态监控

2. 消息处理
异步消息处理
批量消息发送
消息大小限制

3. 数据库优化
按需查询关联数据
缓存常用数据
事务管理

 监控和调试

 已实现的监控功能 

1. 连接监控
实时连接数统计
连接状态日志
连接异常告警

2. 消息监控
消息发送统计
消息处理时间
消息失败率

3. 调试功能
详细的控制台日志
消息内容打印
错误堆栈跟踪

 注意事项

1. 连接管理: 客户端需要正确处理连接断开和重连 
2. 数据安全: 敏感信息在传输前应进行适当处理 
3. 性能考虑: 大量客户端连接时需要考虑服务器性能 
4. 错误恢复: 实现适当的错误处理和恢复机制 
5. 日志记录: 建议记录WebSocket连接和消息传输日志 
6. 状态同步: 确保客户端和服务器状态一致 
7. 消息确认: 重要消息需要客户端确认 

 技术支持

如有问题，请检查：
1. 服务器日志文件 
2. 客户端连接状态 
3. 网络连接情况 
4. 数据格式是否正确 
5. 确认消息是否正常发送 
6. 数据库状态是否正常 

 功能完整性确认

  已完全实现的功能

[x] WebSocket服务器配置和运行
[x] 通知服务数据构建和发送
[x] 数据传输对象完整定义
[x] 数据构建规则完全实现
[x] 触发机制正确配置
[x] 客户端确认机制完善
[x] 错误处理和日志记录详细
[x] 部署配置正确
[x] 状态更新机制完整
[x] 消息类型支持完善
[x] 性能优化措施到位
[x] 监控和调试功能齐全

系统已完全实现了处方执行记录WebSocket实时推送功能，包括数据构建、消息发送、客户端确认、状态更新等完整流程。 