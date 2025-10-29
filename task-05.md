# YYChatRoom用户自定义头像功能实施方案

## 项目概述

YYChatRoom是一个基于Java的客户端-服务器聊天室应用程序，支持多用户聊天、好友管理等功能。本文档详细说明如何在最小成本下为该系统添加用户自定义头像功能。

## 系统架构分析

### 当前架构概览

```
YYChatRoom/
├── Server/                    # Java服务器端
│   └── src/com/yychat/
│       ├── control/           # 控制层
│       │   ├── DBUtil.java    # 数据库操作
│       │   └── ServerReceiverThreadUDP.java  # UDP通信
│       ├── model/             # 数据模型
│       │   ├── User.java      # 用户模型
│       │   ├── Message.java   # 消息模型
│       │   └── FriendType.java # 好友类型
│       └── view/              # 视图层
├── LoginClient/               # Java客户端
│   ├── src/com/yychat/
│   │   ├── model/             # 客户端模型
│   │   ├── view/              # GUI组件
│   │   │   ├── FriendList.java    # 好友列表界面
│   │   │   └── FriendChat.java    # 聊天界面
│   │   ├── control/           # 客户端控制
│   │   └── tcp/               # TCP通信（已废弃）
│   └── res/                   # 资源文件（头像、图标）
└── Database: MySQL yychat2022s
```

### 核心组件分析

**1. 数据库结构**
- `user` 表：username, password
- `userRelation` 表：masterUser, slaveUser, relation (好友关系)
- `message` 表：from_user, to_user, content, sendtime

**2. 用户模型**
```java
class User {
    String userName;
    String password;
    // 只有基本的用户名和密码字段
}
```

**3. 消息传输机制**
- 协议：UDP（当前使用）
- 序列化：Java对象序列化
- JSON扩展：使用Hutool JSON支持复杂数据
- 消息类型：21种消息类型（登录、聊天、好友管理等）

**4. GUI组件**
- `FriendList`：好友列表，支持卡片布局，已实现图标加载机制
- `FriendChat`：聊天窗口，支持消息格式化显示
- 现有头像系统：已使用 `loadFriendIcon()`, `loadStrangerIcon()` 等方法

**5. 现有资源文件**
- 头像文件：`0.jpg` 到 `5.jpg`（6个默认头像）
- 图标文件：`duck2.gif`, `tortoise.gif`, `children.gif` 等
- 系统图标：`login.gif`, `register.gif`, `cancel.jpg` 等

## 实施方案

### 方案概述：基于文件系统的轻量级头像方案

**核心思路：**
- 使用文件系统存储头像文件（服务器端）
- 数据库添加avatar_path字段
- 利用现有UDP消息机制传输头像数据
- 复用现有的GUI图标加载逻辑

### 详细实施步骤

#### 阶段1：数据库结构扩展
**时间估算：1-2天**

**1.1 修改数据库结构** (已完成)

```sql
-- 添加头像路径字段
ALTER TABLE user ADD COLUMN avatar_path VARCHAR(255) DEFAULT '0.jpg';
```

**1.2 修改User.java模型类**
```java
public class User implements Serializable {
    String userName;
    String password;
    String avatarPath;  // 新增头像路径字段

    // getter/setter方法
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
}
```

**1.3 DBUtil.java新增方法**
```java
// 更新用户头像路径
public static boolean updateUserAvatar(String userName, String avatarPath) {
    String query = "UPDATE user SET avatar_path=? WHERE username=?";
    // 实现更新逻辑
}

// 获取用户头像路径
public static String getUserAvatar(String userName) {
    String query = "SELECT avatar_path FROM user WHERE username=?";
    // 实现查询逻辑
}
```

#### 阶段2：消息类型扩展
**时间估算：0.5天**

**在MessageType.java中添加：**
```java
String REQUEST_AVATAR = "22";        // 请求用户头像
String RESPONSE_AVATAR = "23";       // 返回头像数据
String UPDATE_AVATAR = "24";         // 更新头像
String AVATAR_UPLOAD_SUCCESS = "25"; // 头像上传成功
```

#### 阶段3：服务器端实现
**时间估算：1-2天**

**ServerReceiverThreadUDP.java新增：**
```java
private void handleRequestAvatar(Message message) {
    String userName = message.getContent();
    String avatarPath = DBUtil.getUserAvatar(userName);

    Message response = new Message();
    response.setMessageType(MessageType.RESPONSE_AVATAR);
    response.setReceiver(message.getSender());
    response.setContent(avatarPath);

    sendMessageToClient(clientAddress, response);
}

private void handleUpdateAvatar(Message message) {
    String userName = message.getSender();
    String avatarPath = message.getContent();

    // 保存头像文件
    // 更新数据库
    DBUtil.updateUserAvatar(userName, avatarPath);

    // 广播给所有好友
    broadcastAvatarUpdate(userName, avatarPath);
}
```

#### 阶段4：客户端实现
**时间估算：2-3天**

**4.1 FriendList.java增强**
- 修改 `createFriendLabel()` 方法，支持动态头像加载
- 添加头像更新监听器
- 实现头像缓存机制

**4.2 新增头像选择器**
```java
public class AvatarSelector extends JDialog {
    // 头像选择UI组件
    // 支持默认头像和自定义上传
}
```

#### 阶段5：网络传输优化
**时间估算：0.5天**

**UDP消息扩展：**
```java
// 在Message.java中添加
private byte[] avatarData;  // 头像二进制数据
private String avatarFileName; // 头像文件名

// getter/setter方法
```

### 技术实现细节

#### 1. 头像存储结构

```
Server/avatars/
├── day/          # 用户目录
│   └── avatar.jpg
├── alice/
│   └── custom_avatar.png
└── 0.jpg, 1.jpg, 2.jpg, 3.jpg, 4.jpg, 5.jpg  # 默认头像
```

#### 2. 头像传输流程

1. **头像请求**：客户端发送REQUEST_AVATAR消息
2. **头像响应**：服务器返回头像路径或头像数据
3. **头像显示**：客户端加载并显示头像
4. **头像更新**：客户端上传新头像，服务器保存并广播

#### 3. 客户端缓存策略

- **本地缓存**：客户端本地存储头像文件
- **内存缓存**：FriendList组件缓存当前可见头像
- **懒加载**：仅在需要时加载头像

## 修改文件列表

| 组件 | 文件数量 | 修改内容 | 预估时间 |
|------|----------|----------|----------|
| 数据库层 | 1 | DBUtil.java：添加3个方法<br>数据库迁移脚本 | 1-2天 |
| 模型层 | 2 | User.java：添加avatarPath字段<br>MessageType.java：添加5个常量 | 0.5天 |
| 服务器控制层 | 1-2 | ServerReceiverThreadUDP.java：添加2个消息处理方法<br>新增头像文件管理工具类 | 1-2天 |
| 客户端视图层 | 2-3 | FriendList.java：修改头像显示逻辑<br>新增AvatarSelector.java：头像选择对话框<br>FriendChat.java：可能需要显示聊天对象头像 | 2-3天 |
| 客户端控制层 | 1 | ClientReceiverThreadUDP.java：添加头像消息处理 | 1天 |

## 最小成本优化建议

### 1. 使用现有头像框架
- 复用FriendList中现有的 `loadFriendIcon()` 逻辑
- 保持原有的图标加载机制

### 2. 限制头像规格
- 统一头像为64x64像素
- 强制JPG格式
- 文件大小限制：<50KB

### 3. 渐进式实现策略

**第一阶段：默认头像选择**
- 仅支持从现有6个默认头像中选择
- 最小开发量：2-3天

**第二阶段：自定义头像上传**
- 支持用户上传自定义头像
- 文件压缩和验证

**第三阶段：高级功能**
- 头像编辑功能
- 动态头像支持

通过采用文件系统+数据库的轻量级方案，我们可以在不大幅改动现有架构的情况下，优雅地实现用户自定义头像功能，同时确保系统的稳定性和可扩展性。