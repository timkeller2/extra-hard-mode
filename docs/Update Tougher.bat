@echo off
setlocal
title Update Tougher
cd /d "%~dp0"
where powershell >nul 2>&1
if errorlevel 1 (
    echo Windows PowerShell is required.
    pause
    exit /b 1
)
if not exist "%~dp0Update-Tougher.ps1" (
    echo The updater script is missing next to this file.
    echo Keep Update-Tougher.ps1 in the same folder as Update Tougher.bat.
    pause
    exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Update-Tougher.ps1" %*
set ERR=%ERRORLEVEL%
echo.
if "%ERR%"=="0" (
    echo Done.
) else (
    echo Update failed.
)
pause
exit /b %ERR%
