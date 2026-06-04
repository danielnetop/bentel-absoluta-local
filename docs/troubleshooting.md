# Troubleshooting

## Bridge connects to MQTT but not to the panel

- Verify the panel IP and port are reachable from the bridge host: `telnet <ALARM_ADDRESS> <ALARM_PORT>`
- Check that the PIN is correct and has remote access permissions on the panel

## Entities not appearing in Home Assistant

- Ensure `HOME_ASSISTANT_DISCOVERY` is not set to `false`
- Check that the MQTT integration in Home Assistant is connected to the same broker
- Restart the bridge to re-send discovery payloads

## Increase logging verbosity

Set `LOG_LEVEL: "FINE"` for detailed logs suitable for sharing when reporting an issue.

> **Warning:** Do not use `FINER` or `FINEST` in production — from `FINER` onwards, the panel PIN is written in plaintext in the logs.
