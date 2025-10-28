### 任务-03 计划：修改服务端逻辑以使用线程池

1.  **分析现有服务端代码：**
    *   确定 `Server` 模块中当前的数据处理逻辑。需要重点关注的文件可能是 `Server/src/com/yychat/control/ServerReceiverThread.java` 和 `Server/src/com/yychat/control/YYchatServer.java`。
    *   理解服务器当前如何处理客户端连接和消息。目前很可能是为每个客户端连接创建一个新线程。

2.  **实现线程池：**
    *   在 `YYchatServer.java` 中，实例化一个 `java.util.concurrent.ExecutorService`（例如，使用 `Executors.newFixedThreadPool()`）。这将作为我们的线程池。
    *   修改接受连接的循环。将不再是直接创建并启动一个新的 `ServerReceiverThread`（`new Thread(runnable).start()`），而是将 `ServerReceiverThread`（它应该是一个 `Runnable`）提交给 `ExecutorService`。

3.  **重构 `ServerReceiverThread.java`：**
    *   确保 `ServerReceiverThread.java` 是一个可以由线程池中的线程执行的独立任务。它应该处理从单个客户端读取消息并进行处理。
    *   当前实现中可能包含一个只要客户端连接就一直运行的循环。这没有问题，因为线程池中的线程将专用于该客户端的通信，直到断开连接。

4.  **管理线程池关闭：**
    *   在服务器停止时，为线程池实现一个关闭机制。这可以在 `finally` 块或关闭钩子中完成，以确保 `ExecutorService` 被正确关闭（`executorService.shutdown()`）。

5.  **测试：**
    *   完成更改后，测试服务器以确保它仍然可以并发处理多个客户端。
    *   验证服务器在有许多客户端连接时是否更高效，并且不会创建过多的线程。
