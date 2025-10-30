# YYChatRoom TCP文件传输通道实现计划

## 项目概述
基于现有的UDP文件传输实现，为YYChatRoom添加TCP通道支持，用于可靠的大文件传输（如图片、文档等）。同时计划将现有的UDP文件传输功能逐步迁移到TCP协议。

## 技术分析

### 现状评估
- ✅ **现有基础**：项目已实现完整的UDP文件传输（头像系统）
- ✅ **架构完整**：Message类、AvatarFileManager、客户端/服务端通信架构完善
- ✅ **数据库支持**：用户信息、头像路径存储在MySQL数据库中
- ⚠️ **传输限制**：UDP协议存在64KB限制，不适合大文件传输

### TCP vs UDP对比分析

| 特性 | UDP (现状) | TCP (目标) | 优势 |
|------|------------|------------|------|
| 连接方式 | 无连接 | 面向连接 | 传输可靠，确保完整性 |
| 数据包大小 | ≤64KB | 无限制 | 支持大文件传输 |
| 可靠性 | 低（可能丢包） | 高（重传机制） | 确保文件完整传输 |
| 速度 | 快 | 稍慢 | 更稳定的传输体验 |
| 适用场景 | 小文件、头像 | 大文件、文档 | 满足不同文件传输需求 |

## 核心需求

### 主要需求
1. **实现TCP文件传输通道**
   - 新增TCP通信架构
   - 支持大文件传输（无64KB限制）
   - 提供可靠的传输保证

2. **UDP到TCP迁移**
   - 将现有头像传输从UDP迁移到TCP
   - 保持向后兼容性
   - 确保用户体验提升

3. **增强文件传输功能**
   - 分片传输支持
   - 断点续传功能
   - 传输进度显示
   - 文件完整性校验

### 技术需求
- 支持多种文件格式（图片、文档、视频等）
- 文件大小无限制（受系统内存限制）
- 并发传输支持
- 错误重试机制
- 传输进度回调

## 实现计划

### 第一阶段：TCP通信架构设计

#### 1.1 新增TCP相关类
```
Server/src/com/yychat/
├── tcp/                          # 新增TCP通信包
│   ├── control/
│   │   ├── YYchatServerTCP.java         # TCP服务器核心类
│   │   ├── FileTransferManager.java     # 文件传输管理器
│   │   └── ServerReceiverThreadTCP.java # TCP消息接收线程
│   └── model/
│       ├── FileMessage.java             # 文件传输消息类
│       └── TransferStatus.java          # 传输状态枚举
```

#### 1.2 客户端TCP架构
```
LoginClient/src/com/yychat/
├── tcp/                          # 新增TCP客户端
│   ├── control/
│   │   ├── YYchatClientTCP.java        # TCP客户端类
│   │   ├── FileUploadThread.java       # 文件上传线程
│   │   └── FileDownloadThread.java     # 文件下载线程
│   └── model/
│       └── ProgressListener.java       # 传输进度监听器
```

### 第二阶段：数据模型扩展

#### 2.1 FileMessage类设计
```java
public class FileMessage implements Serializable {
    private String fileName;           // 文件名
    private long fileSize;            // 文件大小（字节）
    private byte[] fileData;          // 文件数据
    private String fileType;          // 文件类型（MIME）
    private String sender;            // 发送者
    private String receiver;          // 接收者
    private String transferId;        // 传输唯一标识
    private int chunkIndex;           // 分片索引（用于大文件）
    private int totalChunks;          // 总分片数
    private boolean isLastChunk;      // 是否最后一片
}
```

#### 2.2 传输状态管理
```java
public enum TransferStatus {
    PENDING,        // 等待传输
    IN_PROGRESS,    // 传输中
    COMPLETED,      // 传输完成
    FAILED,         // 传输失败
    CANCELLED       // 传输取消
}
```

### 第三阶段：服务器端TCP实现

#### 3.1 TCP服务器核心类
**功能特性：**
- 监听专用TCP端口（建议端口：3457）
- 支持多线程并发文件传输
- 文件传输进度跟踪
- 自动文件类型检测和验证

#### 3.2 文件传输管理器
**核心功能：**
- 大文件分片传输支持
- 传输进度回调通知
- 文件完整性校验（MD5/SHA256）
- 断点续传支持

#### 3.3 数据库集成增强
```java
// 扩展DBUtil.java，支持文件传输记录
public class DBUtil {
    // 新增文件传输记录表
    public static boolean saveFileTransferRecord(String transferId, String fileName,
                                               String sender, String receiver, long fileSize);
    public static TransferStatus getTransferStatus(String transferId);
    public static void updateTransferProgress(String transferId, int progress);
}
```

### 第四阶段：客户端TCP实现

#### 4.1 TCP客户端通信类
**核心功能：**
- 与TCP服务器建立连接
- 文件上传/下载操作封装
- 进度回调机制
- 错误重试机制

#### 4.2 文件传输UI集成
**在FriendChat.java中集成TCP文件传输：**
- 添加"发送文件"按钮
- 文件选择对话框（支持多种格式）
- 传输进度条显示
- 传输完成通知

#### 4.3 头像系统TCP升级
**将头像传输迁移到TCP：**
- 优先使用TCP传输（更可靠）
- UDP作为备选方案
- 保持向后兼容性

### 第五阶段：分片传输实现

#### 5.1 大文件分片策略
```java
public class FileChunking {
    private static final int CHUNK_SIZE = 64 * 1024; // 64KB分片

    public List<FileMessage> chunkFile(String filePath, String sender, String receiver) {
        // 1. 读取文件
        byte[] fileData = Files.readAllBytes(Paths.get(filePath));

        // 2. 计算分片数
        int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);

        // 3. 分片处理
        List<FileMessage> chunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileData.length);
            byte[] chunkData = Arrays.copyOfRange(fileData, start, end);

            FileMessage chunk = new FileMessage();
            chunk.setFileData(chunkData);
            chunk.setChunkIndex(i);
            chunk.setTotalChunks(totalChunks);
            chunk.setIsLastChunk(i == totalChunks - 1);
            chunks.add(chunk);
        }
        return chunks;
    }
}
```

#### 5.2 断点续传机制
```java
public class ResumeTransfer {
    public boolean resumeFileTransfer(String transferId, String filePath) {
        // 1. 查询已传输的分片
        List<Integer> receivedChunks = getReceivedChunks(transferId);

        // 2. 从未完成的分片继续传输
        List<FileMessage> remainingChunks = getRemainingChunks(filePath, receivedChunks);

        // 3. 继续传输
        return transferChunks(remainingChunks);
    }
}
```

### 第六阶段：UDP到TCP迁移

#### 6.1 头像传输迁移
**迁移策略：**
1. **阶段一：并行运行**
   - TCP和UDP并行工作
   - TCP作为首选，UDP作为备选
   - 用户体验无影响

2. **阶段二：功能切换**
   - 新功能全部使用TCP
   - 保留UDP兼容模式
   - 逐步关闭UDP文件传输

3. **阶段三：完全迁移**
   - 移除UDP文件传输相关代码
   - 清理不必要的UDP消息类型
   - 优化TCP性能

#### 6.2 修改现有代码
**需要修改的文件：**

1. **AvatarFileManager.java**
   - 新增TCP传输支持方法
   - 保持UDP兼容
   - 添加传输方式选择逻辑

2. **ClientReceiverThreadUDP.java**
   - 添加TCP头像请求支持
   - 保持UDP响应处理
   - 实现传输方式切换

3. **ServerReceiverThreadUDP.java**
   - 集成TCP传输调用
   - 保持UDP响应逻辑
   - 添加传输统计功能

4. **FriendList.java**
   - 更新头像显示逻辑
   - 支持TCP传输进度
   - 优化加载性能

### 第七阶段：系统集成与测试

#### 7.1 协议统一
**MessageType.java新增：**
```java
String TCP_FILE_TRANSFER_REQUEST = "30";    // TCP文件传输请求
String TCP_FILE_TRANSFER_START = "31";      // 开始TCP传输
String TCP_FILE_TRANSFER_CHUNK = "32";      // 传输文件分片
String TCP_FILE_TRANSFER_COMPLETE = "33";   // 传输完成
String TCP_FILE_TRANSFER_ERROR = "34";      // 传输错误
String MIGRATION_AVATAR_TO_TCP = "35";      // 头像迁移到TCP
```

#### 7.2 客户端UI升级
**FriendChat.java增强：**
- 文件拖拽支持
- 传输历史记录
- 文件预览功能
- 传输速度显示
- TCP/UDP传输方式显示

#### 7.3 性能监控
**添加监控指标：**
- 传输速度（KB/s）
- 传输成功率
- 平均传输时间
- 文件类型分布
- TCP vs UDP性能对比

## 实施时间线

### 第一周：基础架构
- [ ] 创建TCP相关包结构
- [ ] 实现FileMessage和TransferStatus类
- [ ] 设计TCP通信协议
- [ ] 制定UDP迁移策略

### 第二周：服务器端开发
- [ ] 实现YYchatServerTCP.java
- [ ] 开发FileTransferManager
- [ ] 集成数据库支持
- [ ] 实现基础文件传输

### 第三周：客户端开发
- [ ] 实现YYchatClientTCP.java
- [ ] 开发文件传输线程
- [ ] 集成UI组件
- [ ] 实现头像TCP传输

### 第四周：迁移实施
- [ ] 修改AvatarFileManager支持TCP
- [ ] 更新ClientReceiverThreadUDP
- [ ] 修改ServerReceiverThreadUDP
- [ ] 更新FriendList显示逻辑

### 第五周：高级功能
- [ ] 实现文件分片传输
- [ ] 添加断点续传功能
- [ ] 性能优化
- [ ] 完成UDP到TCP迁移

## 风险评估与缓解

### 主要风险
1. **性能影响**：TCP连接可能影响服务器性能
   - **缓解**：使用独立线程池处理TCP连接
2. **内存占用**：大文件传输可能消耗大量内存
   - **缓解**：流式传输，避免一次性加载整个文件
3. **向后兼容**：迁移可能影响现有功能
   - **缓解**：渐进式迁移，保持UDP备选方案

## 迁移检查清单

### 代码迁移
- [ ] AvatarFileManager.java - 添加TCP支持
- [ ] ClientReceiverThreadUDP.java - 集成TCP调用
- [ ] ServerReceiverThreadUDP.java - 添加TCP处理
- [ ] FriendList.java - 更新头像显示
- [ ] Message.java - 扩展文件传输字段

### 数据库迁移
- [ ] 新增file_transfer_records表
- [ ] 添加transfer_status字段
- [ ] 创建传输统计视图

### UI迁移
- [ ] 更新文件传输界面
- [ ] 添加传输进度显示
- [ ] 实现文件选择器
- [ ] 添加传输历史记录
