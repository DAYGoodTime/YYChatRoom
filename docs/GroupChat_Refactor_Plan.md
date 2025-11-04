# GroupChat 布局重构计划

## 项目概述

### 重构目标
将 YYChatRoom 项目中的 GroupChat 从独立的聊天窗口重构为集成在主界面中的专业群聊面板，提供更丰富的功能和更好的用户体验。

### 当前问题
- GroupChat 使用独立的聊天窗口，布局简单
- 缺少群组成员管理功能
- 没有群组设置和管理选项
- 整体用户体验有待提升

### 重构目标
- 集成到主界面中，不是独立窗口
- 添加群组成员列表和管理功能
- 提供群组设置功能（修改名称、上传头像、退出群聊）
- 实现成员右键菜单（移出、转让、设为管理员、加为好友）
- 优化整体布局和用户体验

## 架构设计方案

### 核心类设计

#### 1. BaseChatFrame（新创建）
```java
public abstract class BaseChatFrame extends JFrame {
    // 窗口框架类，处理所有窗口相关操作
    protected abstract JPanel createChatPanel();
    protected void initializeFrame(String title);
    protected void setupLayout();
}
```

**职责**：
- 继承 `JFrame`，处理所有窗口操作
- 提供框架，子类添加聊天面板
- 统一管理窗口的生命周期

#### 2. BaseChatPanel（重构原 BaseChat）
```java
public abstract class BaseChatPanel extends JPanel implements KeyListener {
    // 聊天面板组件，专注于聊天逻辑
    protected abstract ServiceResponse<Message> sendTextMessage(String text);
    protected abstract void sendFileMessage(File file, String message);
    protected abstract ServiceResponse<Page<ChatMessage>> loadMessageFromHistory();
    protected void initChatUI();
}
```

**职责**：
- 继承 `JPanel`，专注于聊天逻辑
- 消息显示、输入处理、文件传输
- 不再处理窗口操作

#### 3. FriendChat（更新）
```java
public class FriendChat extends BaseChatFrame {
    private FriendChatPanel friendChatPanel;
    private User receiver;

    protected JPanel createChatPanel() {
        return friendChatPanel;
    }
}
```

**职责**：
- 继承 `BaseChatFrame`
- 内部包含 `FriendChatPanel`
- 使用简单的单聊布局

#### 4. GroupChat（重构）
```java
public class GroupChat extends BaseChatFrame {
    private GroupChatPanel groupChatPanel;
    private Group group;

    protected JPanel createChatPanel() {
        return groupChatPanel;
    }
}
```

**职责**：
- 继承 `BaseChatFrame`
- 内部包含 `GroupChatPanel`
- 实现新的群聊布局

### BaseChatPanel 子类设计

#### 1. FriendChatPanel
```java
public class FriendChatPanel extends BaseChatPanel {
    private User receiver;

    protected ServiceResponse<Message> sendTextMessage(String text) {
        return MessageService.getInstance().sendPlainTextMessageToUser(sender, receiver, text);
    }
    // 其他实现...
}
```

#### 2. GroupChatPanel
```java
public class GroupChatPanel extends BaseChatPanel {
    private Group group;

    protected ServiceResponse<Message> sendTextMessage(String text) {
        return MessageService.getInstance().sendPlainTextMessageToGroup(sender, group, text);
    }

    // 创建新的布局结构
    private void initGroupLayout() {
        setLayout(new BorderLayout());

        // 头部区域
        JPanel headerPanel = createGroupHeaderPanel();

        // 中部区域（消息区 + 成员列表）
        JPanel contentPanel = createGroupContentPanel();

        // 底部区域（输入框）
        JPanel inputPanel = createGroupInputPanel();

        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }
}
```

## 详细实施步骤

### 第一阶段：创建基础框架

#### 1.1 创建 BaseChatFrame 类
- 继承 `JFrame`
- 提供抽象方法 `createChatPanel()`
- 统一处理窗口初始化、布局、关闭操作
- 处理窗口显示和隐藏逻辑

#### 1.2 迁移窗口操作
从原 `BaseChat` 迁移到 `BaseChatFrame`：
- `setTitle()`, `setSize()`, `setVisible()`
- `setLocationRelativeTo()`, `setDefaultCloseOperation()`
- `setIconImage()`, `setResizable()`
- `highlightChatWindow()` 窗口高亮功能

### 第二阶段：重构 BaseChat 为 BaseChatPanel

#### 2.1 类定义变更
- 继承关系：`extends JFrame` → `extends JPanel`
- 移除窗口相关代码
- 保留聊天逻辑相关代码

#### 2.2 UI初始化调整
- 移除 `JFrame` 相关的初始化
- 保留消息显示区域、输入框、按钮等组件
- 调整布局管理为适合面板的方式

#### 2.3 消息处理逻辑
- 保持原有的消息发送、接收逻辑不变
- 保持文件传输功能不变
- 保持消息历史加载功能不变

### 第三阶段：更新 FriendChat

#### 3.1 继承关系调整
- 从继承 `BaseChat` 改为继承 `BaseChatFrame`
- 创建内部 `FriendChatPanel` 实例

#### 3.2 布局简化
- 移除复杂的窗口布局代码
- 使用简单的单聊布局
- 保持原有的功能不变

### 第四阶段：重构 GroupChat

#### 4.1 创建 GroupChatPanel 布局

##### 4.1.1 头部区域（GroupHeaderPanel）
```
┌─────────────────────────────────────────────────────┐
│ [群头像]  群聊名称           ⚙️设置按钮         │
└─────────────────────────────────────────────────────┘
```

**实现细节**：
- **群头像**（左）：
  - 显示群组头像图片
  - 点击可预览头像
  - 尺寸：50x50px

- **群名称**（中）：
  - 显示群组名称
  - 支持在线编辑（双击编辑）
  - 字体：微软雅黑，14号，粗体

- **设置按钮**（右）：
  - 图标按钮（⚙️）
  - 点击显示下拉菜单
  - 菜单项：群组信息设置、退出群聊

##### 4.1.2 中部区域（GroupContentPanel）
```
┌─────────────────┬───────────────────┐
│                 │                   │
│   消息显示区域   │   群成员列表      │
│                 │                   │
│                 │                   │
│                 │                   │
└─────────────────┴───────────────────┘
```

**消息显示区域**：
- 复用原有的消息显示组件
- 支持文字消息、文件消息
- 自动滚动到底部
- 消息历史加载功能

**群成员列表**：
- 显示所有群成员
- 成员头像 + 用户名
- 在线状态指示（绿色/灰色）
- 角色标识（👑群主、⭐管理员、👤成员）
- 右键菜单功能

##### 4.1.3 底部区域（GroupInputPanel）
```
┌─────────────────────────────────────────────────────────┐
│ [消息输入框                        ] [发送] [上传文件]   │
└─────────────────────────────────────────────────────────┘
```

**实现**：
- 复用原有的输入组件
- 保持文件上传功能
- 支持Enter键发送

#### 4.2 实现群成员管理功能

##### 4.2.1 成员列表显示
```java
private void displayGroupMembers() {
    ServiceResponse<List<GroupMember>> response = GroupService.getInstance().getGroupMembers(group.getGroupId());
    if (response.isSuccess()) {
        membersList.setModel(createMemberListModel(response.getData()));
    }
}
```

**列表项格式**：
- 头像 + 用户名 + 在线状态 + 角色标签
- 根据角色显示不同图标和颜色

##### 4.2.2 成员右键菜单
```java
private JPopupMenu createMemberContextMenu(GroupMember member) {
    JPopupMenu menu = new JPopupMenu();

    // 如果是当前用户，添加"加为好友"选项
    if (!member.getUsername().equals(currentUser.getUserName()) && !isFriend(member.getUsername())) {
        JMenuItem addFriendItem = new JMenuItem("加为好友");
        addFriendItem.addActionListener(e -> addFriend(member.getUsername()));
        menu.add(addFriendItem);
    }

    // 如果当前用户有管理权限，添加管理选项
    if (currentUserIsAdmin() && !member.getUsername().equals(currentUser.getUserName())) {
        menu.addSeparator();

        // 设为管理员/取消管理员
        if (member.isMember()) {
            JMenuItem setAdminItem = new JMenuItem("设为管理员");
            setAdminItem.addActionListener(e -> setMemberAsAdmin(member.getUsername()));
            menu.add(setAdminItem);
        } else if (member.isAdmin()) {
            JMenuItem removeAdminItem = new JMenuItem("取消管理员");
            removeAdminItem.addActionListener(e -> removeMemberFromAdmin(member.getUsername()));
            menu.add(removeAdminItem);
        }

        // 移出群聊
        JMenuItem removeMemberItem = new JMenuItem("移出群聊");
        removeMemberItem.addActionListener(e -> removeMemberFromGroup(member.getUsername()));
        menu.add(removeMemberItem);
    }

    // 如果是群主，添加转让群主选项
    if (currentUserIsOwner() && !member.getUsername().equals(currentUser.getUserName())) {
        menu.addSeparator();
        JMenuItem transferOwnerItem = new JMenuItem("转让群主");
        transferOwnerItem.addActionListener(e -> transferGroupOwnership(member.getUsername()));
        menu.add(transferOwnerItem);
    }

    return menu;
}
```

##### 4.2.3 权限控制逻辑
```java
private boolean currentUserIsOwner() {
    return group.isOwner(currentUser.getUserName());
}

private boolean currentUserIsAdmin() {
    GroupMember currentMember = findCurrentMember();
    return currentMember != null && currentMember.isAdmin();
}

private GroupMember findCurrentMember() {
    return group.getMembers().stream()
        .filter(member -> member.getUsername().equals(currentUser.getUserName()))
        .findFirst()
        .orElse(null);
}
```

#### 4.3 实现设置功能

##### 4.3.1 设置按钮下拉菜单
```java
private JPopupMenu createSettingsMenu() {
    JPopupMenu menu = new JPopupMenu();

    JMenuItem groupInfoItem = new JMenuItem("群组信息设置");
    groupInfoItem.addActionListener(e -> openGroupSettingsDialog());
    menu.add(groupInfoItem);

    menu.addSeparator();

    JMenuItem leaveGroupItem = new JMenuItem("退出群聊");
    leaveGroupItem.addActionListener(e -> leaveGroup());
    menu.add(leaveGroupItem);

    return menu;
}
```

##### 4.3.2 群组设置对话框
```java
private void openGroupSettingsDialog() {
    GroupSettingsDialog dialog = new GroupSettingsDialog(this, group);
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
        // 更新群组信息
        String newGroupName = dialog.getGroupName();
        String newAvatarPath = dialog.getAvatarPath();

        ServiceResponse<Group> response = GroupService.getInstance()
            .updateGroupInfo(group.getGroupId(), newGroupName, newAvatarPath);

        if (response.isSuccess()) {
            group = response.getData();
            updateGroupHeader();
            JOptionPane.showMessageDialog(this, "群组信息更新成功", "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "群组信息更新失败: " + response.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

##### 4.3.3 退出群聊功能
```java
private void leaveGroup() {
    int result = JOptionPane.showConfirmDialog(this,
        "确定要退出群聊吗？",
        "确认退出",
        JOptionPane.YES_NO_OPTION);

    if (result == JOptionPane.YES_OPTION) {
        ServiceResponse<String> response = GroupService.getInstance()
            .leaveGroup(group.getGroupId());

        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, "已成功退出群聊", "提示", JOptionPane.INFORMATION_MESSAGE);
            dispose(); // 关闭聊天窗口
        } else {
            JOptionPane.showMessageDialog(this, "退出群聊失败: " + response.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

#### 4.4 成员操作实现

##### 4.4.1 添加好友
```java
private void addFriend(String username) {
    Message message = Message.builder()
        .setMessageType(MessageType.USER_ADD_NEW_FRIEND)
        .setSender(currentUser.getUserName())
        .setContent(username)
        .build();

    ClientMain.getUDPConnection().sendMessageToServer(message);
    JOptionPane.showMessageDialog(this, "已发送好友申请", "提示", JOptionPane.INFORMATION_MESSAGE);
}
```

##### 4.4.2 设为管理员
```java
private void setMemberAsAdmin(String username) {
    // TODO: 实现设为管理员的服务器端接口
    // 临时显示功能待实现提示
    JOptionPane.showMessageDialog(this, "设为管理员功能待实现", "提示", JOptionPane.INFORMATION_MESSAGE);
}
```

##### 4.4.3 移除成员
```java
private void removeMemberFromGroup(String username) {
    int result = JOptionPane.showConfirmDialog(this,
        "确定要将 " + username + " 移出群聊吗？",
        "确认移出",
        JOptionPane.YES_NO_OPTION);

    if (result == JOptionPane.YES_OPTION) {
        ServiceResponse<String> response = GroupService.getInstance()
            .removeMember(group.getGroupId(), username);

        if (response.isSuccess()) {
            updateMemberList();
            JOptionPane.showMessageDialog(this, "已成功移出群聊", "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "移除成员失败: " + response.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

##### 4.4.4 转让群主
```java
private void transferGroupOwnership(String username) {
    int result = JOptionPane.showConfirmDialog(this,
        "确定要将群主转让给 " + username + " 吗？\n此操作不可撤销！",
        "确认转让",
        JOptionPane.YES_NO_OPTION);

    if (result == JOptionPane.YES_OPTION) {
        ServiceResponse<String> response = GroupService.getInstance()
            .transferOwnership(group.getGroupId(), username);

        if (response.isSuccess()) {
            updateMemberList();
            JOptionPane.showMessageDialog(this, "群主转让成功", "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "转让群主失败: " + response.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

### 第五阶段：集成和测试

#### 5.1 MainWindow 集成
- 更新 `MainWindow` 中的 `GroupChat` 创建逻辑
- 确保新的 `GroupChat` 可以正常显示和隐藏
- 处理群聊窗口的打开和关闭事件

#### 5.2 功能测试

##### 5.2.1 基础功能测试
- [ ] 消息发送接收正常
- [ ] 文件传输功能正常
- [ ] 消息历史加载正常
- [ ] 窗口显示和隐藏正常

##### 5.2.2 新功能测试
- [ ] 群组成员列表显示
- [ ] 在线状态正确显示
- [ ] 角色标识正确显示
- [ ] 成员右键菜单功能
- [ ] 设置按钮菜单功能
- [ ] 群组信息修改功能
- [ ] 退出群聊功能

##### 5.2.3 权限控制测试
- [ ] 群主可以执行所有操作
- [ ] 管理员可以管理普通成员
- [ ] 普通成员无法执行管理操作
- [ ] 权限判断逻辑正确

#### 5.3 界面测试
- [ ] 布局美观，组件对齐正确
- [ ] 字体和颜色符合设计要求
- [ ] 图标显示正常
- [ ] 响应式布局适配不同屏幕尺寸

## 技术实现细节

### 使用的组件和技术
- **Swing 组件**: JPanel, JLabel, JButton, JPopupMenu, JList
- **布局管理**: BorderLayout, BoxLayout, GridBagLayout
- **事件处理**: MouseListener, ActionListener
- **现有服务**: GroupService, MessageService, AvatarService

### 数据模型
- **Group**: 群组信息模型
- **GroupMember**: 群组成员模型，包含角色和权限信息
- **User**: 用户信息模型
- **Message**: 消息模型

### 消息协议
复用现有的消息类型：
- `GROUP_MEMBERS_REQUEST` / `GROUP_MEMBERS_RESPONSE` - 获取群组成员
- `GROUP_INFO_UPDATE` / `GROUP_INFO_UPDATE_RESPONSE` - 更新群组信息
- `GROUP_LEAVE_REQUEST` / `GROUP_LEAVE_RESPONSE` - 退出群组
- `GROUP_REMOVE_MEMBER` / `GROUP_REMOVE_MEMBER_RESPONSE` - 移除成员
- `GROUP_TRANSFER_OWNER` / `GROUP_TRANSFER_OWNER_RESPONSE` - 转让群主

## 预期效果

### 用户体验提升
1. **集成化体验**: 群聊不再是独立窗口，与主界面无缝集成
2. **功能丰富**: 完整的群组管理功能，提升用户粘性
3. **操作便捷**: 右键菜单提供快速操作入口
4. **视觉优化**: 现代化的界面设计，提升视觉效果

### 技术优势
1. **架构清晰**: 职责分离，代码可维护性强
2. **复用性强**: 基础组件可以复用于其他功能
3. **扩展性好**: 易于添加新的群组管理功能
4. **性能稳定**: 复用现有成熟的消息处理逻辑

### 功能对比

| 功能 | 重构前 | 重构后 |
|------|--------|--------|
| 窗口形式 | 独立聊天窗口 | 集成在主界面中 |
| 群成员显示 | ❌ | ✅ 完整成员列表 |
| 成员管理 | ❌ | ✅ 右键菜单管理 |
| 群组设置 | ❌ | ✅ 完整的设置功能 |
| 权限控制 | ❌ | ✅ 角色权限管理 |
| 布局设计 | 简单 | 专业的群聊布局 |

## 风险评估和缓解措施

### 潜在风险
1. **代码重构风险**: 大量的代码修改可能引入bug
2. **向后兼容性**: 可能影响现有的聊天功能
3. **性能影响**: 新增功能可能影响聊天性能
4. **用户适应**: 用户需要时间适应新的界面

### 缓解措施
1. **分阶段实施**: 按阶段逐步实施，每阶段进行充分测试
2. **渐进式重构**: 保持核心功能不变，逐步添加新功能
3. **详细测试**: 每个功能点都进行全面的功能测试
4. **用户培训**: 提供界面说明，帮助用户快速适应

## 总结

本次 GroupChat 布局重构将显著提升用户体验，提供完整的群组管理功能。通过清晰的架构设计和分阶段实施，可以确保重构的稳定性和可维护性。重构后的 GroupChat 将成为 YYChatRoom 项目的一个重要亮点功能。