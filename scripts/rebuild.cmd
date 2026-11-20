@echo off
rem Windows: double-click to fully rebuild the app in Docker (see rebuild.ps1).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rebuild.ps1" %*
exit /b %ERRORLEVEL%
