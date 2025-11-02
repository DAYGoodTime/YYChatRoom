# YYChatRoom群组功能实现计划

## 📋 项目概述

基于对YYChatRoom项目的深入分析，本文档详细规划了群组功能（GroupService）的完整实现方案。群组功能将支持群组创建、成员管理、信息更新和群组聊天等核心功能。

## 🎯 实现目标

**核心功能清单：**
- ✅ 群组创建（GroupService.createGroup）
- ✅ 群组成员管理（添加/移除成员）
- ✅ 群组信息更新（群组名称、头像）
- ✅ 群组消息传输（文本、图片、文件）
- ✅ 群组权限管理（群主权限控制）

## 📊 当前项目状态分析

### ✅ 已有基础架构
1. **数据库结构完整** - 群组相关3个表已存在：
   - `groups` - 群组信息表
   - `group_members` - 群组成员关系表
   - `group_messages` - 群组消息记录表

2. **消息类型预定义** - 部分群组消息类型已存在：
   ```java
   ChatMessageType.GroupChatPainText(2, "GroupChatPainText")
   ChatMessageType.GroupChatFile(3, "GroupChatFile")
   ```

3. **UI框架预留** - MainWindow中已有群组面板入口

### ⚠️ 需要修复的问题
1. **数据库字段拼写错误** - `group_messages.grou_id` 应为 `group_id`

## 🏗️ 实现架构设计

### 1. 分层架构模式

```
┌─────────────────────────────────────────┐
│  客户端层 (client)                       │
├─────────────────────────────────────────┤
│  - GUI界面 (view)                       │
│  - 客户端服务 (service)                  │
│  - 消息处理 (handler)                    │
├─────────────────────────────────────────┤
│  服务器层 (server)                       │
├─────────────────────────────────────────┤
│  - 服务器服务 (service)                  │
│  - 消息处理 (handler)                    │
│  - UDP通信处理                           │
├─────────────────────────────────────────┤
│  通用层 (common)                         │
├─────────────────────────────────────────┤
│  - 模型类 (model)                        │
│  - 消息类型 (MessageType)                │
│  - 数据库工具 (DBUtil)                   │
└─────────────────────────────────────────┘
```

### 2. 服务层架构模式

遵循现有用户管理模式：
- **服务类** - 业务逻辑封装
- **处理器类** - 消息处理和路由
- **工具类** - 数据库操作和通信

## 📋 详细实施计划

### 第一阶段：基础准备 (步骤1-3)

#### 步骤1：修复数据库字段拼写错误
**文件**: `sql/schema.sql`
```sql
-- 修复前
CREATE TABLE group_messages (
    grou_id int not null  -- 拼写错误
);

-- 修复后
CREATE TABLE group_messages (
    group_id int not null  -- 正确字段名
);
```

#### 步骤2：创建群组模型类
**文件列表**:
- `src/com/yychat/common/model/Group.java`
- `src/com/yychat/common/model/GroupMember.java`
- `src/com/yychat/common/model/GroupInfo.java` (如果需要)

**Group.java 设计**:
```java
public class Group {
    private int groupId;
    private String groupName;
    private String groupAvatarPath;
    private String creatorUsername;
    private Date createTime;
    private List<GroupMember> members;  // 成员列表

    // Builder模式构建
    public static class Builder {
        // 构建逻辑
    }
}
```

**GroupMember.java 设计**:
```java
public class GroupMember {
    private long id;
    private int groupId;
    private String username;
    private Date joinTime;
    private String role;  // member, admin, owner
}
```

#### 步骤3：扩展DBUtil工具类
**新增方法**:
```java
public class DBUtil {
    // 群组CRUD操作
    public static boolean createGroup(Group group) { }
    public static List<Group> getUserGroups(String username) { }
    public static Group getGroupById(int groupId) { }
    public static boolean updateGroupInfo(Group group) { }
    public static boolean deleteGroup(int groupId) { }

    // 成员管理
    public static boolean addGroupMember(int groupId, String username) { }
    public static boolean removeGroupMember(int groupId, String username) { }
    public static List<GroupMember> getGroupMembers(int groupId) { }
    public static boolean isGroupMember(int groupId, String username) { }

    // 消息存储
    public static boolean saveGroupMessage(Message message, int groupId) { }
    public static List<Message> getGroupMessageHistory(int groupId, int limit) { }
}
```

### 第二阶段：服务端核心实现 (步骤4-6)

#### 步骤4：实现服务器端GroupService
**文件**: `src/com/yychat/server/service/GroupService.java`

**核心方法**:
```java
public class GroupService {

    // 群组管理
    public ServiceResponse createGroup(String groupName, String creatorUsername, String avatarPath)
    public ServiceResponse joinGroup(int groupId, String username)
    public ServiceResponse leaveGroup(int groupId, String username)
    public ServiceResponse updateGroupInfo(int groupId, String groupName, String avatarPath)
    public ServiceResponse deleteGroup(int groupId, String requesterUsername)

    // 成员管理
    public ServiceResponse addMember(int groupId, String adderUsername, String targetUsername)
    public ServiceResponse removeMember(int groupId, String removerUsername, String targetUsername)
    public ServiceResponse transferOwnership(int groupId, String currentOwner, String newOwner)

    // 查询操作
    public ServiceResponse getGroupMembers(int groupId, String requesterUsername)
    public ServiceResponse getUserGroups(String username)
    public ServiceResponse searchGroups(String keyword)

    // 权限验证
    private boolean isGroupOwner(int groupId, String username)
    private boolean isGroupAdmin(int groupId, String username)
}
```

#### 步骤5：扩展消息类型定义
**文件**: `src/com/yychat/common/MessageType.java`

**新增常量**:
```java
// 群组管理消息类型
String GROUP_CREATE_REQUEST = "20";      // 创建群组请求
String GROUP_CREATE_RESPONSE = "21";     // 创建群组响应
String GROUP_JOIN_REQUEST = "22";        // 加入群组请求
String GROUP_JOIN_RESPONSE = "23";       // 加入群组响应
String GROUP_LEAVE_REQUEST = "24";       // 退出群组请求
String GROUP_LEAVE_RESPONSE = "25";      // 退出群组响应
String GROUP_INFO_UPDATE = "26";         // 群组信息更新
String GROUP_INFO_UPDATE_RESPONSE = "27";// 群组信息更新响应
String GROUP_DELETE_REQUEST = "28";      // 删除群组请求
String GROUP_DELETE_RESPONSE = "29";     // 删除群组响应

// 群组成员管理消息类型
String GROUP_ADD_MEMBER = "30";          // 添加成员
String GROUP_REMOVE_MEMBER = "31";       // 移除成员
String GROUP_TRANSFER_OWNER = "32";      // 转让群主

// 群组查询消息类型
String GROUP_MEMBERS_REQUEST = "33";     // 获取群组成员
String GROUP_MEMBERS_RESPONSE = "34";    // 群组成员响应
String USER_GROUPS_REQUEST = "35";       // 获取用户群组列表
String USER_GROUPS_RESPONSE = "36";      // 用户群组列表响应
String GROUP_SEARCH_REQUEST = "37";      // 搜索群组
String GROUP_SEARCH_RESPONSE = "38";     // 搜索群组响应

// 群组消息 (已存在但未实现)
String GROUP_CHAT_MESSAGE = "12";        // 群组聊天消息
```

#### 步骤6：实现群组消息处理器
**文件**: `src/com/yychat/server/handler/GroupServiceHandler.java`

**处理方法**:
```java
public class GroupServiceHandler {

    // 群组管理处理
    public void handleCreateGroupRequest(Message message, InetSocketAddress clientAddress)
    public void handleJoinGroupRequest(Message message, InetSocketAddress clientAddress)
    public void handleLeaveGroupRequest(Message message, InetSocketAddress clientAddress)
    public void handleUpdateGroupInfoRequest(Message message, InetSocketAddress clientAddress)
    public void handleDeleteGroupRequest(Message message, InetSocketAddress clientAddress)

    // 成员管理处理
    public void handleAddMemberRequest(Message message, InetSocketAddress clientAddress)
    public void handleRemoveMemberRequest(Message message, InetSocketAddress clientAddress)
    public void handleTransferOwnerRequest(Message message, InetSocketAddress clientAddress)

    // 查询处理
    public void handleGroupMembersRequest(Message message, InetSocketAddress clientAddress)
    public void handleUserGroupsRequest(Message message, InetSocketAddress clientAddress)
    public void handleGroupSearchRequest(Message message, InetSocketAddress clientAddress)

    // 群组消息处理
    public void handleGroupChatMessage(Message message, InetSocketAddress clientAddress)
}
```

### 第三阶段：客户端实现 (步骤7-8)

#### 步骤7：实现客户端GroupService
**文件**: `src/com/yychat/client/service/GroupService.java`

**核心方法**:
```java
public class GroupService {

    // 群组管理
    public CompletableFuture<ServiceResponse> createGroup(String groupName, String avatarPath)
    public CompletableFuture<ServiceResponse> joinGroup(int groupId)
    public CompletableFuture<ServiceResponse> leaveGroup(int groupId)
    public CompletableFuture<ServiceResponse> updateGroupInfo(int groupId, String groupName, String avatarPath)

    // 成员管理
    public CompletableFuture<ServiceResponse> addMember(int groupId, String username)
    public CompletableFuture<ServiceResponse> removeMember(int groupId, String username)

    // 查询操作
    public CompletableFuture<ServiceResponse> getGroupMembers(int groupId)
    public CompletableFuture<ServiceResponse> getUserGroups()
    public CompletableFuture<ServiceResponse> searchGroups(String keyword)

    // 群组消息
    public void sendGroupMessage(int groupId, String content, AttachmentType attachmentType, File file)
}
```

#### 步骤8：扩展客户端消息处理
**新增文件**: `src/com/yychat/client/handler/GroupServiceHandler.java`

**处理方法**:
```java
public class GroupServiceHandler {

    // 群组管理响应处理
    public void handleCreateGroupResponse(ServiceResponse response)
    public void handleJoinGroupResponse(ServiceResponse response)
    public void handleLeaveGroupResponse(ServiceResponse response)
    public void handleUpdateGroupInfoResponse(ServiceResponse response)

    // 成员管理响应处理
    public void handleAddMemberResponse(ServiceResponse response)
    public void handleRemoveMemberResponse(ServiceResponse response)

    // 查询响应处理
    public void handleGroupMembersResponse(ServiceResponse response)
    public void handleUserGroupsResponse(ServiceResponse response)
    public void handleGroupSearchResponse(ServiceResponse response)

    // 群组消息处理
    public void handleGroupChatMessage(ServiceResponse response)
    public void handleGroupInviteNotification(ServiceResponse response)
}
```

### 第四阶段：界面完善 (步骤9-10)

#### 步骤9：完善UI界面群组功能
**主要文件**:
- `src/com/yychat/client/view/MainWindow.java` - 完善群组面板
- `src/com/yychat/client/view/GroupListPanel.java` - 群组列表面板
- `src/com/yychat/client/view/GroupChatWindow.java` - 群组聊天窗口
- `src/com/yychat/client/view/CreateGroupDialog.java` - 创建群组对话框
- `src/com/yychat/client/view/GroupManageDialog.java` - 群组管理对话框

**UI功能设计**:
1. **群组列表面板** - 显示用户加入的所有群组
2. **群组聊天窗口** - 实时聊天、成员列表、群组信息
3. **创建群组对话框** - 输入群组名称、选择头像
4. **群组管理对话框** - 成员管理、群组设置
5. **搜索群组功能** - 查找和加入公开群组

#### 步骤10：集成测试和错误处理
**测试覆盖**:
- 群组创建、加入、退出流程测试
- 群组消息发送和接收测试
- 权限控制测试（群主/成员权限）
- 边界条件测试（空群组名、重复成员等）
- 错误处理测试（网络异常、数据验证）

## 📁 预期文件结构

```
需要创建/修改的文件：
├── sql/
│   └── schema.sql                    [修改] - 修复字段名
├── docs/
│   └── group-impl.md                 [新建] - 本实现计划
├── src/com/yychat/common/model/
│   ├── Group.java                    [新建] - 群组信息模型
│   └── GroupMember.java              [新建] - 群组成员模型
├── src/com/yychat/server/service/
│   └── GroupService.java             [新建] - 服务器端群组服务
├── src/com/yychat/client/service/
│   └── GroupService.java             [新建] - 客户端群组服务
├── src/com/yychat/server/handler/
│   └── GroupServiceHandler.java      [新建] - 服务器端消息处理
├── src/com/yychat/client/handler/
│   └── GroupServiceHandler.java      [新建] - 客户端消息处理
├── src/com/yychat/client/view/
│   ├── GroupListPanel.java           [新建] - 群组列表面板
│   ├── GroupChatWindow.java          [新建] - 群组聊天窗口
│   ├── CreateGroupDialog.java        [新建] - 创建群组对话框
│   ├── GroupManageDialog.java        [新建] - 群组管理对话框
│   └── MainWindow.java               [修改] - 完善群组面板
└── src/com/yychat/common/
    └── MessageType.java              [修改] - 添加群组消息类型
```

## 🔧 技术实现要点

### 1. 消息协议设计

**请求消息结构**:
```java
Message message = new Message.Builder()
    .setMessageType(MessageType.GROUP_CREATE_REQUEST)
    .setSenderUsername(username)
    .setContent(groupName)  // JSON格式包含群组信息
    .setTaskId(taskId)
    .build();
```

**响应消息结构**:
```java
ServiceResponse response = new ServiceResponse(
    success,
    message,
    data,  // JSON格式包含群组信息
    taskId
);
```

### 2. 权限控制设计

**群组角色**:
- **群主 (Owner)** - 拥有所有权限，可以转让群主身份
- **管理员 (Admin)** - 可以添加/移除成员，管理群组信息
- **成员 (Member)** - 可以发送消息，查看群组信息

**权限检查流程**:
```java
// 示例：移除成员权限检查
if (!isGroupOwner(groupId, requesterUsername) &&
    !isGroupAdmin(groupId, requesterUsername)) {
    return ServiceResponse.error("权限不足，只有群主和管理员可以移除成员");
}
```

### 3. 群组消息广播设计

**消息传播路径**:
```
发送者客户端 → UDP服务器 → 消息处理器 → 群组成员列表 → 批量发送 → 接收者客户端
```

**实时性保证**:
- 使用UDP多播机制发送群组消息
- 在线成员实时接收，离线成员历史记录
- 支持消息确认和重发机制

### 4. 数据一致性设计

**事务管理**:
- 群组创建：原子性操作（插入群组信息 + 添加创建者为成员）
- 成员变更：检查权限 + 数据库操作 + 消息通知
- 群组删除：权限检查 + 删除成员关系 + 删除消息记录

## 📋 开发优先级

### 🔴 高优先级 (核心功能)
1. **基础模型创建** - Group、GroupMember类
2. **数据库操作扩展** - DBUtil群组方法
3. **服务器端GroupService** - 核心业务逻辑
4. **消息协议定义** - 消息类型扩展

### 🟡 中优先级 (客户端功能)
5. **客户端GroupService** - 客户端服务接口
6. **消息处理器** - 群组消息处理
7. **基础UI界面** - 群组列表、聊天窗口

### 🟢 低优先级 (完善功能)
8. **高级UI功能** - 群组管理、搜索功能
9. **错误处理优化** - 边界情况处理
10. **性能优化** - 缓存机制、批量操作

## ⚡ 风险评估与应对

### 🔴 风险点
1. **数据库事务一致性** - 群组操作涉及多表操作
2. **权限安全漏洞** - 权限检查逻辑复杂
3. **消息广播性能** - 大量群组消息的处理
4. **并发访问冲突** - 多线程环境下的数据一致性

### 🛡️ 应对策略
1. **数据库事务** - 使用事务确保操作原子性
2. **权限验证** - 多层权限检查，统一权限接口
3. **消息队列** - 使用消息队列处理批量群发
4. **线程同步** - 使用synchronized或ReentrantLock保护关键数据

## 🎯 预期效果

完成后的群组功能将提供：
- ✅ **完整的群组生命周期管理** - 创建、加入、退出、删除
- ✅ **灵活的成员权限控制** - 群主、管理员、成员三级权限
- ✅ **实时群组消息通信** - 支持文本、图片、文件等多种消息类型
- ✅ **直观的用户界面** - 符合现有UI风格的群组管理界面
- ✅ **安全的操作权限** - 完善的用户认证和权限验证机制

此实现计划将确保群组功能的无缝集成，保持与现有代码风格和架构模式的一致性，为用户提供完整的群组通信体验。