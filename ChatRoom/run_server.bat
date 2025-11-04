@echo off
chcp  936 > nul
java -cp "lib/*;out\production\ChatRoom" com.yychat.server.view.StartServer
pause