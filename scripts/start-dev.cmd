@echo off
setlocal EnableExtensions

if /I "%~1"=="--dry-run" goto dry_run
if not "%~1"=="" goto usage

set "project_root=%~dp0.."
for %%I in ("%project_root%") do set "project_root=%%~fI"

if not exist "%project_root%\backend\pom.xml" goto missing_backend
if not exist "%project_root%\frontend\package.json" goto missing_frontend

where mvn >nul 2>nul
if errorlevel 1 goto missing_maven
where npm >nul 2>nul
if errorlevel 1 goto missing_npm

if exist "%project_root%\.env" (
  for /F "usebackq tokens=1,* delims==" %%A in ("%project_root%\.env") do set "%%A=%%B"
)
if not defined JWT_SECRET (
  for /F "delims=" %%S in ('powershell.exe -NoProfile -Command "$bytes = New-Object byte[] 48; $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create(); $rng.GetBytes($bytes); $rng.Dispose(); [Convert]::ToBase64String($bytes)"') do set "JWT_SECRET=%%S"
)
if not defined JWT_SECRET goto missing_jwt_secret

if not exist "%project_root%\frontend\node_modules\.bin\vite.cmd" (
  echo Installing frontend dependencies...
  pushd "%project_root%\frontend"
  call npm ci
  set "frontend_install_exit=%errorlevel%"
  popd
  if not "%frontend_install_exit%"=="0" goto frontend_install_failed
)

echo Starting backend and frontend in separate command windows...
start "Jingxuan Backend" /D "%project_root%\backend" cmd.exe /k "mvn clean spring-boot:run -Dspring-boot.run.profiles=dev"
start "Jingxuan Frontend" /D "%project_root%\frontend" cmd.exe /k "npm run dev -- --host 127.0.0.1"

echo.
echo Startup requested:
echo   Backend: http://127.0.0.1:8080
echo   Frontend: http://127.0.0.1:5173/jingxuan/
echo.
echo Keep both new windows open. Check the relevant window if startup fails.
pause
exit /b 0

:dry_run
echo [DRY RUN] No services started.
exit /b 0

:usage
echo Usage: double-click start-dev.cmd or run start-dev.cmd [--dry-run]
exit /b 2

:missing_backend
echo [ERROR] backend\pom.xml was not found.
exit /b 1

:missing_frontend
echo [ERROR] frontend\package.json was not found.
exit /b 1

:missing_maven
echo [ERROR] Maven was not found on PATH.
exit /b 1

:missing_npm
echo [ERROR] npm was not found on PATH.
exit /b 1

:missing_jwt_secret
echo [ERROR] Failed to generate a local JWT_SECRET.
exit /b 1

:frontend_install_failed
echo [ERROR] Frontend dependency installation failed.
exit /b 1
