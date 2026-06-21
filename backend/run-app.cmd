@echo off
set MVN="C:\Users\lucia\AppData\Local\Programs\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd"
cd /d "%~dp0"
call %MVN% spring-boot:run
exit /b %ERRORLEVEL%