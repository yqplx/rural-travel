@echo off
chcp 936 >nul
title 乡野智游 - 一键启动
cd /d "%~dp0"
call "%~dp0config.bat"
echo.
echo ============================================
echo  [乡野智游] 一键启动（三个服务）
echo  注意：路径必须为纯英文，否则 MySQL/esbuild 会崩
echo ============================================
echo.
echo [1/3] 启动 MySQL ...
call "%~dp0start-mysql.bat"
set /a n=0
:waitmy
netstat -ano | findstr ":%MYSQL_PORT% " | findstr LISTENING >nul 2>&1
if %errorlevel%==0 goto my_ready
set /a n+=1
if %n% geq 30 goto my_timeout
ping -n 3 127.0.0.1 >nul
goto waitmy
:my_timeout
echo [WARN] MySQL 未在 60s 内就绪，请检查是否解压到英文路径。
goto my_done
:my_ready
echo [OK] MySQL %MYSQL_PORT% is up.
:my_done

echo [2/3] 启动后端（约 20-40 秒）...
call "%~dp0start-backend.bat"
set /a n=0
:waitbe
netstat -ano | findstr ":%BACKEND_PORT% " | findstr LISTENING >nul 2>&1
if %errorlevel%==0 goto be_ready
set /a n+=1
if %n% geq 50 goto be_timeout
ping -n 3 127.0.0.1 >nul
goto waitbe
:be_timeout
echo [WARN] 后端未在 100s 内就绪，请看后端窗口日志。
goto be_done
:be_ready
echo [OK] Backend %BACKEND_PORT% is up.
:be_done

echo [3/3] 启动前端 ...
call "%~dp0start-frontend.bat"
set /a n=0
:waitfe
netstat -ano | findstr ":%FRONTEND_PORT% " | findstr LISTENING >nul 2>&1
if %errorlevel%==0 goto fe_ready
set /a n+=1
if %n% geq 40 goto fe_timeout
ping -n 3 127.0.0.1 >nul
goto waitfe
:fe_timeout
echo [WARN] 前端未在 80s 内就绪，请看前端窗口日志。
goto fe_done
:fe_ready
echo [OK] Frontend %FRONTEND_PORT% is up.
:fe_done

echo.
echo ============================================
echo  ALL READY. 打开浏览器：
echo    游客端 : http://localhost:%FRONTEND_PORT%
echo    大屏   : http://localhost:%FRONTEND_PORT%/bigscreen
echo    API    : http://localhost:%BACKEND_PORT%/api
echo  停止：双击 stop.bat 或关闭三个黑窗口。
echo ============================================
pause
