#!/bin/bash
set -e

mkdir -p build

# Compila e crea protocol.jar
find src/protocol -name "*.java" > protocol_sources.txt
javac --release 19 -cp "lib/jars/*:secured/*:build" -d build @protocol_sources.txt
rm protocol_sources.txt
find src/protocol -name "*.properties" | while read f; do
    dest="build/${f#src/}"
    mkdir -p "$(dirname "$dest")"
    cp "$f" "$dest"
done
jar cf secured/protocol.jar -C build/protocol .

# Compila e crea absoluta.jar
find src/absoluta -name "*.java" > absoluta_sources.txt
javac --release 19 -cp "lib/jars/*:secured/*:build" -d build @absoluta_sources.txt
rm absoluta_sources.txt
find src/absoluta -name "*.properties" | while read f; do
    dest="build/${f#src/}"
    mkdir -p "$(dirname "$dest")"
    cp "$f" "$dest"
done
jar cf secured/absoluta.jar -C build/absoluta .

echo "Build completed: secured/protocol.jar, secured/absoluta.jar"
