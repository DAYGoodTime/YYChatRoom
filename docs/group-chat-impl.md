# 群组聊天功能实现计划

## 项目概述

在现有YYChatRoom项目基础上，实现完整的群组聊天功能，包括代码复用优化、历史消息加载等特性。

## 现有代码分析

### 已存在的相关类
- `FriendChat.java` - 好友聊天窗口（577行代码）
- `GroupListPanel.java` - 群组列表面板（目前openGroupChat未实现）
- `ChatTarget`接口、`UserChatTarget`、`GroupChatTarget`实现类
- `MessageService.java` - 消息服务（支持用户聊天）
- `GroupService.java` - 群组服务（支持群组管理）

### 现有功能
- ✅ 群组列表显示
- ✅ 创建群组、加入群组、搜索群组
- ✅ 群组成员管理
- ✅ 好友聊天（文本+文件传输）
- ❌ 群组聊天（待实现）

## 实现方案

### 第一阶段：基础群组聊天功能

#### 1.1 代码复用优化
**目标**: 创建BaseChat抽象类，提取公共功能

**BaseChat抽象类设计**:
```java
public abstract class BaseChat extends JFrame implements KeyListener {
    // 公共UI组件
    protected JButton sendButton;
    protected JButton sendFileButton;
    protected JTextPane messageArea;
    protected JTextField messageInputField;

    // 公共方法
    protected abstract void sendMessage();
    protected abstract void loadChatHistory();
    protected abstract ChatTarget getChatTarget();

    // 复用FriendChat的公共方法
    protected void appendSendMessage(Message message, boolean received)
    protected Component appendTextMessage(Message message)
    protected Component appendFileMessage(Message message, boolean received)
    // ... 其他公共方法
}
```

**优势**:
- 减少代码重复
- 统一聊天窗口行为
- 便于后续维护和扩展

#### 1.2 修改FriendChat
- 让FriendChat继承BaseChat
- 实现抽象方法
- 保留现有功能不变

#### 1.3 创建GroupChat类
- 继承BaseChat
- 专门处理群组聊天
- 支持群组成员显示
- 消息显示时显示真实发送者名称

### 第二阶段：历史消息功能

#### 2.1 添加消息类型常量
在`MessageType.java`中添加：
```java
String USER_CHAT_HISTORY_REQUEST = "50";     // 用户聊天历史请求
String USER_CHAT_HISTORY_RESPONSE = "51";    // 用户聊天历史响应
String GROUP_CHAT_HISTORY_REQUEST = "52";    // 群组聊天历史请求
String GROUP_CHAT_HISTORY_RESPONSE = "53";   // 群组聊天历史响应
```

#### 2.2 客户端历史消息服务
在`MessageService.java`中添加：
```java
// 获取与指定用户的历史消息（分页）
public ServiceResponse<List<Message>> getUserChatHistory(String username, int page, int pageSize)

// 获取群组历史消息（分页）
public ServiceResponse<List<Message>> getGroupChatHistory(int groupId, int page, int pageSize)
```

**参数说明**:
- `page`: 页码（从0开始）
- `pageSize`: 每页消息数量（建议20条）

#### 2.3 历史消息加载UI
在BaseChat中添加：
```java
// 加载历史消息按钮
protected JButton loadMoreButton;

// 初始化历史消息加载
protected void initHistoryLoading()

// 异步加载历史消息
protected void loadHistoryAsync(int page)

// 加载更多历史消息
protected void loadMoreHistory()
```

**加载策略**:
- 初始化时加载最新20条消息
- 显示"加载更多历史消息"按钮
- 点击按钮加载前20条消息
- 支持无限滚动加载

#### 2.4 加载动画
创建加载动画面板：
```java
private JPanel createLoadingPanel() {
    // 显示旋转图标和"正在加载..."文字
    // 支持取消操作
}
```

### 第三阶段：服务端支持

#### 3.1 历史消息查询API
在服务端添加消息历史查询逻辑：

**用户聊天历史查询**:
```java
// 查询与指定用户的历史聊天记录
public List<Message> getUserChatHistory(String username1, String username2, int page, int pageSize)
```

**群组聊天历史查询**:
```java
// 查询群组历史聊天记录
public List<Message> getGroupChatHistory(int groupId, int page, int pageSize)
```

#### 3.2 数据库查询优化
- 按时间倒序查询
- 使用LIMIT和OFFSET实现分页
- 索引优化（sender, receiver, time）

### 第四阶段：UI优化和体验

#### 4.1 群组聊天特色功能
- 显示群组成员在线状态
- 群组头像显示
- 群组公告显示（后续扩展）

#### 4.2 消息显示优化
- 群组消息显示发送者头像和名称
- 消息时间显示优化
- 支持@功能（后续扩展）

#### 4.3 文件传输复用
- 群组文件传输功能
- 文件下载权限控制
- 文件预览功能

## 实现步骤

### 步骤1: 创建BaseChat抽象类
- [ ] 提取FriendChat的公共字段和方法
- [ ] 定义抽象方法
- [ ] 设计消息显示逻辑

### 步骤2: 修改FriendChat
- [ ] 继承BaseChat
- [ ] 实现抽象方法
- [ ] 保持现有功能

### 步骤3: 创建GroupChat类
- [ ] 继承BaseChat
- [ ] 实现群组聊天逻辑
- [ ] 添加群组特色功能

### 步骤4: 实现历史消息API
- [ ] 客户端MessageService添加历史消息方法
- [ ] 服务端添加历史消息查询逻辑
- [ ] 测试API功能

### 步骤5: 历史消息UI
- [ ] 在BaseChat中添加历史消息加载UI
- [ ] 实现异步加载和加载动画
- [ ] 实现"加载更多"功能

### 步骤6: 更新GroupListPanel
- [ ] 实现openGroupChat方法
- [ ] 创建GroupChat实例并显示

### 步骤7: 测试和优化
- [ ] 群组聊天功能测试
- [ ] 历史消息加载测试
- [ ] UI优化和bug修复

## 技术要点

### 线程安全
- 所有UI更新必须在EDT中执行
- 使用SwingWorker进行历史消息异步加载

### 性能优化
- 消息分页加载，避免一次性加载大量数据
- 消息缓存机制
- 图片懒加载

### 用户体验
- 加载状态提示
- 错误处理和重试机制
- 响应式UI设计

## 风险评估

### 技术风险
- **高**: 服务端数据库查询性能（大量历史消息）
- **中**: 客户端内存占用（大量消息缓存）
- **低**: UI响应性问题

### 缓解措施
- 实现消息分页和懒加载
- 添加内存监控和清理机制
- 使用SwingWorker避免UI阻塞

## 预期成果

### 功能完整性
- ✅ 群组聊天基本功能
- ✅ 群组文件传输
- ✅ 历史消息查看
- ✅ 良好的用户体验

### 代码质量
- 代码复用率高
- 架构清晰，易于维护
- 统一的错误处理
- 完整的注释文档

## 后续扩展

### 短期扩展
- 群组公告功能
- 消息撤回功能

