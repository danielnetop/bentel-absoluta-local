import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import absoluta.AbsolutaPanelProvider;
import absoluta.connection.PanelStatus;

final class CommandManager {
    private static final Map<String, PanelStatus.PartitionArming> PARTITION_COMMANDS = Map.of(
            "DISARM", PanelStatus.PartitionArming.DISARMED,
            "ARM_HOME", PanelStatus.PartitionArming.STAY,
            "ARM_AWAY", PanelStatus.PartitionArming.AWAY,
            "ARM_NIGHT", PanelStatus.PartitionArming.NODELAY);

    private static final Map<String, PanelStatus.GlobalArming> GLOBAL_COMMANDS = Map.of(
            "DISARM", PanelStatus.GlobalArming.GLOBALLY_DISARMED,
            "ARM_AWAY", PanelStatus.GlobalArming.GLOBALLY_ARMED);

    private static final Map<String, Character> MODE_COMMANDS = Map.of(
            "MODE_A", 'A',
            "MODE_B", 'B',
            "MODE_C", 'C',
            "MODE_D", 'D');

    private final Entities entities;
    private final AbsolutaPanelProvider provider;
    private final ErrorManager errorManager;
    private final EntityManager entityManager;
    private final Logger logger;

    CommandManager(Entities entities, AbsolutaPanelProvider provider, ErrorManager errorManager,
            EntityManager entityManager, Logger logger) {
        this.entities = entities;
        this.provider = provider;
        this.errorManager = errorManager;
        this.entityManager = entityManager;
        this.logger = logger;
    }

    void onMessageArrived(String topic, MqttMessage msg) {
        if (topic == null) {
            return;
        }

        if (topic.startsWith("ABS/") && topic.endsWith("/set")) {
            String parentTopic = topic.replace("/set", "");
            Integer partitionId = entities.partitionIdByTopic.get(parentTopic);
            if (partitionId != null) {
                if (partitionId == 0) {
                    commandGlobal(msg);
                } else {
                    commandPartition(partitionId, msg);
                }
                return;
            }

            Integer zoneId = entities.sensorIdByTopic.get(parentTopic);
            if (zoneId != null) {
                commandSensor(zoneId, msg);
                return;
            }

            if (parentTopic.contains("mode")) {
                commandMode(msg);
                return;
            }

            if (parentTopic.contains("absoluta_errors")) {
                if (msg != null && msg.toString().equals("RESET_ERRORS")) {
                    logger.info("Resetting errors...");
                    provider.cleanTroubles();
                    errorManager.resetErrors();
                } else {
                    logger.warning("Comando " + (msg == null ? "null" : msg.toString()) + " non valido per il topic: "
                            + topic);
                }
                return;
            }

            logger.warning("Comando non riconosciuto per il topic: " + topic);
            return;
        }

        if (topic.equals("homeassistant/status")) {
            if (msg != null && msg.toString().equals("online")) {
                entityManager.onHomeAssistantOnline();
            }
            return;
        }

        logger.warning("Comando non riconosciuto per il topic: " + topic);
    }

    private void commandPartition(int partitionId, MqttMessage msg) {
        logger.fine("Comando ricevuto per partizione numero: " + partitionId + " nuovo stato: "
                + (msg == null ? "null" : msg.toString()));
        String cmd = normalizeCmd(msg);
        PanelStatus.PartitionArming arming = PARTITION_COMMANDS.get(cmd);
        if (arming == null) {
            logger.warning("Comando " + cmd + " non valido");
            return;
        }
        provider.setPartitionArming(partitionId, arming);
    }

    private void commandGlobal(MqttMessage msg) {
        logger.fine("Comando ricevuto per stato globale: " + (msg == null ? "null" : msg.toString()));
        String cmd = normalizeCmd(msg);
        PanelStatus.GlobalArming arming = GLOBAL_COMMANDS.get(cmd);
        if (arming == null) {
            logger.warning("Comando " + cmd + " non valido");
            return;
        }
        provider.setGlobalArming(arming);
    }

    private void commandMode(MqttMessage msg) {
        logger.fine("Comando ricevuto per modalità: " + (msg == null ? "null" : msg.toString()));
        String cmd = normalizeCmd(msg);
        Character mode = MODE_COMMANDS.get(cmd);
        if (mode == null) {
            logger.warning("Comando " + cmd + " non valido");
            return;
        }
        provider.setModeArming(mode);
    }

    private void commandSensor(int zoneId, MqttMessage msg) {
        if (msg == null) {
            logger.warning("Comando null non valido per il sensore ID: " + zoneId);
            return;
        }

        if (msg.toString().equals("ON")) {
            provider.setZoneBypass(zoneId, true);
        } else if (msg.toString().equals("OFF")) {
            provider.setZoneBypass(zoneId, false);
        } else {
            logger.warning("Comando " + msg.toString() + " non valido per il sensore ID: " + zoneId);
        }
    }

    private static String normalizeCmd(MqttMessage msg) {
        return msg == null ? "" : msg.toString().trim().toUpperCase();
    }
}
