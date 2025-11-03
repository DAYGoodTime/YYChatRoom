# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

YYChatRoom是一个基于Java的聊天室应用程序，采用C/S架构，使用UDP进行消息传输、TCP进行文件传输。该应用程序提供实时聊天、好友管理、群组管理、头像显示和Swing GUI功能。项目已完成完整实现，支持单聊、群聊、文件传输等核心功能。

## 核心架构

### 分层架构
- **客户端层** (`com.yychat.client`) - Swing GUI界面、业务逻辑处理、用户交互
- **服务器层** (`com.yychat.server`) - UDP消息处理、TCP文件传输、数据库操作、用户状态管理
- **通用层** (`com.yychat.common`) - 共享模型、常量定义、工具类、通信协议

### 通信协议
- **UDP (端口5678)** - 聊天消息、用户认证、好友管理、群组操作
- **TCP (端口3457)** - 文件传输、心跳机制、群组管理、消息历史记录
- **数据库** - MySQL `yychat2022s`

### 数据库表结构
- `user` - 用户基本信息（username, password, avatar_path）
- `userrelation` - 用户关系（好友、拉黑等，relation字段标识关系类型）
- `groups` - 群组信息（group_id, group_name, group_avatar_path, creator_username, member_count）
- `group_members` - 群组成员关系（group_id, username, join_time, role）
- `message` - 个人聊天消息记录（sender, receiver, content, sendtime）
- `group_messages` - 群组消息记录（sender_name, group_id, content, time）

## 关键模型
- `Message` - 支持附件的序列化消息，支持JSON和同步/异步模式，构建器模式构造
- `User` - 用户信息模型，包含头像路径等属性
- `Group` - 群组模型，包含群组信息和成员管理
- `ServiceResponse` - 服务响应封装，统一响应格式
- `MessageType` - 消息类型常量定义，包含完整的消息协议

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
- `ClientMain.java` - 客户端入口，管理UDP/TCP连接、线程池（10线程）、当前用户状态和主窗口
- `UDPClientConnection.java` - UDP通信核心，支持同步/异步消息、CompletableFuture响应机制
- `TCPClient.java` - TCP文件传输，带心跳机制、文件管理器集成
- `MainWindow.java` - 主界面框架，整合好友列表、群组列表、聊天窗口

### 服务器核心
- `YYChatUDPServer.java` - UDP服务器，维护用户地址映射、消息路由、在线状态管理
- `YYChatTCPServer.java` - TCP文件服务器，处理文件上传下载、心跳检测、群组操作
- `StartServer.java` - 服务器启动入口，初始化数据库连接、文件管理器、UDP/TCP服务
- `DBUtil.java` - 数据库连接单例，PreparedStatement防注入、连接池管理

### 消息系统
- 基于`Message`构建器模式构造消息，支持链式调用
- 支持附件类型：`FILE`、`IMAGE`、`AVATAR`，泛型安全类型转换
- JSON消息支持（通过Hutool），结构化数据传递
- 同步消息使用CompletableFuture + 任务ID，超时和错误处理

### UI架构
- 所有UI组件在`client/view/`包中，使用Swing标准组件
- 聊天界面：`BaseChat.java`（抽象基类）、`FriendChat.java`（私聊）、`GroupChat.java`（群聊）
- 列表面板：`FriendListPanel.java`（好友列表）、`GroupListPanel.java`（群组列表）、`StrangerListPanel.java`（陌生人）
- 头像系统：默认头像`res/DefaultAvatar/`，用户头像`res/UserAvatar/`，群组头像`res/GroupAvatar/`
- 好友列表实时更新在线状态，图标和颜色区分

### 服务层架构
- **客户端服务** (`client/service/`):
  - `UserService.java` - 用户操作（登录、注册、信息获取）
  - `MessageService.java` - 消息发送接收、聊天记录管理
  - `GroupService.java` - 群组操作（创建、加入、退出、成员管理）
  - `AvatarService.java` - 头像上传、下载、管理
- **服务器服务** (`server/service/`):
  - `UserService.java` - 用户管理、认证、好友关系
  - `GroupService.java` - 群组CRUD操作、成员管理、权限控制
  - `FileManager.java` - 文件上传下载、存储管理
  - `AvatarFileManager.java` - 头像文件管理、缩略图生成

## 消息协议（完整版）

### 客户端→服务器
- `USER_LOGIN_REQUEST` (1) - 用户登录
- `USER_SIGNUP_REQUEST` (2) - 用户注册
- `COMMON_CHAT_MESSAGE` (3) - 个人聊天消息
- `REQUEST_ONLINE_FRIENDS` (4) - 获取在线好友
- `REQUEST_FRIEND_LIST` (7) - 获取所有好友
- `USER_ADD_NEW_FRIEND` (8) - 添加好友
- `REQUEST_AVATAR_PATH` (10) - 请求头像
- `REQUEST_USER_INFO` (11) - 请求好友详细信息

#### 群组管理消息
- `GROUP_JOIN` (22) - 加入群组
- `GROUP_LEAVE_REQUEST` (24) - 退出群组请求
- `GROUP_INFO_UPDATE` (26) - 群组信息更新
- `GROUP_DELETE_REQUEST` (28) - 删除群组请求
- `GROUP_ADD_MEMBER` (30) - 添加群组成员
- `GROUP_REMOVE_MEMBER` (32) - 移除群组成员
- `GROUP_TRANSFER_OWNER` (34) - 转让群主
- `GROUP_MEMBERS_REQUEST` (36) - 获取群组成员列表
- `USER_GROUPS_REQUEST` (38) - 获取用户群组列表
- `GROUP_SEARCH_REQUEST` (40) - 搜索群组

### 服务器→客户端
- `NEW_ONLINE_FRIEND` (5) - 好友上线通知
- `NEW_OFFLINE_FRIEND` (6) - 好友下线通知
- `REQUEST_UNK_USER_LIST` (9) - 非好友用户列表

#### 群组响应消息
- `GROUP_CHAT_MESSAGE` (12) - 群组聊天消息
- `GROUP_LEAVE_RESPONSE` (25) - 退出群组响应
- `GROUP_INFO_UPDATE_RESPONSE` (27) - 群组信息更新响应
- `GROUP_DELETE_RESPONSE` (29) - 删除群组响应
- `GROUP_ADD_MEMBER_RESPONSE` (31) - 添加成员响应
- `GROUP_REMOVE_MEMBER_RESPONSE` (33) - 移除成员响应
- `GROUP_TRANSFER_OWNER_RESPONSE` (35) - 转让群主响应
- `GROUP_MEMBERS_RESPONSE` (37) - 群组成员列表响应
- `USER_GROUPS_RESPONSE` (39) - 用户群组列表响应
- `GROUP_SEARCH_RESPONSE` (41) - 搜索群组响应
- `GROUP_CHAT_MESSAGE_RESPONSE` (42) - 群组聊天消息响应

### TCP消息
- `TCP_ACK` (100) - TCP处理确认
- `TCP_FILE_UPLOAD` (101) - 文件上传
- `TCP_FILE_DOWNLOAD` (102) - 文件下载
- `TCP_HEARTBEAT` (103) - 心跳检测
- `TCP_HEARTBEAT_ACK` (104) - 心跳响应
- `TCP_GROUP_SAVE` (120) - 保存群组请求（更新或创建）
- `TCP_USER_MESSAGE_REQUEST` (121) - 获取用户聊天记录
- `TCP_GROUP_MESSAGE_REQUEST` (122) - 获取群组聊天记录

## 文件结构

```
ChatRoom/
├── src/com/yychat/
│   ├── client/              # 客户端代码
│   │   ├── service/         # 客户端业务服务
│   │   ├── tcp/             # TCP通信相关
│   │   ├── udp/             # UDP通信和消息处理
│   │   │   └── handler/     # 消息处理器
│   │   ├── util/            # 客户端工具类
│   │   └── view/            # GUI界面组件
│   │       ├── chat/        # 聊天窗口
│   │       └── listpanel/   # 列表面板
│   ├── server/              # 服务器代码
│   │   ├── service/         # 服务器业务服务
│   │   ├── tcp/             # TCP服务器和处理器
│   │   ├── udp/             # UDP服务器和处理器
│   │   │   └── handler/     # 消息处理器
│   │   ├── util/            # 服务器工具类
│   │   └── view/            # 服务器视图（启动界面）
│   └── common/              # 通用模块
│       ├── model/           # 数据模型
│       └── util/            # 通用工具类
├── lib/                     # 外部JAR依赖
│   ├── hutool-all-5.8.41.jar      # JSON处理、工具类
│   ├── mysql-connector-j-8.4.0.jar # 数据库驱动
│   └── webp-imageio-0.1.6.jar     # WebP图像支持
├── res/                     # GUI资源、头像图像
│   ├── DefaultAvatar/       # 默认头像
│   ├── UserAvatar/          # 用户自定义头像
│   └── GroupAvatar/         # 群组头像
├── sql/                     # 数据库SQL文件
│   └── schema.sql           # 数据库表结构定义
├── data/                    # 服务器数据文件
│   └── server/              # 服务器端文件存储
│       └── UserMessage/     # 用户消息记录
└── docs/                    # 项目文档
    ├── TCP-README.md        # TCP通信框架文档
    ├── group-impl.md        # 群组功能实现文档
    └── AVATAR_*.md          # 头像系统文档
```

## 依赖库
- **Hutool 5.8.41** - JSON处理、工具类、日期操作 (`lib/hutool-all-5.8.41.jar`)
- **MySQL Connector/J 8.4.0** - 数据库驱动 (`lib/mysql-connector-j-8.4.0.jar`)
- **WebP ImageIO 0.1.6** - WebP图像格式支持 (`lib/webp-imageio-0.1.6.jar`)

## 重要文件
- `ClientMain.java` - 客户端入口点，管理全局状态（当前用户、主窗口、UDP/TCP连接）
- `UDPClientConnection.java` - 核心UDP通信处理器，同步/异步消息管理
- `YYChatUDPServer.java` - 服务器UDP消息处理和用户管理
- `Message.java` - 支持附件的灵活消息模型，构建器模式
- `DBUtil.java` - 数据库连接和操作，单例模式
- `Constant.java` - 应用程序常量（路径配置等）
- `MainWindow.java` - 主界面框架，整合所有UI组件

## 开发要点

### 架构原则
- **分层清晰** - 客户端、服务端、公共模块独立，各层职责明确
- **消息驱动** - 基于消息类型的处理器模式，易于扩展新功能
- **多线程安全** - Swing事件分发线程、后台线程池、线程安全集合
- **异步处理** - CompletableFuture响应机制，超时和错误处理

### 消息处理流程
1. **客户端发送** - 构建Message对象，设置消息类型和数据
2. **UDP传输** - 序列化消息，通过UDP发送
3. **服务器接收** - 反序列化消息，分发到对应处理器
4. **业务处理** - 处理器执行逻辑，操作数据库或内存状态
5. **响应返回** - 构造响应消息，通过UDP返回客户端
6. **客户端处理** - 接收响应，更新UI状态或处理结果

### GUI更新规则
- **Swing线程安全** - 所有GUI更新必须在Swing事件分发线程中执行
- **线程协调** - 使用SwingUtilities.invokeLater()协调UI更新
- **异步消息处理** - 后台线程接收消息，主线程更新UI

### 群组功能实现
- **创建群组** - TCP_GROUP_SAVE，创建群组记录和成员关系
- **加入群组** - GROUP_JOIN，更新成员列表和在线状态
- **群组消息** - GROUP_CHAT_MESSAGE，广播给所有在线成员
- **权限管理** - 通过group_members表的role字段控制权限（owner/admin/member）

### 文件传输机制
- **TCP连接** - 独立TCP连接处理文件传输，避免阻塞UDP消息
- **心跳检测** - 定期心跳包维持连接，超时自动断开
- **文件管理** - 统一的文件管理器，支持上传、下载、删除、缩略图生成
- **路径安全** - 相对路径存储，防止目录遍历攻击

### 数据库最佳实践
- **连接管理** - DBUtil单例，统一连接管理和错误处理
- **SQL防注入** - 所有SQL使用PreparedStatement
- **事务处理** - 重要操作使用事务保证数据一致性
- **索引优化** - 用户名、关系表、群组ID等关键字段建立索引

## 常见开发任务

### 添加新消息类型
1. 在`MessageType.java`中添加常量定义
2. 在对应处理器（`handler/`包）中添加处理逻辑
3. 客户端/服务器端同步更新发送和接收逻辑
4. 测试消息传递和数据处理

### 数据库字段变更
1. 更新相关模型类（`User.java`、`Group.java`等）
2. 修改`DBUtil.java`中的SQL操作
3. 更新数据库schema
4. 更新UI显示逻辑和数据绑定

### 群组功能扩展
- **群组权限** - 扩展role字段支持更多角色
- **群组公告** - 在groups表添加公告字段
- **群组设置** - 群组名称、头像、描述更新
- **禁言功能** - 在group_members表添加禁言状态字段

### 文件传输扩展
- TCP相关代码在`client/tcp/`和`server/tcp/`
- 文件管理器：`FileManager.java`、`AvatarFileManager.java`
- 附件类型定义在`AttachmentType.java`
- 支持断点续传、大文件分片传输

### UI组件扩展
- **聊天界面** - 扩展BaseChat支持更多消息类型（文件、表情、图片）
- **列表优化** - 支持搜索、分组、排序功能
- **主题系统** - 实现深色模式、自定义主题
- **通知系统** - 消息提醒、好友动态通知

## 测试和调试

### 常用调试命令
```bash
# 检查编译错误
javac -cp "lib/*" -d out/production/ChatRoom src/com/yychat/**/*.java

# 运行数据库连接测试
java -cp "lib/*:out/production/ChatRoom" com.yychat.server.util.DBUtil

# 测试消息发送
java -cp "lib/*:out/production/ChatRoom" com.yychat.client.ClientMain testuser testpass
```

### 日志输出
- 客户端和服务端都有详细的控制台日志输出
- 消息发送接收会有日志记录
- 数据库操作会有SQL日志（如果启用）
- TCP连接状态和心跳检测有实时日志

### 常见问题排查
1. **数据库连接失败** - 检查MySQL服务、数据库名、用户名密码
2. **UDP消息无法发送** - 检查端口占用、防火墙设置
3. **文件传输失败** - 检查TCP端口、文件权限、磁盘空间
4. **UI界面卡顿** - 检查是否在非Swing线程更新UI
5. **群组功能异常** - 检查group_members表数据和权限字段

## 性能优化要点

### 并发处理
- 线程池大小：客户端10线程，服务器10线程（可配置）
- 消息队列：UDP消息使用阻塞队列，TCP使用线程池
- 连接池：数据库连接池管理，避免频繁创建销毁

### 内存管理
- 消息对象复用：减少频繁的Message对象创建
- 图片缓存：头像和聊天图片使用缓存机制
- 定期清理：离线用户数据、历史消息定期清理

### 网络优化
- 消息压缩：JSON消息可以使用压缩算法
- 连接复用：TCP连接复用减少握手开销
- 批量处理：批量数据库操作减少IO次数

## 注意事项
- 开发功能时，务必同步实现客户端和服务端的发送和接收逻辑
- 所有GUI更新必须在Swing事件分发线程中执行
- 数据库操作要使用PreparedStatement防止SQL注入
- 文件操作要考虑权限和安全问题
- 群组操作要验证用户权限和群组状态
- 测试时注意客户端和服务端的版本兼容性