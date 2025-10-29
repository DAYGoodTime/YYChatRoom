# GEMINI.md

**在交流中使用中文**

## 项目概述

这是一个名为“YYChatRoom”的基于Java的聊天室应用程序。它遵循客户端-服务器架构。两个项目均以JDK8为目标进行设计

*   **服务器:** 服务器是一个多线程的Java应用程序，处理客户端连接、用户身份验证和消息转发。它使用MySQL数据库（`yychat2022s`）来存储用户信息和好友关系。服务器监听3456端口。
*   **客户端:** 客户端是一个Java Swing GUI应用程序，允许用户登录、注册、查看好友列表以及与其他用户聊天。

## 构建和运行

根据`.iml`和`.idea`文件，该项目似乎是一个IntelliJ IDEA项目。

### 依赖

*   **MySQL Connector/J:** 服务器需要MySQL JDBC驱动程序才能连接到数据库。

### 运行应用程序

1.  **设置数据库:**
    *   创建一个名为`yychat2022s`的MySQL数据库。
    *   服务器期望有一个包含`username`和`password`列的`user`表，以及一个`userRelation`表。
    *   数据库连接设置硬编码在`Server/src/com/yychat/control/DBUtil.java`中。您可能需要更改数据库URL、用户名和密码。
    **警告:** 数据库密码硬编码在`DBUtil.java`中。

2.  **运行服务器:**
    *   编译并运行`com.yychat.view.StartServer`类。这将启动服务器并开始在3456端口上监听客户端连接。

3.  **运行客户端:**
    *   编译并运行`com.yychat.view.ClientLogin`类。这将打开登录窗口。然后，您可以注册新用户或使用现有用户登录。

## 开发约定

*   代码被组织到`model`、`view`和`control`包中，遵循模型-视图-控制器（MVC）模式。
*   客户端和服务器之间的通信是通过在套接-字连接上发送序列化的Java `Message`对象来完成的。
*   服务器是多线程的，每个客户端连接都在一个单独的`ServerReceiverThread`中处理。