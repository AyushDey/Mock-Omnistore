Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "             MOCK OMNISTORE FULL-STACK LAUNCHER (PowerShell)" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Validate Maven
$mvnCmd = "mvn"
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $userMvn = Join-Path $env:USERPROFILE "apache-maven-3.9.6\bin\mvn.cmd"
    if (Test-Path $userMvn) {
        $mvnCmd = $userMvn
    } elseif (Test-Path "..\apache-maven-3.9.6\bin\mvn.cmd") {
        $mvnCmd = "..\apache-maven-3.9.6\bin\mvn.cmd"
    } elseif (Test-Path "apache-maven-3.9.6\bin\mvn.cmd") {
        $mvnCmd = "apache-maven-3.9.6\bin\mvn.cmd"
    } else {
        Write-Error "Maven (mvn) was not found in PATH or standard directories."
        Read-Host "Press Enter to exit"
        exit
    }
}

# 2. Validate Node/NPM
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Error "Node.js / npm was not found in PATH. Please install Node.js (18+)."
    Read-Host "Press Enter to exit"
    exit
}

# 3. Check and install frontend dependencies if needed
if (-not (Test-Path "frontend\node_modules")) {
    Write-Host "[INFO] frontend/node_modules not found. Running npm install..." -ForegroundColor Yellow
    Push-Location frontend
    npm install
    Pop-Location
}

Write-Host "[SUCCESS] Pre-checks complete. Starting services..." -ForegroundColor Green
Write-Host ""

# Start Quotation API (Port 8081)
Write-Host "[1/3] Launching Quotation API (Port 8081)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle='Quotation API (Port 8081)'; cd Quotation; & '$mvnCmd' spring-boot:run"

# Wait a little for Quotation API to initialize
Start-Sleep -Seconds 3

# Start Omnistore Backend (Port 8080)
Write-Host "[2/3] Launching Omnistore Backend (Port 8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle='Omnistore Backend (Port 8080)'; cd backend; & '$mvnCmd' spring-boot:run"

# Wait a little for Backend to initialize
Start-Sleep -Seconds 3

# Start Angular Frontend (Port 4200)
Write-Host "[3/3] Launching Angular Frontend (Port 4200)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle='Angular Frontend (Port 4200)'; cd frontend; npm start"

Write-Host ""
Write-Host "=====================================================================" -ForegroundColor Green
Write-Host "[SUCCESS] All components have been triggered!" -ForegroundColor Green
Write-Host ""
Write-Host " - Quotation API:     http://localhost:8081/api/quotation"
Write-Host " - Omnistore Backend: http://localhost:8080/api/products"
Write-Host " - Angular Frontend:  http://localhost:4200"
Write-Host ""
Write-Host "You can close this launcher window. Keep the service windows open."
Write-Host "=====================================================================" -ForegroundColor Green
