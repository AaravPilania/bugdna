# BugDNA — One-command Demo Reset (Windows PowerShell)
$ErrorActionPreference = "Stop"

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    $fallback = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if (Test-Path $fallback) {
        $mysqlExe = $fallback
    } else {
        Write-Error "mysql.exe not found on PATH or in Program Files. Please add MySQL bin folder to PATH."
    }
} else {
    $mysqlExe = $mysql.Source
}

$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "bugdna" }
$dbPass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "CHANGE_ME_LOCAL_ONLY" }

Write-Host "Resetting BugDNA database to clean pre-demo state..." -ForegroundColor Cyan
Get-Content "$PSScriptRoot\database\reset_demo.sql" | & $mysqlExe -u $dbUser "-p$dbPass"
Write-Host "Done! Seed bugs 101-106 restored (bug #104 is OPEN, next live bug starts at #200)." -ForegroundColor Green

