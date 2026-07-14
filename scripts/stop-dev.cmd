@echo off
setlocal EnableExtensions

set "dry_run=false"
if /I "%~1"=="--dry-run" set "dry_run=true"
if not "%~1"=="" if /I not "%~1"=="--dry-run" goto usage

call :stop_port 8080 Backend
call :stop_port 5173 Frontend

echo.
echo Done. Ports 8080 and 5173 are available for Jingxuan.
pause
exit /b 0

:stop_port
set "port=%~1"
set "label=%~2"
set "found=false"
for /F "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%port% .*LISTENING"') do call :stop_pid "%port%" "%label%" "%%P"
if "%found%"=="false" echo [%label%] No listener on port %port%.
exit /b 0

:stop_pid
set "found=true"
set "port=%~1"
set "label=%~2"
set "pid=%~3"
echo [%label%] Port %port% is owned by PID %pid%.
tasklist /FI "PID eq %pid%" /FO TABLE /NH
if "%dry_run%"=="true" (
  echo [DRY RUN] Would terminate PID %pid% and its child processes.
  exit /b 0
)
taskkill /PID %pid% /T /F
exit /b 0

:usage
echo Usage: double-click stop-dev.cmd or run stop-dev.cmd [--dry-run]
exit /b 2
