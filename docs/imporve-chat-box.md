# FriendChat聊天框改进实现步骤

## 改进目标

对`LoginClient/src/com/yychat/view/FriendChat.java`文件进行以下改进：

1. **发送消息时添加时间戳显示**：发送者发送消息时同时显示发送时间
2. **改进接收消息显示格式**：优化消息接收时的显示样式
3. **颜色区分优化**：
   - 时间显示使用灰色
   - 发送者和接收者消息使用不同颜色
   - 增加视觉层次感

## 实现步骤

### 步骤1：添加必要的导入包

在文件顶部添加以下导入语句：

```java
import java.text.SimpleDateFormat;
import java.util.Date;
```

### 步骤2：修改构造函数中的文本区域设置

将原有的文本区域设置修改为支持多色显示：

```java
// 删除原有的单色设置
// textArea.setForeground(Color.red);

// 设置文本区域为支持多色显示
textArea.setEditable(false);
textArea.setBackground(Color.WHITE);
textArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
```

### 步骤3：创建消息格式化方法

在类中添加一个新方法用于格式化发送消息：

```java
/**
 * 格式化发送消息显示
 * @param message 消息内容
 * @param sender 发送者
 */
private void appendSendMessage(String message, String sender) {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    String currentTime = sdf.format(new Date());

    // 添加时间（灰色）
    textArea.append("[" + currentTime + "]\n");
    textArea.setForeground(Color.GRAY);
    textArea.append("我");

    // 添加消息内容（蓝色）
    textArea.append(": ");
    textArea.setForeground(Color.BLUE);
    textArea.append(message + "\n");

    // 重置为默认颜色以便后续操作
    textArea.setForeground(Color.BLACK);
}
```

### 步骤4：创建接收消息格式化方法

添加一个新方法用于格式化接收到的消息：

```java
/**
 * 格式化接收消息显示
 * @param message 消息对象
 */
private void appendReceiveMessage(Message message) {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    String currentTime = sdf.format(new Date());

    // 添加时间（灰色）
    textArea.append("[" + currentTime + "]\n");
    textArea.setForeground(Color.GRAY);

    // 添加发送者名称（绿色）
    textArea.append(message.getSender());

    // 添加消息内容（黑色）
    textArea.append(": ");
    textArea.setForeground(Color.BLACK);
    textArea.append(message.getContent() + "\n");

    // 重置为默认颜色
    textArea.setForeground(Color.BLACK);
}
```

### 步骤5：修改发送按钮事件处理

更新发送按钮的事件处理代码，将原来的简单消息追加替换为格式化显示：

```java
sendButton.addActionListener(e -> {
    String msg = messageField.getText();
    if (msg.trim().isEmpty()) return;

    // 使用新的格式化方法显示发送的消息
    appendSendMessage(msg, sender);
    messageField.setText("");

    Message message = new Message();
    message.setSender(sender);
    message.setReceiver(receiver);
    message.setMessageType(MessageType.COMMON_CHAT_MESSAGE);
    message.setContent(msg);

    try {
        if (this.udpConnection != null) {
            this.udpConnection.sendChatMessage(message);
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        // 错误消息使用红色
        textArea.setForeground(Color.RED);
        textArea.append("消息发送失败: " + ex.getMessage() + "\n");
        textArea.setForeground(Color.BLACK);
    }
});
```

### 步骤6：更新接收消息处理方法

修改现有的`append`方法以使用新的格式化显示：

```java
public void append(Message message) {
    appendReceiveMessage(message);
}
```

### 步骤7：优化界面样式

在构造函数的最后添加界面优化设置：

```java
// 在setVisible(true)之前添加
this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
this.setResizable(true);

// 优化文本区域滚动
scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
```

## 改进效果

完成以上步骤后，聊天框将实现以下效果：

1. **发送消息显示**：
   - 格式：`[时间] 我: 消息内容`
   - 时间显示为灰色
   - "我"和消息内容显示为蓝色

2. **接收消息显示**：
   - 格式：`[时间] 发送者: 消息内容`
   - 时间显示为灰色
   - 发送者名称显示为绿色
   - 消息内容显示为黑色

3. **整体视觉效果**：
   - 清晰的时间标识
   - 明确的发送者/接收者区分
   - 良好的视觉层次感
   - 优雅的错误提示

## 注意事项

1. 保持原有的UDP通信逻辑不变
2. 确保Message对象的获取时间戳功能正常
3. 错误处理保持原有逻辑，只改进显示样式
4. 字体和颜色可根据实际需求调整