# BugDNA — One-command Demo Launcher (Windows PowerShell)
$ErrorActionPreference = "Stop"

Write-Host "Starting BugDNA full-stack demo..." -ForegroundColor Cyan

# 1. Start Spring Boot Backend (:8080)
Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\backend'; .\mvnw.cmd spring-boot:run"

# 2. Start BugDNA Dashboard (:5173)
Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\dashboard'; npm run dev"

# 3. Start Kova Goods Demo Store (:5174)
Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\demo-store'; npm run dev"

Write-Host "Waiting for backend at http://localhost:8080/api/v1/health ..." -ForegroundColor Yellow
for ($i = 0; $i -lt 30; $i++) {
    try {
        $h = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health" -TimeoutSec 2
        if ($h.status -eq "UP") {
            Write-Host "`nAll BugDNA services are ready!" -ForegroundColor Green
            Write-Host "  Dashboard  : http://localhost:5173  (login: dev / password123)"
            Write-Host "  Demo Store : http://localhost:5174  (coupon: SAVE20 -> Checkout)"
            Write-Host "  Backend API: http://localhost:8080/api/v1/health"
            Write-Host "  Reset DB   : .\reset-demo.ps1"
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Host "Backend is still starting; check the backend terminal window for logs." -ForegroundColor Yellow

