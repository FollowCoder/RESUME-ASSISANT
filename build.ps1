# Resume Assistant Build Script
# This script builds both frontend and backend for production deployment

param(
    [switch]$FrontendOnly,
    [switch]$BackendOnly,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

Write-Host "=== Resume Assistant Build Script ===" -ForegroundColor Cyan

# Clean build directories if requested
if ($Clean) {
    Write-Host "Cleaning build directories..." -ForegroundColor Yellow
    if (Test-Path "frontend/dist") {
        Remove-Item -Recurse -Force "frontend/dist"
    }
    if (Test-Path "backend/target") {
        Remove-Item -Recurse -Force "backend/target"
    }
    Write-Host "Clean completed." -ForegroundColor Green
}

# Build Frontend
if (-not $BackendOnly) {
    Write-Host "`n=== Building Frontend ===" -ForegroundColor Cyan
    Set-Location "frontend"
    
    # Install dependencies
    Write-Host "Installing frontend dependencies..." -ForegroundColor Yellow
    npm ci
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Frontend dependency installation failed!" -ForegroundColor Red
        exit 1
    }
    
    # Build for production
    Write-Host "Building frontend for production..." -ForegroundColor Yellow
    npm run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Frontend build failed!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Frontend build completed successfully!" -ForegroundColor Green
    Set-Location ".."
}

# Build Backend
if (-not $FrontendOnly) {
    Write-Host "`n=== Building Backend ===" -ForegroundColor Cyan
    Set-Location "backend"
    
    # Build with Maven
    Write-Host "Building backend with Maven..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Backend build failed!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Backend build completed successfully!" -ForegroundColor Green
    Set-Location ".."
}

Write-Host "`n=== Build Summary ===" -ForegroundColor Cyan
if (-not $BackendOnly) {
    $frontendSize = (Get-ChildItem -Recurse "frontend/dist" | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "Frontend: frontend/dist ($([math]::Round($frontendSize, 2)) MB)" -ForegroundColor Green
}
if (-not $FrontendOnly) {
    $backendJar = Get-ChildItem "backend/target/*.jar" | Where-Object { $_.Name -notmatch "-sources" } | Select-Object -First 1
    if ($backendJar) {
        $backendSize = $backendJar.Length / 1MB
        Write-Host "Backend: $($backendJar.FullName) ($([math]::Round($backendSize, 2)) MB)" -ForegroundColor Green
    }
}

Write-Host "`nBuild completed successfully!" -ForegroundColor Green