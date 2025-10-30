# 任务02：修改网络传输部分，从面向连接结构变成无连接结构

## 目标概述
将现有的基于TCP的聊天室系统改造为基于UDP的无连接网络传输系统。

## 现状分析

### 现有架构特征：
1. **面向连接**：使用ServerSocket和Socket建立TCP连接
2. **可靠传输**：TCP协议保证数据传输的可靠性
3. **独立线程**：每个客户端连接使用独立的ServerReceiverThread
4. **端口配置**：服务器监听5678端口，客户端连接3456端口
5. **消息序列化**：通过ObjectInputStream/ObjectOutputStream传输Message对象

### 需要变更的技术组件：
- ServerSocket → DatagramSocket（UDP套接字）
- Socket → DatagramPacket（UDP数据包）
- ObjectOutputStream → ByteArrayOutputStream（字节数组输出）
- ObjectInputStream → ByteArrayInputStream（字节数组输入）

## 执行计划

### 阶段1：架构改造准备

#### 1.1 分析现有网络传输架构和Message类结构 ✅
- **已完成**：分析了现有TCP架构的组件和数据流
- **关键发现**：
  - 现有系统使用Socket连接管理用户会话
  - Message类使用Java序列化进行网络传输
  - 服务器通过userSocketMap维护用户与Socket的映射关系

### 阶段2：创建UDP版本的服务器组件

#### 1.2 创建UDP版本的Server类（ServerUDP.java） 🔄
- **目标**：创建基于UDP的服务器类，替换现有的YYchatServer
- **关键变更**：
  - 使用DatagramSocket替代ServerSocket
  - 使用DatagramPacket接收和发送数据
  - 重新设计用户会话管理（HashMap<String, InetSocketAddress>）
  - 端口保持5678
- **实现要点**：
  ```java
  private static final HashMap<String, InetSocketAddress> userAddressMap = new HashMap<>();
  private DatagramSocket datagramSocket;
  private ExecutorService threadPool = Executors.newFixedThreadPool(10);
  ```

#### 1.3 修改Message类以支持UDP传输特性
- **目标**：确保Message类在UDP环境下的兼容性
- **验证内容**：
  - Message类实现了Serializable接口，适合网络传输
  - 需要考虑UDP数据包大小限制
  - 可能需要添加序列化和反序列化方法

#### 1.4 修改客户端Client类为UDP版本（ClientUDP.java）
- **目标**：创建基于UDP的客户端连接类
- **关键变更**：
  - 使用DatagramSocket替代Socket
  - 客户端使用随机端口或固定端口（如3456）
  - 实现UDP消息发送和接收机制

#### 1.5 实现UDP消息发送和接收机制
- **发送机制**：
  - 将Message对象序列化为字节数组
  - 使用DatagramPacket发送数据包
- **接收机制**：
  - 使用DatagramPacket接收数据包
  - 将字节数组反序列化为Message对象
  - 启动接收线程处理消息

#### 1.6 更新消息处理逻辑以适应无连接架构
- **用户管理**：
  - 使用InetSocketAddress替代Socket管理用户地址
  - 更新getUserSocket()方法为getUserAddress()
  - 修改userSocketMap为userAddressMap
- **消息转发**：
  - 适应UDP无连接特性
  - 处理消息丢失和重复的情况

#### 1.7 测试UDP聊天功能并验证数据传输
- **测试场景**：
  - 单用户登录和注册
  - 双用户聊天消息传递
  - 多用户并发连接
  - 消息丢失率测试

## 技术挑战与解决方案

### 挑战1：UDP无连接特性
- **问题**：无法像TCP那样维护持久的连接状态
- **解决**：使用InetSocketAddress维护用户地址映射，每个数据包包含完整的地址信息

### 挑战2：UDP数据包大小限制
- **问题**：UDP数据包大小有限制（通常65,535字节）
- **解决**：Message对象较小，一般不会有此问题；如果需要传输大文件，可考虑分片

### 挑战3：UDP不可靠传输
- **问题**：UDP不保证消息可靠到达
- **解决**：
  - 对于登录验证等关键操作，可以实现简单的确认机制
  - 对于普通聊天消息，允许少量丢失

### 挑战4：线程安全
- **问题**：UDP接收需要持续监听，需要独立的接收线程
- **解决**：
  - 使用独立的UDPReceiveThread处理接收
  - 使用线程安全的Map管理用户地址
