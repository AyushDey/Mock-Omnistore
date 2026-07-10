@echo off
title Mock Omnistore Launcher
cls
echo =====================================================================
echo              MOCK OMNISTORE FULL-STACK LAUNCHER
echo =====================================================================
echo.

:: 1. Validate Maven installation
set "MVN_CMD=mvn"
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    if exist "%USERPROFILE%\apache-maven-3.9.6\bin\mvn.cmd" (
        set "MVN_CMD=%USERPROFILE%\apache-maven-3.9.6\bin\mvn.cmd"
    ) else if exist "..\apache-maven-3.9.6\bin\mvn.cmd" (
        set "MVN_CMD=..\apache-maven-3.9.6\bin\mvn.cmd"
    ) else if exist "apache-maven-3.9.6\bin\mvn.cmd" (
        set "MVN_CMD=apache-maven-3.9.6\bin\mvn.cmd"
    ) else (
        echo [ERROR] Maven (mvn) was not found in PATH or standard directories.
        echo Please ensure Maven is installed and available.
        pause
        exit /b 1
    )
)

:: 2. Validate Node / NPM installation
where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js / npm was not found in PATH.
    echo Please install Node.js (18+) to run the Angular Frontend.
    pause
    exit /b 1
)

:: 3. Check and install frontend dependencies if needed
if not exist "frontend\node_modules\" (
    echo [INFO] frontend/node_modules not found. Running npm install...
    cd frontend
    call npm install
    cd ..
)

echo [SUCCESS] Pre-checks complete. Starting services...
echo.

:: Start Quotation API (Port 8081)
echo [1/3] Launching Quotation API (Port 8081) in a new window...
start "Quotation API (Port 8081)" cmd /k "echo Starting Quotation API... & cd Quotation && "%MVN_CMD%" spring-boot:run"

:: Wait a little for Quotation API to start initializing
timeout /t 3 /nobreak >nul

:: Start Omnistore Backend (Port 8080)
echo [2/3] Launching Omnistore Backend (Port 8080) in a new window...
start "Omnistore Backend (Port 8080)" cmd /k "echo Starting Omnistore Backend... & cd backend && "%MVN_CMD%" spring-boot:run"

:: Wait a little for Backend to start initializing
timeout /t 3 /nobreak >nul

:: Start Angular Frontend (Port 4200)
echo [3/3] Launching Angular Frontend (Port 4200) in a new window...
start "Angular Frontend (Port 4200)" cmd /k "echo Starting Angular Frontend... & cd frontend && npm start"

echo.
echo =====================================================================
echo [SUCCESS] All components have been triggered!
echo.
echo - Quotation API:     http://localhost:8081/api/quotation
echo - Omnistore Backend:   http://localhost:8080/api/products
echo - Angular Frontend:    http://localhost:4200
echo.
echo You can close this launcher window. Keep the service windows open.
echo =====================================================================
pause
