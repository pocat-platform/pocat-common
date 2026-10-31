@echo off

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

java -jar "pocat-launcher.jar" %*

exit /b %ERRORLEVEL%