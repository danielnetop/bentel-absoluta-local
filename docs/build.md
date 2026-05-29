# Build From Source

## Prerequisites

- Java JDK 19+
- PowerShell (Windows) or Bash (Linux/macOS)
- Docker

## Steps

1. Compile the protocol and panel modules:

```bash
# Linux/macOS
./build_secured.sh

# Windows
.\build_secured.ps1
```

2. Compile the main application:

```bash
# Linux/macOS
./build.sh

# Windows
.\build.ps1
```

3. Build the Docker image:

```bash
docker build -t bentel-absoluta-local-bridge .
```
