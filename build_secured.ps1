# Script PowerShell per compilare e creare i JAR di cms, plugin e protocol nella cartella secured

# 1. Crea la cartella build se non esiste
if (!(Test-Path -Path "build")) {
    New-Item -ItemType Directory -Path "build"
}

# 2. Compila i sorgenti
$allJava = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac --release 19 -cp "lib/jars/*;secured/*" -d build $allJava

# 3. Copia i file .properties nel build mantenendo la struttura dei package
Get-ChildItem -Path src -Recurse -Filter *.properties | ForEach-Object {
    $dest = Join-Path "build" $_.FullName.Substring((Resolve-Path src).Path.Length + 1)
    $destDir = Split-Path $dest -Parent
    if (!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item $_.FullName $dest -Force
}

# 4. Crea i JAR
jar cf secured/protocol.jar -C build/protocol .
jar cf secured/absoluta.jar -C build/absoluta .

Write-Host "Compilazione e creazione dei JAR in secured/ completata."