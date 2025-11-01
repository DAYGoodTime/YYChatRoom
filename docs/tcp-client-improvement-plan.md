# TCP客户端长连接+心跳机制改进计划

## 问题分析

当前TCPClient存在的问题：
1. **混合连接模式**：每次发送消息都建立新连接，发送完立即断开，效率低下
2. **资源泄漏**：异常情况下连接资源未正确清理
3. **空闲超时**：长连接空闲时会被网络设备断开连接
4. **状态不一致**：连接状态检查不准确

## 改进目标

1. **实现真正的长连接**：复用TCP连接，避免频繁连接建立/断开
2. **心跳机制**：定期发送心跳包防止连接空闲超时
3. **健壮的异常处理**：确保连接异常时能正确恢复
4. **连接状态管理**：准确跟踪和维护连接状态

## 实施计划

### 第一阶段：核心架构重构 (Task 1)

1. **移除短连接逻辑**
   - 修改 `sendMessage()` 方法，移除自动连接/断开逻辑
   - 改为使用现有的持久连接

2. **实现心跳机制**
   - 添加 `ScheduledExecutorService` 用于心跳调度
   - 实现 `startHeartbeat()` 和 `stopHeartbeat()` 方法
   - 创建心跳消息发送逻辑（TCP_HEARTBEAT消息类型）

3. **连接状态管理优化**
   - 改进 `connect()` 方法，确保连接状态检查的准确性
   - 实现连接自动重连机制
   - 添加连接健康检查

### 第二阶段：连接管理增强 (Task 2)

1. **连接生命周期管理**
   - 实现连接建立、维持、监控的完整流程
   - 添加连接断开的优雅处理机制
   - 实现连接重连逻辑

2. **异常处理增强**
   - 改进所有发送操作的异常处理
   - 添加连接恢复机制
   - 实现发送重试逻辑

3. **资源清理优化**
   - 确保所有连接资源正确释放
   - 实现心跳线程的正确停止

### 第三阶段：消息处理优化 (Task 3)

1. **并发安全改进**
   - 优化 `responseHandlers` 的并发访问
   - 改进消息发送的同步机制
   - 确保消息顺序性

2. **性能优化**
   - 减少不必要的同步块
   - 优化消息接收线程的效率
   - 添加连接池支持（如果需要）

### 第四阶段：测试和验证 (Task 4)

1. **功能测试**
   - 验证长连接的稳定性
   - 测试心跳机制的有效性
   - 验证异常恢复机制

2. **性能测试**
   - 测试长时间连接的稳定性
   - 验证大数据量传输性能
   - 压力测试并发连接

## 技术实现要点

### 心跳机制设计

```java
// 心跳间隔30秒，超时3次认为连接断开
private static final int HEARTBEAT_INTERVAL = 30;
private static final int MAX_HEARTBEAT_FAILURES = 3;

// 心跳失败计数和重连机制
private int heartbeatFailureCount = 0;
private ScheduledFuture<?> heartbeatTask;
```

### 连接状态管理

```java
// 改进的连接检查
public boolean ensureConnected() {
    if (isConnected()) {
        return true;
    }
    return connect();
}

// 自动重连逻辑
private boolean reconnectWithRetry(int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        if (connect()) {
            return true;
        }
        sleepWithBackoff(i);
    }
    return false;
}
```

### 消息发送优化

```java
// 改进的消息发送with重试
public Optional<Message> sendMessageWithRetry(Message message, int maxRetries) {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            if (!ensureConnected()) {
                continue;
            }
            return sendMessageInternal(message);
        } catch (Exception e) {
            if (attempt == maxRetries) {
                throw e;
            }
            disconnect(); // 清理损坏的连接
        }
    }
    return Optional.empty();
}
```

## 预期收益

1. **性能提升**：避免频繁连接建立/断开开销，提升大数据传输效率
2. **稳定性增强**：心跳机制防止连接空闲超时，自动重连提升容错性
3. **资源优化**：减少连接建立/断开产生的资源消耗
4. **用户体验**：更快的传输速度，更少的网络异常

## 风险评估

1. **复杂度增加**：长连接管理比短连接复杂，需要仔细处理各种边界情况
2. **调试难度**：连接问题可能更难以定位和调试
3. **内存使用**：保持连接会增加一定的内存使用

## 验证标准

1. 连续运行24小时连接不自动断开
2. 空闲状态下能正常响应心跳
3. 网络中断后能自动重连
4. 大文件传输（>10MB）成功率>99%
5. 并发多个文件传输正常工作

## 详细实现步骤

### 1. 添加消息类型定义

首先需要在 `MessageType.java` 中添加心跳消息类型：

```java
// TCP心跳消息类型
int TCP_HEARTBEAT = 103;
int TCP_HEARTBEAT_ACK = 104;
```

### 2. 修改TCPClient类的核心方法

```java
// 添加心跳相关字段
private ScheduledExecutorService heartbeatExecutor =
    Executors.newScheduledThreadPool(1);
private ScheduledFuture<?> heartbeatTask;
private int heartbeatFailureCount = 0;
private static final int HEARTBEAT_INTERVAL = 30; // 30秒
private static final int MAX_HEARTBEAT_FAILURES = 3;

// 修改sendMessage方法，移除自动连接/断开
public Optional<Message> sendMessage(Message message, int timeoutSeconds) {
    try {
        if (!isConnected()) {
            System.out.println("TCP连接未建立，无法发送消息");
            return Optional.empty();
        }

        synchronized (oos) {
            // 生成唯一的任务ID用于匹配响应
            final String taskId = UUID.randomUUID().toString();
            message.setSyncTaskId(taskId);
            System.out.println("发送TCP请求到服务端 " + JSONUtil.toJsonStr(message));

            CompletableFuture<Message> task = new CompletableFuture<>();
            responseHandlers.put(taskId, task);

            oos.writeObject(message);
            oos.flush();

            Message response = task.get(timeoutSeconds, TimeUnit.SECONDS);
            return Optional.ofNullable(response);
        }

    } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
        System.out.println("TCP发送失败: " + e.getMessage());
        // 连接可能已断开，尝试重连
        disconnect();
        return Optional.empty();
    }
}

// 实现心跳机制
private void startHeartbeat() {
    heartbeatTask = heartbeatExecutor.scheduleWithFixedDelay(() -> {
        if (isConnected()) {
            try {
                Message heartbeat = new Message();
                heartbeat.setMessageType(MessageType.TCP_HEARTBEAT);
                synchronized (oos) {
                    oos.writeObject(heartbeat);
                    oos.flush();
                }
                heartbeatFailureCount = 0; // 重置失败计数
                System.out.println("发送TCP心跳包");
            } catch (IOException e) {
                heartbeatFailureCount++;
                System.err.println("心跳发送失败 (" + heartbeatFailureCount + "/" + MAX_HEARTBEAT_FAILURES + ")");

                if (heartbeatFailureCount >= MAX_HEARTBEAT_FAILURES) {
                    System.err.println("心跳失败次数过多，断开连接");
                    disconnect();
                }
            }
        }
    }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
}

private void stopHeartbeat() {
    if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
        heartbeatTask.cancel(false);
    }
}

// 改进的连接方法
public synchronized boolean connect() {
    if (isConnected()) {
        return true;
    }

    try {
        socket = new Socket(TCP_HOST, TCP_PORT);
        socket.setSoTimeout(READ_TIMEOUT);
        socket.setKeepAlive(true);

        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());

        connected = true;
        running = true;

        // 启动消息接收线程
        startMessageReceiver();

        // 启动心跳
        startHeartbeat();

        return true;

    } catch (IOException e) {
        System.err.println("TCP连接失败: " + e.getMessage());
        disconnect();
        return false;
    }
}

// 改进的断开连接方法
public synchronized void disconnect() {
    running = false;
    connected = false;

    // 停止心跳
    stopHeartbeat();

    if (messageReceiverThread != null && messageReceiverThread.isAlive()) {
        messageReceiverThread.interrupt();
    }

    try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (socket != null) socket.close();
        responseHandlers.clear();
    } catch (IOException e) {
        System.err.println("断开连接时发生错误: " + e.getMessage());
    }
}

// 添加连接检查和重连方法
public boolean ensureConnected() {
    if (isConnected()) {
        return true;
    }
    return connect();
}
```

### 3. 改进消息接收处理

```java
private void handleResponse(Message response) {
    String taskId = response.getSyncTaskId();
    if (taskId != null && responseHandlers.containsKey(taskId)) {
        responseHandlers.remove(taskId).complete(response);
    }

    // 处理心跳响应
    if (response.getMessageType() == MessageType.TCP_HEARTBEAT_ACK) {
        System.out.println("收到心跳响应");
        heartbeatFailureCount = 0;
        return;
    }

    // 处理其他响应消息...
}
```

### 4. 添加连接监控和重连机制

```java
// 连接健康检查
private void checkConnectionHealth() {
    if (!isConnected()) {
        System.out.println("检测到连接断开，尝试重连...");
        reconnectWithRetry(3);
    }
}

// 重连机制with退避
private boolean reconnectWithRetry(int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        if (connect()) {
            return true;
        }
        // 指数退避：1s, 2s, 4s...
        try {
            Thread.sleep((1 << i) * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
    return false;
}
```

这个计划将彻底解决当前TCPClient的问题，为大数据传输提供稳定高效的长连接服务。