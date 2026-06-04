# Bentel Absoluta Local MQTT Bridge

A Java application that acts as a local bridge between a **Bentel Absoluta** security alarm panel and an **MQTT broker**, enabling real-time integration with home automation systems such as Home Assistant.

The bridge communicates directly with the panel over your local network using the ITv2 protocol and exposes all panel entities (partitions, zones, arming modes) as MQTT topics. It supports **Home Assistant MQTT Discovery** out of the box.

---

## Home Assistant OS

If you are running **Home Assistant OS**, you can install this bridge as a native app (formerly called add-on) without Docker or any external setup:

**[ha-absoluta-local-bridge](https://github.com/AlessandroTischer/ha-absoluta-local-bridge)**

The Docker-based setup described below is intended for all other environments (Home Assistant Container, Home Assistant Core, or any standalone deployment).

---

## Requirements

- A **Bentel Absoluta** alarm panel reachable on your local network (IP + port, typically `3064`)
- A running **MQTT broker** (e.g., Mosquitto)
- **Docker** and Docker Compose

---

## Quick Start

1. Copy `docker-compose.yml` and fill in your values:

```yaml
version: "3.8"
services:
  bentel-absoluta:
    image: "axtx2301/bentel-absoluta-local-bridge:latest"
    restart: unless-stopped
    environment:
      MQTT_ADDRESS: "192.168.1.10"
      MQTT_PORT: "1883"
      MQTT_USERNAME: "your_mqtt_user"
      MQTT_PASSWORD: "your_mqtt_password"
      ALARM_ADDRESS: "192.168.1.20"
      ALARM_PIN: "123456"
      ALARM_PORT: "3064"
```

2. Start the container:

```bash
docker compose up -d
```

---

## Documentation

- [Configuration](docs/configuration.md) — all environment variables, security notes, full docker-compose example
- [MQTT Topics](docs/mqtt-topics.md) — published topics and command topics reference
- [Home Assistant Integration & Dashboard](docs/home-assistant.md) — MQTT Discovery, dashboard card templates
- [Troubleshooting](docs/troubleshooting.md)
- [Build From Source](docs/build.md)

---

## Architecture

```
Bentel Absoluta Panel
        │  ITv2 / TCP
        ▼
 AbsolutaPanelProvider
        │
   ConnectionHandler ◄──── PingKeepAlive (TCP keep-alive)
        │
    StatusReader / Commander
        │
     Callback
    ┌───┴────┐
    │        │
EntityManager  CommandManager
    │        │
    └───┬────┘
        │  MQTT (Paho)
        ▼
   MQTT Broker
        │
   Home Assistant
```

---

## Disclaimer

This project is not affiliated with or endorsed by Bentel Security s.r.l.

This software is provided "as is", without any warranty of any kind. Use it at your own risk. The authors assume no responsibility for any malfunctions, damages, or security issues — including but not limited to unintended arming or disarming of the alarm system — arising from the use of this software.

Although this software operates entirely offline and does not send any data to the internet, the authors assume no responsibility for any security breaches or attacks that may result from exposing the alarm panel, the MQTT broker, or the host running this bridge on a network.
