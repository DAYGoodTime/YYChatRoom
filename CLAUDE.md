# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

YYChatRoom是一个基于Java的聊天室应用程序，采用C/S架构，使用UDP进行消息传输、TCP进行文件传输。该应用程序提供实时聊天、好友管理、头像显示和Swing GUI功能。

## 核心架构

### 分层架构
- **客户端层** (`com.yychat.client`) - GUI界面和业务逻辑
- **服务器层** (`com.yychat.server`) - UDP消息处理、TCP文件传输、数据库操作
- **通用层** (`com.yychat.common`) - 共享模型、常量定义、工具类

### 通信协议
- **UDP (端口5678)** - 聊天消息、用户认证、好友管理
- **TCP (端口3457)** - 文件传输、心跳机制
- **数据库** - MySQL `yychat2022s`

### 数据库表结构
- `user` - 用户基本信息（username, password, avatar_path）
- `userrelation` - 用户关系（好友、拉黑等，relation字段标识关系类型）
- `groups` - 群组信息（group_id, group_name, group_avatar_path）
- `group_members` - 群组成员关系（group_id, joiner, join_time）
- `message` - 个人聊天消息记录（sender, receiver, content, sendtime）
- `group_messages` - 群组消息记录（sender_name, group_id, content, time）

### 关键模型
- `Message` - 支持附件的序列化消息，支持JSON和同步/异步模式
- `User` - 用户信息模型
- `ServiceResponse` - 服务响应封装
- `MessageType` - 消息类型常量定义

## 常用开发命令

### 编译项目
```bash
# 使用javac编译所有Java文件
javac -cp "lib/*" -d out/production/ChatRoom src/com/yychat/**/*.java

# 在IntelliJ IDEA中：Build > Build Project
```

### 运行应用
```bash
# 启动服务器（需要先启动MySQL）
java -cp "lib/*:out/production/ChatRoom" com.yychat.server.view.StartServer

# 启动客户端
java -cp "lib/*:out/production/ChatRoom" com.yychat.client.ClientMain

# 带参数启动客户端（测试用）
java -cp "lib/*:out/production/ChatRoom" com.yychat.client.ClientMain testuser testpass
```

### 数据库操作
```bash
# 测试数据库连接
java -cp "lib/*:out/production/ChatRoom" com.yychat.server.util.DBUtil
```

## 重要组件说明

### 客户端核心
- `ClientMain.java` - 客户端入口，管理UDP/TCP连接和线程池
- `UDPClientConnection.java` - UDP通信核心，支持同步/异步消息
- `TCPClient.java` - TCP文件传输，带心跳机制
- `MainWindow.java` - 主界面框架

### 服务器核心
- `YYChatUDPServer.java` - UDP服务器，维护用户地址映射
- `YYChatTCPServer.java` - TCP文件服务器
- `StartServer.java` - 服务器启动入口，初始化所有服务
- `DBUtil.java` - 数据库连接单例，PreparedStatement防注入

### 消息系统
- 基于`Message`构建器模式构造消息
- 支持附件类型：`FILE`、`IMAGE`、`AVATAR`
- JSON消息支持（通过Hutool）
- 同步消息使用CompletableFuture + 任务ID

### UI架构
- 所有UI组件在`client/view/`包中
- 使用Swing标准组件
- 头像系统：默认头像`res/DefaultAvatar/`，用户头像`res/UserAvatar/`
- 好友列表实时更新在线状态

## 消息协议

### 客户端→服务器
- `USER_LOGIN_REQUEST` (1) - 用户登录
- `USER_SIGNUP_REQUEST` (2) - 用户注册
- `COMMON_CHAT_MESSAGE` (3) - 个人聊天消息
- `REQUEST_ONLINE_FRIENDS` (4) - 获取在线好友
- `REQUEST_FRIEND_LIST` (7) - 获取所有好友
- `USER_ADD_NEW_FRIEND` (8) - 添加好友
- `REQUEST_AVATAR_PATH` (10) - 请求头像
- `GROUP_CHAT_MESSAGE` (12) - 群组聊天消息（如果有群组功能扩展）

### 服务器→客户端
- `NEW_ONLINE_FRIEND` (5) - 好友上线通知
- `NEW_OFFLINE_FRIEND` (6) - 好友下线通知
- `REQUEST_UNK_USER_LIST` (9) - 非好友用户列表

### TCP消息
- `TCP_FILE_UPLOAD` (101) - 文件上传
- `TCP_FILE_DOWNLOAD` (102) - 文件下载
- `TCP_HEARTBEAT` (103) - 心跳检测

## 常见开发任务

### 添加新消息类型
1. 在`MessageType.java`中添加常量
2. 在对应处理器（`handler/`包）中添加处理逻辑
3. 客户端/服务器端同步更新

### 数据库字段变更
1. 更新相关模型类（`User.java`等）
2. 修改`DBUtil.java`中的SQL操作
3. 更新UI显示逻辑

### 文件传输扩展
- TCP相关代码在`client/tcp/`和`server/tcp/`
- 文件管理器：`FileManager.java`、`AvatarFileManager.java`
- 附件类型定义在`AttachmentType.java`

### 群组功能扩展
- 群组管理需要实现：创建群组、加入群组、群组消息处理
- 涉及表：`groups`、`group_members`、`group_messages`
- 群组消息类型：`GROUP_CHAT_MESSAGE` (12)
- 群组权限管理：通过`group_members`表的`joiner`字段控制成员资格

## 依赖库
- **Hutool 5.8.41** - JSON处理、工具类 (`lib/hutool-all-5.8.41.jar`)
- **MySQL Connector/J 8.4.0** - 数据库驱动 (`lib/mysql-connector-j-8.4.0.jar`)

## 项目结构关键目录
```
ChatRoom/
├── src/com/yychat/
│   ├── client/          # 客户端代码
│   ├── server/          # 服务器代码
│   └── common/          # 通用模型和工具
├── lib/                 # 外部JAR依赖
├── res/                 # GUI资源、头像图像
│   ├── DefaultAvatar/   # 默认头像
│   └── UserAvatar/      # 用户自定义头像
├── sql/                 # 数据库SQL文件
│   └── schema.sql       # 数据库表结构定义
└── data/                # 服务器数据文件
```

## 重要文件
- `ClientMain.java` - 客户端入口点，管理全局状态
- `UDPClientConnection.java` - 核心UDP通信处理器
- `YYChatUDPServer.java` - 服务器UDP消息处理和用户管理
- `Message.java` - 支持附件的灵活消息模型
- `DBUtil.java` - 数据库连接和操作
- `Constant.java` - 应用程序常量（路径配置等）

## 开发要点
- 所有GUI更新必须在Swing事件分发线程中执行
- UDP通信支持无连接，适合聊天消息的广播特性
- TCP用于文件传输，确保数据可靠性
- 使用CompletableFuture处理异步消息响应
- 线程池管理并发连接（客户端10线程，服务器10线程）
- 数据库操作使用PreparedStatement防止SQL注入
- 请记住在执行任务或编写功能的时候，不要添加测试代码或文档，任务完成后自行检查编译错误。
- 开发功能的时候，注意客户端和服务端是否同时实现完成了(尤其是两边的发送端和接收端)。