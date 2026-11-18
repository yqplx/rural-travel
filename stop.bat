@echo off
chcp 936 >nul
cd /d "%~dp0"
call "%~dp0config.bat"
for %%P in (%MYSQL_PORT% %BACKEND_PORT% %FRONTEND_PORT%) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%P " ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1 && echo [停止] 端口 %%P 进程 %%a 已结束
  )
)
echo 完成。可双击 status.bat 复核。
pause
