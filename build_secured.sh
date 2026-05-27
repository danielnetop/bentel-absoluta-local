#!/bin/bash
# Script per compilare e creare i JAR di cms, plugin e protocol nella cartella secured (versione Unix)

# 1. Crea la cartella build se non esiste
mkdir -p build

# 2. Compila e crea protocol.jar
find src/protocol -name "*.java" > protocol_sources.txt
javac --release 19 -cp "lib/jars/*:secured/*:build" -d build @protocol_sources.txt
find src/protocol -name "*.properties" | while read f; do
    dest="build/${f#src/}"
    mkdir -p "$(dirname "$dest")"
    cp "$f" "$dest"
done
jar cf secured/protocol.jar -C build/protocol .
rm protocol_sources.txt

# 3. Compila e crea absoluta.jar
find src/absoluta -name "*.java" > absoluta_sources.txt
javac --release 19 -cp "lib/jars/*:secured/*:build" -d build @absoluta_sources.txt
find src/absoluta -name "*.properties" | while read f; do
    dest="build/${f#src/}"
    mkdir -p "$(dirname "$dest")"
    cp "$f" "$dest"
done
jar cf secured/absoluta.jar -C build/absoluta .
rm absoluta_sources.txt

echo "Compilazione e creazione dei JAR in secured/ completata."