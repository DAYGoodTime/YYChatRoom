# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 交互规则

1. **语言要求**: 在所有交流中请使用中文
2. **测试策略**: 如果没有明确要求进行测试，请勿主动编写或运行测试代码，测试工作由用户自行完成
3. **编写注意事项**: 在实现功能的时候，注意客户端与服务端的代码同步。

## 项目概述

YYChatRoom是一个基于Java的聊天室应用程序，采用客户端-服务器架构。该项目实现了TCP和UDP两种通信协议，支持文本聊天、文件传输、头像管理等功能。

### 核心组件

- **Server模块**: 多线程服务器，处理客户端连接、用户认证、消息转发
- **LoginClient模块**: Java Swing GUI客户端，提供用户界面

### 技术栈

- **Java版本**: JDK 8
- **数据库**: MySQL (数据库名: yychat2022s)
- **GUI框架**: Java Swing
- **通信协议**: UDP和TCP
- **依赖库**:
  - MySQL Connector/J 8.4.0 (Server端)
  - Hutool 5.8.41 (通用工具库)
- **开发环境**: IntelliJ IDEA

## 快速开始

### 环境准备

1. **安装MySQL**并创建数据库:
   ```sql
   CREATE DATABASE yychat2022s;
   ```

2. **配置数据库连接** (在`Server/src/com/yychat/control/DBUtil.java`中):
   ```java
   private static final String db_url = "jdbc:mysql://localhost:3306/yychat2022s";
   private static final String db_user = "root";
   private static final String db_pass = "kel123";
   ```

### 启动应用

**服务器端**:
```bash
cd Server/src
javac -cp "lib/mysql-connector-j-8.4.0.jar:lib/hutool-all-5.8.41.jar" com/yychat/view/StartServerUDP.java
java -cp ".:lib/mysql-connector-j-8.4.0.jar:lib/hutool-all-5.8.41.jar" com.yychat.view.StartServerUDP
```

**客户端**:
```bash
cd LoginClient/src
javac -cp "lib/hutool-all-5.8.41.jar" com/yychat/view/ClientMain.java
java -cp ".:lib/hutool-all-5.8.41.jar" com.yychat.view.ClientMain
```

### 主要入口类

- **服务器**: `com.yychat.view.StartServerUDP`
- **客户端**: `com.yychat.view.ClientMain` (启动`ClientLoginUDP`)

## 代码架构

### 包结构

```
com.yychat
├── model/          # 数据模型
│   ├── Message.java           # 消息实体 (实现序列化)
│   ├── MessageType.java       # 消息类型常量接口
│   ├── User.java              # 用户模型
│   └── FriendType.java        # 好友关系类型
├── control/        # 业务控制层
│   ├── DBUtil.java            # 数据库连接工具
│   ├── AvatarFileManager.java # 头像文件管理
│   ├── YYchatServerUDP.java   # UDP服务器控制
│   └── ServerReceiverThreadUDP.java
├── view/           # 视图层 (GUI)
│   ├── StartServerUDP.java    # 服务器启动界面
│   ├── ClientMain.java        # 客户端主入口
│   ├── ClientLoginUDP.java    # UDP登录窗口
│   ├── FriendList.java        # 好友列表窗口
│   ├── FriendChat.java        # 聊天窗口
│   └── AvatarSelector.java    # 头像选择器
└── (tcp/udp/)      # 通信层
```

### 核心通信协议

项目使用`Message`对象作为通信载体，通过`MessageType`接口定义消息类型：

- `LOGIN_VALIDATE_SUCCESS` (1): 登录成功
- `COMMON_CHAT_MESSAGE` (3): 聊天消息
- `REQUEST_ONLINE_FRIENDS` (4): 请求在线好友
- `RESPONSE_FRIEND_LIST` (13): 返回好友列表
- `REQUEST_AVATAR` (22): 请求头像
- `UPDATE_AVATAR` (24): 更新头像
- 等等...

### 线程模型

- **服务器**: 每个客户端连接在独立的`ServerReceiverThreadUDP`中处理
- **客户端**: 使用`ExecutorService`线程池(固定10线程)处理并发

### 数据库设计

核心表结构:

- `user`表: 存储用户信息(username, password等)
- `userRelation`表: 存储好友关系

数据库连接配置硬编码在`DBUtil.java`中，需要手动修改。

## 主要功能

### 1. 用户管理
- 用户注册/登录验证
- 好友添加/删除
- 在线状态管理

### 2. 消息系统
- 文本消息传输
- 消息类型标记
- 时间戳记录(LocalDateTime)

### 3. 头像功能
- 头像上传/下载
- 二进制数据存储在`Message`对象中
- 头像文件管理器(`AvatarFileManager`)

### 4. 文件传输
- 基于UDP的文件传输
- 大文件支持(通过分片)

## 关键配置

### 端口配置
- 默认UDP端口: 3456
- 数据库端口: 3306

### 线程池配置
- 客户端线程池大小: 10
- 服务器: 无限制(每连接一线程)

## 开发注意事项

1. **数据库密码硬编码**: `DBUtil.java`中的数据库密码需要根据实际环境修改

2. **UDP协议**: 项目主要使用UDP协议，需要考虑数据包丢失和乱序问题

3. **序列化兼容性**: `Message`类实现了`Serializable`，注意版本兼容性

4. **GUI线程**: Swing组件操作必须在EDT线程中进行

5. **资源管理**: 头像文件存储在服务器本地，需要适当清理机制

## 项目文件说明

### 核心文件
- `Server/src/com/yychat/control/DBUtil.java`: 数据库连接管理
- `Server/src/com/yychat/control/AvatarFileManager.java`: 头像文件管理
- `LoginClient/src/com/yychat/view/ClientMain.java`: 客户端入口
- `Server/src/com/yychat/view/StartServerUDP.java`: 服务器入口

### 配置文件
- `Server/Server.iml`: IntelliJ模块配置
- `LoginClient/LoginClient.iml`: IntelliJ模块配置
- `Server/lib/`: 服务器依赖库
- `LoginClient/lib/`: 客户端依赖库

### 文档文件
- `GEMINI.md`: 项目概述和运行说明(中文)
- `AVATAR_IMPLEMENTATION_SUMMARY.md`: 头像功能实现总结
- `FileTransfer_Design.md`: 文件传输设计文档
- `UDP_TEST_GUIDE.md`: UDP测试指南

## 依赖库位置

- **Server模块**: `Server/lib/mysql-connector-j-8.4.0.jar`, `Server/lib/hutool-all-5.8.41.jar`
- **LoginClient模块**: `LoginClient/lib/hutool-all-5.8.41.jar`

## 故障排除

1. **数据库连接失败**: 检查MySQL服务状态，确认数据库名、用户名、密码
2. **端口占用**: 检查3456端口是否被其他程序占用
3. **编译错误**: 确保JDK8版本兼容，检查类路径设置
4. **GUI显示问题**: 确保支持图形界面(X11/Wayland)

## 扩展建议

1. **添加测试套件**: 当前无单元测试
2. **配置外部化**: 将数据库配置移到配置文件
3. **日志系统**: 添加日志记录功能
4. **错误处理**: 完善异常处理机制
5. **消息加密**: 考虑添加消息加密功能
