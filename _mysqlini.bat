@echo off
cd /d "%~dp0"
call "%~dp0_env.bat"
call "%~dp0config.bat"
set "INIFILE=%RUNTIME%\mysql\my.ini"
set "BASE_F=%RUNTIME:\=/%/mysql"
set "DATA_F=%DATADIR:\=/%"
set "INIT_F=%ROOT:\=/%data/mysql-init.sql"
> "%INIFILE%" echo [mysqld]
>>"%INIFILE%" echo basedir=%BASE_F%
>>"%INIFILE%" echo datadir=%DATA_F%
>>"%INIFILE%" echo tmpdir=C:/Windows/Temp
>>"%INIFILE%" echo port=%MYSQL_PORT%
>>"%INIFILE%" echo init-file=%INIT_F%
>>"%INIFILE%" echo character-set-server=utf8mb4
>>"%INIFILE%" echo collation-server=utf8mb4_unicode_ci
>>"%INIFILE%" echo default-time-zone=+08:00
>>"%INIFILE%" echo max_connections=200
>>"%INIFILE%" echo sql_mode=STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION
>>"%INIFILE%" echo.
>>"%INIFILE%" echo [mysql]
>>"%INIFILE%" echo default-character-set=utf8mb4
>>"%INIFILE%" echo.
>>"%INIFILE%" echo [client]
>>"%INIFILE%" echo port=%MYSQL_PORT%
>>"%INIFILE%" echo default-character-set=utf8mb4
