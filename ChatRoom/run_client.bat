@echo off
chcp 936 > nul
java -cp "lib/*;out\production\ChatRoom" com.yychat.client.ClientMain
pause