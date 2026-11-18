@echo off
cd /d "%~dp0"
call "%~dp0_env.bat"
call "%~dp0config.bat"
set "DB_USER=%DB_USER%"
set "DB_PASS=%DB_PASS%"
set "SITE_BASE_URL=http://localhost:%FRONTEND_PORT%"
rem jar 内 jdbc URL 写死 3307，用 Spring 环境变量按 config.bat 覆盖，支持改端口
set "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:%MYSQL_PORT%/rural_travel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false"
start "Backend-%BACKEND_PORT%" cmd /k ""%JAVAX%" -jar "%JAR%" --server.port=%BACKEND_PORT%"
