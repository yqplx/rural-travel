@echo off
chcp 936 >nul
cd /d "%~dp0"
call "%~dp0config.bat"
call :check %MYSQL_PORT% MySQL
call :check %BACKEND_PORT% Backend
call :check %FRONTEND_PORT% Frontend
pause
exit /b 0
:check
netstat -ano | findstr ":%~1 " | findstr LISTENING >nul 2>&1
if errorlevel 1 (echo [未运行] %~2 端口 %~1) else (echo [运行中] %~2 端口 %~1)
exit /b 0
