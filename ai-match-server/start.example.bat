@echo off
cd /d "%~dp0"
if not exist src\main\resources\application.yml (
    copy src\main\resources\application.example.yml src\main\resources\application.yml >nul
)
if not defined JWT_SECRET (
    echo Please set JWT_SECRET before starting the server.
    exit /b 1
)
if not defined OPENAI_API_KEY (
    echo Please set OPENAI_API_KEY before starting the server.
    exit /b 1
)
call mvn spring-boot:run
