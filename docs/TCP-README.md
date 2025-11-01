# TCP客户端通信框架

这个TCP客户端通信框架为YYChatRoom聊天应用提供了完整的文件传输功能。

## 架构设计

### 核心组件

1. **TCPClient** - TCP连接管理核心类
   - 负责TCP连接建立、维护、断开
   - 消息发送和接收
   - 响应处理和异步任务管理

2. **FileTransferClient** - 文件传输专用类
   - 文件上传功能
   - 文件下载功能
   - 批量文件处理
   - 传输进度监控

3. **TCPClientManager** - 统一管理类
   - 整合TCP和文件传输功能
   - 提供简化的API接口
   - 适合在客户端应用中使用

4. **TCPExample** - 使用示例
   - 演示各种功能的使用方法
   - 最佳实践参考

## 快速开始

### 基本使用

```java
// 1. 创建TCP客户端管理器
TCPClientManager manager = new TCPClientManager();

// 2. 连接服务器
if (manager.connect()) {
    System.out.println("连接成功");

    // 3. 设置当前用户
    manager.setCurrentUser("username");

    // 4. 上传文件
    CompletableFuture<FileTransferResult> uploadResult =
        manager.uploadFile("path/to/your/file.txt");

    uploadResult.thenAccept(result -> {
        if (result.isSuccess()) {
            System.out.println("上传成功: " + result.getFilePath());
        } else {
            System.out.println("上传失败: " + result.getMessage());
        }
    });

    // 5. 下载文件
    CompletableFuture<FileTransferResult> downloadResult =
        manager.downloadFile("server_filename.txt");

    downloadResult.thenAccept(result -> {
        if (result.isSuccess()) {
            System.out.println("下载成功: " + result.getFilePath());
        } else {
            System.out.println("下载失败: " + result.getMessage());
        }
    });
}

// 6. 清理资源
manager.cleanup();
```

### 带进度回调的文件传输

```java
manager.uploadFile("large_file.zip", new FileTransferClient.FileTransferCallback() {
    @Override
    public void onProgress(int current, int total, String status) {
        System.out.println("上传进度: " + (current * 100 / total) + "% - " + status);
    }

    @Override
    public void onComplete(boolean success, String message) {
        if (success) {
            System.out.println("文件上传完成: " + message);
        } else {
            System.err.println("文件上传失败: " + message);
        }
    }
});
```

### 批量文件处理

```java
String[] filesToUpload = {
    "file1.txt",
    "file2.jpg",
    "file3.pdf"
};

CompletableFuture<BatchTransferResult> batchResult =
    manager.uploadFiles(filesToUpload);

batchResult.thenAccept(result -> {
    if (result.isOverallSuccess()) {
        System.out.println("所有文件上传成功");
    } else {
        System.out.println("部分文件上传失败:");
        result.getErrors().forEach(System.err::println);
    }
});
```

## 配置文件

### 服务器配置

TCP客户端默认连接配置：
- 服务器地址：`localhost`
- TCP端口：`3457`
- 连接超时：5秒
- 读取超时：30秒

### 自定义配置

如需修改服务器地址和端口，需要修改`TCPClient.java`中的常量：

```java
private static final String TCP_HOST = "your-server-host";
private static final int TCP_PORT = 3457;
```

## 最佳实践

### 1. 资源管理

```java
// 使用try-with-resources确保资源释放
try (TCPClientManager manager = new TCPClientManager()) {
    manager.connect();
    // 执行文件传输操作
} // 自动调用cleanup()
```

### 2. 错误处理

```java
CompletableFuture<FileTransferResult> result = manager.uploadFile("file.txt");
try {
    FileTransferResult transferResult = result.get(30, TimeUnit.SECONDS);
    if (!transferResult.isSuccess()) {
        // 处理业务错误
        System.err.println("上传失败: " + transferResult.getMessage());
    }
} catch (TimeoutException e) {
    // 处理超时
    System.err.println("上传超时");
} catch (Exception e) {
    // 处理其他异常
    System.err.println("上传异常: " + e.getMessage());
}
```

### 3. 异步操作

```java
// 所有文件传输操作都是异步的，返回CompletableFuture
CompletableFuture<FileTransferResult> upload = manager.uploadFile("file.txt");

// 可以链式调用
upload.thenCompose(result -> {
    if (result.isSuccess()) {
        return manager.downloadFile("server_file.txt");
    }
    return CompletableFuture.completedFuture(null);
}).thenAccept(downloadResult -> {
    // 处理下载结果
});
```

### 4. 进度监控

```java
manager.uploadFile("large_file.zip", new FileTransferClient.FileTransferCallback() {
    @Override
    public void onProgress(int current, int total, String status) {
        double progress = (double) current / total * 100;
        System.out.printf("进度: %.1f%% - %s%n", progress, status);
    }

    @Override
    public void onComplete(boolean success, String message) {
        System.out.println("传输" + (success ? "成功" : "失败") + ": " + message);
    }
});
```

## 消息协议

### 支持的消息类型

- `TCP_ACK` (100) - TCP处理确认
- `TCP_FILE_UPLOAD` (101) - TCP文件上传
- `TCP_FILE_DOWNLOAD` (102) - TCP文件下载

### 消息格式

文件上传消息：
```json
{
    "fileName": "example.txt",
    "fileSize": 1024,
    "uploadTime": "2024-01-01T12:00:00",
    "fileData": [binary_data]
}
```

文件下载消息：
```json
{
    "remoteFileName": "example.txt",
    "localFileName": "downloaded.txt",
    "requestTime": "2024-01-01T12:00:00"
}
```

## 运行示例

运行示例代码：

```bash
# 编译项目
javac -cp "lib/*:out/production/ChatRoom" -d out/production/ChatRoom ChatRoom/src/com/yychat/**/*.java

# 运行示例
java -cp "lib/*:out/production/ChatRoom" com.yychat.client.tcp.TCPExample
```

## 注意事项

1. **服务器要求**: 确保TCP服务器（`YYChatTCPServer`）正在运行
2. **网络配置**: 确保客户端可以访问服务器的3457端口
3. **文件权限**: 确保对上传/下载目录有读写权限
4. **内存管理**: 大文件传输可能消耗较多内存，建议分块处理
5. **异常处理**: 所有网络操作都可能抛出异常，需要适当处理

## 故障排除

### 连接失败
- 检查TCP服务器是否运行
- 确认端口3457没有被防火墙阻挡
- 验证服务器地址配置

### 传输失败
- 检查文件路径是否正确
- 确认服务器端文件管理功能正常
- 查看服务器日志获取详细错误信息

### 超时问题
- 增加连接和读取超时时间
- 检查网络连接稳定性
- 考虑使用更小文件进行测试