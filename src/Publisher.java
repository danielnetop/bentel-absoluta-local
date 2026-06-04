import java.util.logging.Logger;

final class Publisher {
   private final MqttMessageDispatcher dispatcher;
   private final Logger logger;

   Publisher(MqttMessageDispatcher dispatcher, Logger logger) {
      this.dispatcher = dispatcher;
      this.logger = logger;
   }

   void publish(String topic, String payload, int qos, boolean retained, String context) {
      try {
         dispatcher.publishString(topic, payload, qos, retained);
      } catch (Exception ex) {
         logger.warning("Invio messaggio: " + topic + " (" + context + ") Exception: " + ex.getMessage());
      }
   }
}
