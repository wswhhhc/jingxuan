param(
    [string]$JavaHome = "D:\java\jdk",
    [string]$ProjectRoot = "D:\AI Demo\菁选",
    [string]$Profile = "dev"
)

$ErrorActionPreference = "Stop"

$backendDir = Join-Path $ProjectRoot "backend"
$javaExe = Join-Path $JavaHome "bin\java.exe"
$jarPath = Join-Path $backendDir "target\jingxuan-backend-1.0.0.jar"

if (-not (Test-Path $javaExe)) {
    throw "Java executable not found: $javaExe"
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$($JavaHome)\bin;$env:Path"

Push-Location $backendDir
try {
    if (-not (Test-Path $jarPath)) {
        Write-Host "Jar not found. Packaging backend ..."
        mvn -q -DskipTests package
    }

    Write-Host "Starting backend with $javaExe"
    & $javaExe -jar $jarPath "--spring.profiles.active=$Profile"
}
finally {
    Pop-Location
}
