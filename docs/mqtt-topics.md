# MQTT Topics

All topics use the `ABS/` prefix.

## Published Topics (Bridge → MQTT)

| Topic                          | Description                                      | Values |
|--------------------------------|--------------------------------------------------|--------|
| `ABS/availability`             | Bridge online/offline status (Last Will)         | `online` / `offline` |
| `ABS/conn`                     | Human-readable connection status                 | Text string |
| `ABS/partition/{id}`           | Partition arming state (`0` = global)            | `disarmed` / `armed_away` / `armed_home` / `armed_night` / `triggered` |
| `ABS/sensor/{id}`              | Zone/sensor open state                           | `ON` / `OFF` |
| `ABS/sensor/{id}_bypass`       | Zone bypass state                                | `ON` / `OFF` |
| `ABS/mode/{A\|B\|C\|D}`        | Arming mode button state                         | Text |
| `ABS/panel_faults`             | Panel fault status                               | JSON |
| `ABS/panel_faults/attributes`  | Panel fault details                              | JSON |
| `ABS/bridge_alerts`            | Bridge alert state                               | Text |
| `ABS/bridge_alerts/last`       | Last bridge alert message                        | Text |
| `ABS/bridge_alerts/attributes` | Bridge alert details                             | JSON |

## Command Topics (MQTT → Bridge)

| Topic                               | Description               | Accepted Values |
|-------------------------------------|---------------------------|-----------------|
| `ABS/partition/{id}/set`            | Arm or disarm a partition | `DISARM` / `ARM_HOME` / `ARM_AWAY` / `ARM_NIGHT` |
| `ABS/sensor/{id}/set`              | Toggle zone bypass        | `ON` (bypass) / `OFF` (unbypass) |
| `ABS/mode/{A\|B\|C\|D}/set`        | Activate an arming mode   | `MODE_A` / `MODE_B` / `MODE_C` / `MODE_D` |
| `ABS/absoluta_alarm_memory/set`     | Reset alarm memory        | `RESET_ALARM_MEMORY` |
| `ABS/absoluta_bridge_alerts/set`    | Clear bridge alerts       | `RESET_ERRORS` |
