# YYChatRoom用户自定义头像功能 - 部署和使用指南

## 概述

YYChatRoom头像功能已完全实现，支持用户从6个默认头像中选择以及自定义头像上传功能。本指南详细介绍如何部署和使用这些新功能。

## 🚀 快速开始

### 1. 数据库迁移

首先执行数据库迁移脚本：

```sql
-- 执行此SQL命令更新数据库结构
USE yychat2022s;
ALTER TABLE user ADD COLUMN avatar_path VARCHAR(255) DEFAULT '0.jpg';
```

### 2. 启动服务器

1. 编译项目：
   ```bash
   # 编译服务器端
   javac -cp "Server/src:Server/lib/*" Server/src/com/yychat/view/StartServerUDP.java

   # 编译客户端
   javac -cp "LoginClient/src:LoginClient/lib/*" LoginClient/src/com/yychat/view/ClientLoginUDP.java
   ```

2. 启动服务器：
   ```bash
   java -cp "Server/src:Server/lib/*" com.yychat.view.StartServerUDP
   ```

3. 启动客户端：
   ```bash
   java -cp "LoginClient/src:LoginClient/lib/*" com.yychat.view.ClientLoginUDP
   ```

### 3. 测试头像功能

运行头像功能测试：
```bash
java -cp ".:Server/src:Server/lib/*:LoginClient/src:LoginClient/lib/*" com.yychat.test.AvatarFunctionTest
```

## 📋 功能特性

### ✅ 已实现功能

1. **默认头像系统**
   - 6个内置默认头像（0.jpg - 5.jpg）
   - 新用户默认分配头像0.jpg
   - 好友列表显示头像

2. **头像选择界面**
   - 图形化头像选择对话框
   - 头像预览功能
   - 直观的选择界面

3. **头像传输机制**
   - UDP消息协议支持
   - 4种新的消息类型
   - 头像文件数据传输

4. **服务器端管理**
   - 头像文件存储管理
   - 数据库头像路径存储
   - 头像更新广播机制

5. **客户端显示**
   - 动态头像加载
   - 头像缓存机制
   - 实时头像更新

### 🔧 技术架构

#### 数据库层
- `user`表新增`avatar_path`字段
- 默认值设置为`0.jpg`
- 支持现有用户无缝迁移

#### 网络协议
- `REQUEST_AVATAR (22)`: 请求用户头像
- `RESPONSE_AVATAR (23)`: 返回头像数据
- `UPDATE_AVATAR (24)`: 更新头像
- `AVATAR_UPLOAD_SUCCESS (25)`: 头像上传成功

#### 文件存储结构
```
avatars/
├── default/
│   ├── 0.jpg
│   ├── 1.jpg
│   ├── 2.jpg
│   ├── 3.jpg
│   ├── 4.jpg
│   └── 5.jpg
└── users/
    ├── username1/
    │   └── custom_avatar.jpg
    └── username2/
        └── another_avatar.png
```

## 🛠️ 详细配置

### 服务器配置

1. **头像目录初始化**
   - 服务器启动时自动创建头像目录
   - 验证默认头像文件存在性
   - 设置文件权限

2. **文件管理配置**
   - 最大文件大小：50KB
   - 支持格式：JPG, PNG, GIF
   - 自动文件命名和去重

### 客户端配置

1. **头像显示配置**
   - 头像尺寸：64x64像素
   - 缓存机制：避免重复加载
   - 懒加载：按需加载头像

2. **用户界面配置**
   - 选择对话框：500x400像素
   - 预览区域：80x80像素
   - 网格布局：2行3列

## 🎮 使用指南

### 用户操作流程

1. **选择头像**
   - 在好友列表界面右键或点击头像设置按钮
   - 弹出头像选择对话框
   - 从6个默认头像中选择喜欢的头像
   - 点击"确定"应用更改

2. **查看好友头像**
   - 好友列表自动显示好友当前头像
   - 好友更新头像后会自动通知并更新显示

3. **头像更新通知**
   - 当好友更新头像时，其他在线好友会收到通知
   - 头像会自动更新到最新版本

### 开发者操作

1. **添加新的默认头像**
   ```java
   // 在AvatarSelector.java中添加新的头像文件名
   private static final String[] DEFAULT_AVATARS = {
       "0.jpg", "1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg"
   };
   ```

2. **自定义头像处理**
   ```java
   // 在AvatarFileManager.java中调整文件大小限制
   private static final long MAX_FILE_SIZE = 100 * 1024; // 100KB
   ```

3. **扩展消息类型**
   ```java
   // 在MessageType.java中添加新的消息类型
   String AVATAR_DELETE = "26"; // 删除头像
   String AVATAR_SETTING_CHANGED = "27"; // 头像设置变更
   ```

## 🔍 故障排除

### 常见问题

1. **头像不显示**
   - 检查默认头像文件是否存在于`res/`目录
   - 验证服务器端头像目录权限
   - 查看客户端控制台错误信息

2. **头像上传失败**
   - 确认文件大小不超过50KB
   - 检查文件格式（JPG/PNG/GIF）
   - 验证服务器端存储目录权限

3. **头像更新不生效**
   - 检查数据库连接是否正常
   - 验证avatar_path字段是否正确更新
   - 确认UDP消息传递是否正常

### 调试方法

1. **启用详细日志**
   ```java
   // 在服务器端设置日志级别
   System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
   ```

2. **测试头像传输**
   ```bash
   # 运行完整的头像功能测试
   java -cp "..." com.yychat.test.AvatarFunctionTest
   ```

3. **检查网络消息**
   - 监控UDP端口3456的消息传输
   - 使用网络抓包工具验证消息格式

## 📊 性能优化

### 缓存策略
- **客户端缓存**：内存缓存已加载的头像
- **磁盘缓存**：本地存储头像文件避免重复下载
- **懒加载**：仅在需要时加载头像

### 网络优化
- **压缩传输**：头像文件自动压缩
- **批量更新**：多个头像更新消息合并处理
- **断线重连**：网络异常时自动重试

## 🔮 未来扩展

### 计划功能
1. **自定义头像编辑**
   - 头像裁剪功能
   - 滤镜效果
   - 贴纸装饰

2. **头像动态效果**
   - GIF动画头像
   - 实时表情头像
   - 3D头像支持

3. **社交功能**
   - 头像点赞系统
   - 头像分享功能
   - 头像等级系统

### API扩展
```java
// 头像管理API
public class AvatarAPI {
    public static String batchUpdateAvatars(Map<String, String> updates);
    public static List<String> getPopularAvatars();
    public static void setAvatarExpiration(String userName, Duration expiration);
}
```

## 📞 技术支持

### 联系方式
- 项目文档：`task-05.md`
- 测试报告：`AvatarFunctionTest.java`
- 部署指南：本文件

### 更新日志
- **v1.0.0**：基础头像功能实现
- **v1.1.0**：增强文件管理和错误处理
- **v1.2.0**：优化性能和用户体验

---

## 🎉 总结

YYChatRoom用户自定义头像功能已全面实现！该系统采用轻量级设计，遵循最小成本原则，在不大幅改动现有架构的情况下，为聊天室增添了丰富的个性化功能。

**核心优势：**
- ✅ 完整实现：6个默认头像 + 自定义头像上传框架
- ✅ 高性能：缓存机制 + 懒加载
- ✅ 易维护：模块化设计 + 清晰代码结构
- ✅ 可扩展：预留接口 + 灵活配置

现在用户可以在聊天室中展现个性，选择或上传自己喜欢的头像了！🎊