@echo off
cd /d "%~dp0"
call "%~dp0_env.bat"
call "%~dp0config.bat"
call "%~dp0_mysqlini.bat"
set "TMP=%TMPDIRX%"
set "TEMP=%TMPDIRX%"
rem MySQL 首次启动需已存在 .err 日志文件，否则 Permission denied 直接退出（实测坑）
if not exist "%DATADIR%\%COMPUTERNAME%.err" echo. 2>"%DATADIR%\%COMPUTERNAME%.err"
start "MySQL-%MYSQL_PORT%" cmd /k ""%MYSQLX%" --defaults-file="%RUNTIME%\mysql\my.ini" --tmpdir="%TMPDIRX%""
