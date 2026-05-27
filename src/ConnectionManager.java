import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import absoluta.AbsolutaPanelProvider;
import absoluta.AbsolutaPanelProvider.providerConnStatus;

final class ConnectionManager {
    private static final int RECON_DELAY_SECONDS = 90;
    private static final int MAX_MQTT_RECONNECT_ATTEMPTS = 10;

    private final MqttClient mqttClient;
    private final MqttConnectOptions mqttConnOpts;
    private final AbsolutaPanelProvider provider;
    private final Publisher publisher;
    private final Logger logger;
    private final int qos;
    private final EntityManager entityManager;

    private int panelReconnectionAttempts = 0;
    private int mqttReconnectAttempts = 0;
    private boolean isPanelConnected = false;

    ConnectionManager(MqttClient mqttClient, MqttConnectOptions mqttConnOpts, AbsolutaPanelProvider provider,
            Publisher publisher, Logger logger, int qos, EntityManager entityManager) {
        this.mqttClient = mqttClient;
        this.mqttConnOpts = mqttConnOpts;
        this.provider = provider;
        this.publisher = publisher;
        this.logger = logger;
        this.qos = qos;
        this.entityManager = entityManager;
    }

    void onPanelConnected() {
        if (!isPanelConnected) {
            isPanelConnected = true;
            publisher.publish("ABS/conn", "Status: Connesso", qos, false, "connessione iniziale");
            publisher.publish(HomeAssistantManager.AVAILABILITY_TOPIC, "online", qos, true, "disponibilità online");
        }

        try {
            mqttClient.subscribe("ABS/+/set");
            mqttClient.subscribe("ABS/+/+/set");
            mqttClient.subscribe("homeassistant/status");
        } catch (Exception ex) {
            logger.warning("Errore durante la subscribe ai topic: " + ex.getMessage());
        }
    }

    void onMqttConnectionLost(Throwable ex) {
        String msg = ex == null ? "" : ex.getMessage();
        logger.warning("Connessione persa con il broker MQTT: " + msg + ". Riconnessione...");
        reconnectWithDelay("broker MQTT");
    }

    void onPanelConnectionLost() {
        logger.warning("Connessione persa con la centrale! Riconnessione...");
        reconnectWithDelay("centrale");
    }

    private void reconnectWithDelay(String objName) {
        try {
            if (objName.equals("centrale")) {
                isPanelConnected = false;
                ++panelReconnectionAttempts;
                logger.warning("Tentativo di riconnessione " + panelReconnectionAttempts + " a " + objName + " in "
                        + RECON_DELAY_SECONDS + " secondi...");
                publisher.publish("ABS/conn", "Status: Scollegato", qos, false, "disconnessione forzata");
                publisher.publish(HomeAssistantManager.AVAILABILITY_TOPIC, "offline", qos, true, "disponibilità offline");

                TimeUnit.SECONDS.sleep(RECON_DELAY_SECONDS);
                providerConnStatus status = provider.connect();
                if (status == providerConnStatus.UNREACHABLE) {
                    throw new Exception("Busy panel");
                } else if (status == providerConnStatus.SUCCESS) {
                    panelReconnectionAttempts = 0;
                    isPanelConnected = true;
                    publisher.publish("ABS/conn", "Status: Connesso", qos, false, "riconnessione riuscita");
                    publisher.publish(HomeAssistantManager.AVAILABILITY_TOPIC, "online", qos, true, "disponibilità online");
                    entityManager.republishAllStates();
                }

                return;
            }

            if (objName.equals("broker MQTT")) {
                if (mqttReconnectAttempts >= MAX_MQTT_RECONNECT_ATTEMPTS) {
                    logger.severe(
                            "Raggiunto il numero massimo di tentativi di riconnessione al broker MQTT. Riavviare l'applicazione.");
                    return;
                }

                ++mqttReconnectAttempts;
                logger.warning("Tentativo di riconnessione " + mqttReconnectAttempts + "/" + MAX_MQTT_RECONNECT_ATTEMPTS
                        + " al broker MQTT in " + RECON_DELAY_SECONDS + " secondi...");

                try {
                    if (mqttClient.isConnected()) {
                        mqttClient.disconnect();
                    }

                    TimeUnit.SECONDS.sleep(RECON_DELAY_SECONDS);
                    mqttClient.connect(mqttConnOpts);
                    mqttReconnectAttempts = 0;

                    // Resubscribe to topics after reconnection
                    mqttClient.subscribe("ABS/+/set");
                    mqttClient.subscribe("ABS/+/+/set");
                    mqttClient.subscribe("homeassistant/status");

                    logger.info("Riconnessione al broker MQTT riuscita");
                } catch (Exception e) {
                    handleReconnectionFailure(objName, e);
                }

                return;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            handleReconnectionFailure(objName, ex);
        } catch (Exception ex) {
            handleReconnectionFailure(objName, ex);
        }
    }

    private void handleReconnectionFailure(String name, Exception ex) {
        logger.warning("Impossibile riconnettersi a " + name + " Causa: " + ex.getMessage());
        ex.printStackTrace();

        boolean shouldRetry = name.equals("centrale")
                || (name.equals("broker MQTT") && mqttReconnectAttempts < MAX_MQTT_RECONNECT_ATTEMPTS);
        if (!shouldRetry) {
            return;
        }

        new Thread(() -> reconnectWithDelay(name)).start();
    }
}
