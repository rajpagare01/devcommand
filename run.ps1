param(
    [switch]$Docker,
    [switch]$Maven
)

Write-Host "DevCommand Local Runner" -ForegroundColor Cyan

if ($Docker) {
    Write-Host "Starting project via Docker Compose..." -ForegroundColor Green
    docker compose up -d --build
    exit $LASTEXITCODE
}

Write-Host "Loading .env variables..." -ForegroundColor Yellow
if (Test-Path ".env") {
    Get-Content .env | Where-Object { $_ -match '^([^#=]+)=(.*)$' } | ForEach-Object {
        $key = $Matches[1].Trim()
        $val = $Matches[2].Trim()
        if ($key -eq "DATABASE_URL") {
            # Map postgres host to localhost for Maven execution
            $val = $val -replace "postgres:5432", "localhost:5432"
        }
        Set-Item -Path "env:$key" -Value $val
        Write-Host "Loaded $key" -ForegroundColor Gray
    }
} else {
    Write-Host "Warning: .env file not found." -ForegroundColor Red
}

Write-Host "Starting backend via Maven..." -ForegroundColor Green
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
