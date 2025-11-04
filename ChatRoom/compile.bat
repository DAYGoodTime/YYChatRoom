@echo off
setlocal enabledelayedexpansion

:: 检查是否启用调试模式
if "%1"=="debug" (
    set DEBUG_MODE=1
    echo 调试模式已启用
    echo.
) else (
    set DEBUG_MODE=0
)

echo ================================
echo   YYChatRoom 编译脚本
echo ================================
echo.

:: 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误：未找到Java环境，请先安装JDK并配置环境变量
    pause
    exit /b 1
)

:: 检查项目结构和当前目录
echo 当前工作目录: %CD%
echo 项目路径检查:
if exist "src" (
    echo   - src目录: %CD%\src ✅ 存在
) else (
    echo   - src目录: %CD%\src ❌ 不存在
)
if exist "lib" (
    echo   - lib目录: %CD%\lib ✅ 存在
) else (
    echo   - lib目录: %CD%\lib ❌ 不存在
)
echo.

if not exist "src" (
    echo 错误：未找到src目录
    echo 请确保在项目根目录（包含src文件夹的目录）下运行此脚本
    echo.
    pause
    exit /b 1
)

if not exist "lib" (
    echo 警告：未找到lib目录，某些依赖可能无法加载
)

:: 创建输出目录
echo 正在创建输出目录...
if not exist "out" mkdir out
if not exist "out\production" mkdir out\production
if not exist "out\production\ChatRoom" mkdir out\production\ChatRoom

:: 检查lib目录中的jar文件
if exist "lib" (
    echo 找到以下jar文件：
    for %%f in ("lib\*.jar") do (
        echo   - %%~nxf
    )
    echo.
    echo 将使用 -cp "lib/*" 参数进行编译
) else (
    echo 警告：lib目录不存在，将使用默认classpath
)
echo.
echo 开始编译Java源文件...
echo.

:: 统计要编译的文件数量并收集所有文件路径
set COUNT=0
set JAVA_FILES=

echo 收集Java源文件列表...

for /r src %%f in (*.java) do (
    set JAVA_FILES=!JAVA_FILES! "%%f"
    set /a COUNT+=1
)

echo 找到 %COUNT% 个Java源文件
echo.

:: 构建编译命令
set COMPILATION_FILES=!
for /r src %%f in (*.java) do (
    set COMPILATION_FILES=!COMPILATION_FILES! "%%f"
)

:: 显示要编译的文件数量（仅调试模式）
if "!DEBUG_MODE!"=="1" (
    echo 即将编译的文件列表:
    for /r src %%f in (*.java) do (
        echo   - %%f
    )
    echo.
)

:: 执行编译
echo 开始编译...
if "!DEBUG_MODE!"=="1" (
    echo 执行命令: javac -cp "lib/*" -d "out\production\ChatRoom" %COMPILATION_FILES%
    javac -cp "lib/*" -d "out\production\ChatRoom" %COMPILATION_FILES% --release 8
) else (
    :: 正常模式下隐藏错误输出，只显示错误信息
    javac -cp "lib/*" -d "out\production\ChatRoom" %COMPILATION_FILES% --release 8 2>javac_errors.log
)

:: 检查编译结果
if errorlevel 1 (
    echo ❌ 编译失败！
    echo.
    if exist "javac_errors.log" (
        echo ========== 编译错误 ==========
        type javac_errors.log
        echo ==============================
        echo.
        del javac_errors.log 2>nul
    )
    echo 请检查源代码语法错误或依赖问题
    echo.
    echo 🔍 调试信息:
    echo   - 使用classpath: lib/*
    echo   - 目标目录: out\production\ChatRoom
    echo   - 源文件数量: %COUNT%
    echo.
    pause
    exit /b 1
) else (
    echo ✅ 编译成功！
    echo 已清理错误日志文件
    del javac_errors.log 2>nul
)

echo.
echo ================================
echo   编译完成统计
echo ================================
echo 编译的源文件数量: %COUNT% 个
echo 输出目录: out\production\ChatRoom
echo.

:: 验证实际生成的class文件
echo 验证编译结果...
set GENERATED_CLASSES=0
for /r "out\production\ChatRoom" %%c in (*.class) do (
    set /a GENERATED_CLASSES+=1
)

echo 实际生成的class文件数量: %GENERATED_CLASSES%
echo.

if %GENERATED_CLASSES% equ 0 (
    echo ❌ 错误：没有生成任何class文件！
    echo.
    echo 可能的解决方案：
    echo 1. 检查javac命令是否正确执行
    echo 2. 运行调试模式: compile.bat debug
    echo 3. 手动测试: javac -cp "lib/*" -d "out\production\ChatRoom" "src\com\yychat\common\model\User.java"
    echo.
    echo 🔍 调试信息:
    echo   - 当前目录: %CD%
    echo   - 编译命令: javac -cp "lib/*" -d "out\production\ChatRoom" [所有Java文件]
    echo   - 输出目录: %CD%\out\production\ChatRoom
    if exist "out\production\ChatRoom" (
        echo   - 输出目录状态: 存在
    ) else (
        echo   - 输出目录状态: 不存在
    )
    echo.
    pause
    exit /b 1
) else (
    echo ✅ 验证通过：生成了 %GENERATED_CLASSES% 个class文件
    echo.
    echo 一些生成的class文件示例:
    set COUNT_EXAMPLES=0
    for /r "out\production\ChatRoom" %%c in (*.class) do (
        if !COUNT_EXAMPLES! lss 5 (
            echo   - %%~nxc
            set /a COUNT_EXAMPLES+=1
        )
    )
    if !COUNT_EXAMPLES! geq 5 echo   ... 等
)

:: 只有在成功生成class文件时才显示成功信息
if %GENERATED_CLASSES% gtr 0 (
    echo.
    echo 🎉 所有文件编译成功！
    echo.
    echo 编译输出目录: out\production\ChatRoom
    echo.
    echo 运行命令:
    echo   方法1 - 使用启动脚本（推荐）:
    echo     双击运行: run_server.bat （启动服务器）
    echo     双击运行: run_client.bat （启动客户端）
    echo.
    echo   方法2 - 使用命令行:
    echo     java -cp "lib/*;out\production\ChatRoom" com.yychat.server.view.StartServer
    echo     java -cp "lib/*;out\production\ChatRoom" com.yychat.client.ClientMain

    echo.
    echo 💡 提示:
    echo   - 如果遇到问题，可以运行: compile.bat debug
    echo   - 请确保MySQL服务正在运行（数据库名: yychat2022s）
    echo   - 确保端口5678和3457没有被占用
    echo   - 推荐使用启动脚本，避免复杂的classpath设置
) else (
    echo.
    echo ❌ 编译过程有问题，程序终止
)

echo.
pause