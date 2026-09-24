# ============================================================
# Script de correction des vulnérabilités Trivy
# - Met à jour Spring Boot 3.4.3 -> 3.4.5
# - Remplace l'image Docker alpine -> jammy
# ============================================================

$rootPath = Get-Location
$services = @(
    "discovery-service",
    "api-gateway",
    "auth-service",
    "user-service",
    "offer-service",
    "reservation-service",
    "product-service",
    "notification-service"
)

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " Correction des vulnérabilités Trivy" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host ""

$modifiedCount = 0
$skippedCount = 0

foreach ($service in $services) {
    $servicePath = Join-Path $rootPath $service

    if (-not (Test-Path $servicePath)) {
        Write-Host "[SKIP] $service (dossier introuvable)" -ForegroundColor Yellow
        $skippedCount++
        continue
    }

    Write-Host "---------------------------------------------------" -ForegroundColor DarkGray
    Write-Host "[SERVICE] $service" -ForegroundColor Green
    Write-Host "---------------------------------------------------" -ForegroundColor DarkGray

    # ============================================================
    # 1. Mise à jour du pom.xml (Spring Boot 3.4.3 -> 3.4.5)
    # ============================================================
    $pomPath = Join-Path $servicePath "pom.xml"

    if (Test-Path $pomPath) {
        $pomContent = Get-Content $pomPath -Raw
        $originalPom = $pomContent

        # Vérifie si le pom contient Spring Boot 3.4.3
        if ($pomContent -match '<version>3\.4\.3</version>') {
            # Backup
            Copy-Item $pomPath "$pomPath.bak" -Force

            # Remplacement
            $pomContent = $pomContent -replace '<version>3\.4\.3</version>', '<version>3.4.5</version>'
            Set-Content $pomPath -Value $pomContent -NoNewline

            Write-Host "  [OK] pom.xml : Spring Boot 3.4.3 -> 3.4.5" -ForegroundColor Green
            $modifiedCount++
        }
        elseif ($pomContent -match '<version>3\.4\.5</version>') {
            Write-Host "  [SKIP] pom.xml : déjà en 3.4.5" -ForegroundColor Yellow
        }
        else {
            Write-Host "  [WARN] pom.xml : version Spring Boot non reconnue" -ForegroundColor Yellow
        }
    }
    else {
        Write-Host "  [SKIP] pom.xml introuvable" -ForegroundColor Yellow
    }

    # ============================================================
    # 2. Mise à jour du Dockerfile (alpine -> jammy)
    # ============================================================
    $dockerfilePath = Join-Path $servicePath "Dockerfile"

    if (Test-Path $dockerfilePath) {
        $dockerfileContent = Get-Content $dockerfilePath -Raw
        $originalDockerfile = $dockerfileContent

        if ($dockerfileContent -match 'eclipse-temurin:21-jre-alpine') {
            # Backup
            Copy-Item $dockerfilePath "$dockerfilePath.bak" -Force

            # Remplacement
            $dockerfileContent = $dockerfileContent -replace 'eclipse-temurin:21-jre-alpine', 'eclipse-temurin:21-jre-jammy'
            Set-Content $dockerfilePath -Value $dockerfileContent -NoNewline

            Write-Host "  [OK] Dockerfile : alpine -> jammy" -ForegroundColor Green
            $modifiedCount++
        }
        elseif ($dockerfileContent -match 'eclipse-temurin:21-jre-jammy') {
            Write-Host "  [SKIP] Dockerfile : déjà en jammy" -ForegroundColor Yellow
        }
        else {
            Write-Host "  [WARN] Dockerfile : image de base non reconnue" -ForegroundColor Yellow
        }
    }
    else {
        Write-Host "  [SKIP] Dockerfile introuvable" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " RÉSUMÉ" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " Services traités    : $($services.Count)" -ForegroundColor White
Write-Host " Modifications       : $modifiedCount" -ForegroundColor Green
Write-Host " Services ignorés    : $skippedCount" -ForegroundColor Yellow
Write-Host ""
Write-Host " Des fichiers .bak ont été créés pour chaque fichier modifié." -ForegroundColor DarkGray
Write-Host " En cas de problème : Get-ChildItem -Recurse -Filter *.bak | Rename-Item -NewName { `$_.Name -replace '\.bak$','' }" -ForegroundColor DarkGray
Write-Host ""
Write-Host " Prochaine étape : git add . && git commit -m 'fix: mise à jour Spring Boot et Dockerfile pour corriger les CVE' && git push" -ForegroundColor Cyan