# Script PowerShell per compilare e creare i JAR di protocol e absoluta nella cartella secured
$ErrorActionPreference = "Stop"

if (!(Test-Path "build")) { New-Item -ItemType Directory -Path "build" | Out-Null }

# Compila e crea protocol.jar
$protocolFiles = Get-ChildItem -Path src/protocol -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac --release 19 -cp "lib/jars/*;secured/*;build" -d build $protocolFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Get-ChildItem -Path src/protocol -Recurse -Filter *.properties | ForEach-Object {
    $dest = Join-Path "build" $_.FullName.Substring((Resolve-Path src).Path.Length + 1)
    $destDir = Split-Path $dest -Parent
    if (!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item $_.FullName $dest -Force
}
jar cf secured/protocol.jar -C build/protocol .

# Compila e crea absoluta.jar
$absolutaFiles = Get-ChildItem -Path src/absoluta -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac --release 19 -cp "lib/jars/*;secured/*;build" -d build $absolutaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Get-ChildItem -Path src/absoluta -Recurse -Filter *.properties | ForEach-Object {
    $dest = Join-Path "build" $_.FullName.Substring((Resolve-Path src).Path.Length + 1)
    $destDir = Split-Path $dest -Parent
    if (!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item $_.FullName $dest -Force
}
jar cf secured/absoluta.jar -C build/absoluta .

Write-Host "Build completed: secured/protocol.jar, secured/absoluta.jar"
