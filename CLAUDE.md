# CLAUDE.md

本文档为Claude Code（claude.ai/code）在该代码仓库中工作时提供指导。

## 开发要求

1. **交流语言**：在所有交流过程中使用中文
2. **测试限制**：在执行任何命令或任务时都不要进行测试或编写相关的测试代码
3. **C/S架构同步**：这是一个客户端-服务器架构的项目，在实现功能时需要主要两端（客户端和服务器）的实现是否同步

## 项目概述

YYChatRoom是一个基于Java的聊天应用程序，采用客户端-服务器架构，通过UDP进行消息传输、通过TCP进行文件传输。该应用程序提供实时聊天功能，支持好友管理、头像显示和Swing图形界面。

## 架构

### 高层次结构

应用程序采用分层架构：

1. **客户端层**（`com.yychat.client`）
   - 入口点：`ClientMain.java`
   - 通过`UDPClientConnection`处理UDP连接
   - 业务逻辑服务层
   - Swing图形界面视图

2. **服务器层**（`com.yychat.server`）
   - 双协议支持：
     - UDP服务器（端口5678）：消息路由和用户管理
     - TCP服务器（端口3457）：文件传输处理
   - 服务器端服务层
   - 持久化数据库工具

3. **通用层**（`com.yychat.common`）
   - 共享模型：`User`、`Message`、`ServiceResponse`
   - 消息类型和常量定义
   - 协议规范

### 核心组件

**通信协议：**
- **UDP（端口5678）**：聊天消息、用户认证、好友列表管理的主要消息协议
- **TCP（端口3457）**：文件上传/下载操作及确认系统

**数据库：**
- MySQL数据库：`yychat2022s`
- JDBC URL：`jdbc:mysql://localhost:3306/yychat2022s`
- 凭据：`root` / `kel123`
- 核心表：`user`、`userRelation`

**依赖库：**
- Hutool 5.8.41（`lib/hutool-all-5.8.41.jar`）：JSON处理和工具类
- MySQL Connector/J 8.4.0（`lib/mysql-connector-j-8.4.0.jar`）：数据库连接

## 开发

### 构建与运行命令

这是一个IntelliJ IDEA项目。支持的开发流程：

**编译项目：**
```bash
# 使用javac直接编译
javac -cp "lib/*:out/production/ChatRoom" -d out/production/ChatRoom ChatRoom/src/com/yychat/**/*.java

# 或者在IntelliJ IDEA中打开，使用 Build > Build Project
```

**运行服务器：**
```bash
# 服务器入口点
java -cp "lib/*:out/production/ChatRoom" com.yychat.server.view.StartServer
```

**运行客户端：**
```bash
# 客户端入口点
java -cp "lib/*:out/production/ChatRoom" com.yychat.client.ClientMain
```

**数据库设置：**
确保MySQL正在运行且`yychat2022s`数据库存在，包含以下表：

```sql
-- 用户表
CREATE TABLE user (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(50) NOT NULL,
    avatar_path VARCHAR(100) DEFAULT '0.jpg'
);

-- 用户关系表（好友、拉黑用户等）
CREATE TABLE userRelation (
    masterUser VARCHAR(50),
    slaveUser VARCHAR(50),
    relation INT
);
```

### 测试单个组件

**测试特定客户端功能：**
```bash
# 使用特定用户名/密码测试
java -cp "lib/*:out/production/ChatRoom" com.yychat.client.ClientMain testuser testpass
```

**测试数据库连接：**
```bash
# 验证数据库连接
java -cp "lib/*:out/production/ChatRoom" com.yychat.server.util.DBUtil
```

### 项目结构

```
ChatRoom/
├── src/                          # 源代码
│   └── com/yychat/
│       ├── client/               # 客户端代码
│       │   ├── ClientMain.java   # 入口点
│       │   ├── udp/              # UDP连接处理
│       │   ├── service/          # 客户端服务（用户、消息、头像）
│       │   ├── view/             # Swing UI组件
│       │   └── util/             # 客户端工具类
│       ├── server/               # 服务器端代码
│       │   ├── tcp/              # TCP服务器（文件传输）
│       │   ├── udp/              # UDP服务器和处理器
│       │   ├── service/          # 服务器服务
│       │   └── util/             # DBUtil和服务器工具类
│       └── common/               # 共享模型和常量
│           └── model/            # User、Message、ServiceResponse等
├── lib/                          # 外部JAR依赖
├── res/                          # GUI资源（图标、图像）
│   ├── DefaultAvatar/            # 默认头像图像
│   └── UserAvatar/               # 用户上传的头像
├── out/production/ChatRoom/      # 编译后的类文件
└── ChatRoom.iml                  # IntelliJ IDEA模块文件
```

### 消息协议

应用程序使用灵活的消息系统（定义在`MessageType.java`中）：

**客户端到服务器消息：**
- `USER_LOGIN_REQUEST` (1)：用户认证
- `USER_SIGNUP_REQUEST` (2)：新用户注册
- `COMMON_CHAT_MESSAGE` (3)：聊天消息
- `REQUEST_ONLINE_FRIENDS` (4)：获取在线好友列表
- `REQUEST_FRIEND_LIST` (7)：获取所有好友
- `USER_ADD_NEW_FRIEND` (8)：添加新好友
- `REQUEST_AVATAR_PATH` (10)：请求用户头像
- `REQUEST_USER_INFO` (11)：获取用户资料信息

**服务器到客户端消息：**
- `NEW_ONLINE_FRIEND` (5)：通知好友上线
- `NEW_OFFLINE_FRIEND` (6)：通知好友下线
- `REQUEST_UNK_USER_LIST` (9)：非好友用户列表

**TCP消息（文件传输）：**
- `TCP_FILE_UPLOAD` (101)：文件上传请求
- `TCP_FILE_DOWNLOAD` (102)：文件下载请求
- `TCP_ACK` (100)：确认

## 关键实现细节

**客户端架构：**
- `UDPClientConnection`处理所有UDP通信，支持异步/同步消息
- 线程池处理后台操作
- Swing事件分发线程处理UI更新
- 消息构建器模式构造消息

**服务器架构：**
- `YYChatUDPServer`管理UDP连接和消息路由
- `YYChatTCPServer`处理文件传输连接
- 不同消息类型使用独立的处理器类
- 内存中维护在线用户的地址映射

**数据库操作：**
- 通过`DBUtil`单例模式实现连接池
- 使用PreparedStatement防止SQL注入
- 支持用户关系（好友、拉黑用户等）

**UI特性：**
- 头像选择器和自定义头像支持
- 好友列表显示在线/离线状态
- 聊天窗口支持文件附件
- 登录/注册界面

## 重要文件

- `ClientMain.java`：客户端入口点，管理全局状态
- `UDPClientConnection.java`：核心UDP通信处理器
- `Message.java`：支持附件的灵活消息模型
- `DBUtil.java`：数据库连接和操作
- `YYChatUDPServer.java`：服务器UDP消息处理
- `YYChatTCPServer.java`：服务器文件传输处理
- `Constant.java`：应用程序范围的常量（头像路径等）

## 常见任务

**添加新消息类型：**
1. 在`MessageType.java`中定义常量
2. 在适当的服务/处理器类中添加处理逻辑
3. 在客户端对应的服务中更新处理

**添加数据库字段：**
1. 更新`User.java`模型类
2. 更新`DBUtil.java`数据库操作
3. 如需要，更新相关UI组件

**修改UI：**
- 所有视图都在`client/view/`包中
- 使用标准Swing组件和自定义样式
- 图像存储在`res/`目录中
