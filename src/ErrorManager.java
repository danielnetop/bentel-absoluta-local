import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

final class ErrorManager {
    private static final int MAX_ERRORS = 50;

    private final Object errorsLock = new Object();
    private final Deque<ErrorEvent> errors = new ArrayDeque<>(MAX_ERRORS);
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final Publisher publish;
    private final int qos;

    ErrorManager(Publisher publish, int qos) {
        this.publish = publish;
        this.qos = qos;
    }

    private static final class ErrorEvent {
        @SerializedName("Time")
        final String time;

        @SerializedName("Message")
        final String message;

        ErrorEvent(String time, String message) {
            this.time = time;
            this.message = message;
        }
    }

    void notifyError(String errorMessage) {
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        addError(new ErrorEvent(timestamp, errorMessage));
        publishErrors();
        publish.publish("ABS/errors/last", errorMessage, qos, false, "ultimo errore");
    }

    void resetErrors() {
        clearErrors();
        publishErrors();
    }

    private void addError(ErrorEvent event) {
        synchronized (errorsLock) {
            errors.addLast(event);
            while (errors.size() > MAX_ERRORS) {
                errors.removeFirst();
            }
        }
    }

    private void clearErrors() {
        synchronized (errorsLock) {
            errors.clear();
        }
    }

    private void publishErrors() {
        final String attributesPayload;
        final boolean hasErrors;
        synchronized (errorsLock) {
            hasErrors = !errors.isEmpty();
            attributesPayload = gson.toJson(Map.of("errors", new ArrayList<>(errors)));
        }

        publish.publish("ABS/errors/attributes", attributesPayload, qos, true, "errore centrale");
        publish.publish("ABS/errors", hasErrors ? "Errore!" : "Funzionamento Regolare", qos, true, "errore centrale");
    }
}
