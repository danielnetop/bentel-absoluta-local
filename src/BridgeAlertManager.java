import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

final class BridgeAlertManager {
    private static final int MAX_ALERTS = 50;

    private final Object alertsLock = new Object();
    private final Deque<AlertEvent> alerts = new ArrayDeque<>(MAX_ALERTS);
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final Publisher publish;
    private final int qos;

    BridgeAlertManager(Publisher publish, int qos) {
        this.publish = publish;
        this.qos = qos;
    }

    private static final class AlertEvent {
        @SerializedName("Time")
        final String time;

        @SerializedName("Message")
        final String message;

        AlertEvent(String time, String message) {
            this.time = time;
            this.message = message;
        }
    }

    void notifyAlert(String message) {
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        addAlert(new AlertEvent(timestamp, message));
        publishAlerts();
        publish.publish("ABS/bridge_alerts/last", message, qos, false, "ultimo avviso bridge");
    }

    void resetAlerts() {
        clearAlerts();
        publishAlerts();
    }

    private void addAlert(AlertEvent event) {
        synchronized (alertsLock) {
            alerts.addLast(event);
            while (alerts.size() > MAX_ALERTS) {
                alerts.removeFirst();
            }
        }
    }

    private void clearAlerts() {
        synchronized (alertsLock) {
            alerts.clear();
        }
    }

    void publishAlerts() {
        final String attributesPayload;
        final boolean hasAlerts;
        synchronized (alertsLock) {
            hasAlerts = !alerts.isEmpty();
            attributesPayload = gson.toJson(Map.of("avvisi", new ArrayList<>(alerts)));
        }

        publish.publish("ABS/bridge_alerts/attributes", attributesPayload, qos, true, "avviso bridge");
        publish.publish("ABS/bridge_alerts", hasAlerts ? "Avviso!" : "Funzionamento Regolare", qos, true, "avviso bridge");
    }
}
