import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import cms.device.api.Panel;
import cms.device.spi.PanelProvider;
import plugin.absoluta.AbsolutaPlugin;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Application {
   private static final Logger logger = Logger.getLogger(Application.class.getName());
   // Restituisce il valore della variabile d'ambiente o, se vuota/nulla, dal file di configurazione
   private static String getConfigValue(Properties props, String envKey, String propKey) {
      String value = System.getenv(envKey);
      if (value == null || value.isEmpty()) {
         value = props.getProperty(propKey);
      }
      return value;
   }

   public Application() {
   }

   public static void main(String[] var0) {
      Properties props = new Properties();
      try (FileInputStream fis = new FileInputStream("config.properties")) {
         props.load(fis);
      } catch (IOException e) {
         logger.severe("Impossibile caricare config.properties: " + e.getMessage());
      }

      String MQTT_ADDRESS = getConfigValue(props, "MQTT_ADDRESS", "MQTT_ADDRESS");
      String MQTT_PORT = getConfigValue(props, "MQTT_PORT", "MQTT_PORT");
      String Username = getConfigValue(props, "MQTT_USERNAME", "MQTT_USERNAME");
      String Password = getConfigValue(props, "MQTT_PASSWORD", "MQTT_PASSWORD");
      String ADDRESS = getConfigValue(props, "ALARM_ADDRESS", "ALARM_ADDRESS");
      String PIN = getConfigValue(props, "ALARM_PIN", "ALARM_PIN");
      String PORT = getConfigValue(props, "ALARM_PORT", "ALARM_PORT");
      String MQTT_CONNECT_ATTEMPTS_STR = getConfigValue(props, "MQTT_CONNECT_ATTEMPTS", "MQTT_CONNECT_ATTEMPTS");
      String HOME_ASSISTANT_DISCOVERY = getConfigValue(props, "HOME_ASSISTANT_DISCOVERY", "HOME_ASSISTANT_DISCOVERY");
      String LOG_LEVEL = getConfigValue(props, "LOG_LEVEL", "LOG_LEVEL");

      MemoryPersistence memPers = new MemoryPersistence();

      int MQTT_CONNECT_ATTEMPTS = 5; // Default value
      if (MQTT_CONNECT_ATTEMPTS_STR != null) {
         try {
            MQTT_CONNECT_ATTEMPTS = Integer.parseInt(MQTT_CONNECT_ATTEMPTS_STR);
         } catch (NumberFormatException e) {
            logger.warning("MQTT_CONNECT_ATTEMPTS non è un numero valido, utilizzando il valore predefinito di 5");
         }
      } else {
         logger.warning("MQTT_CONNECT_ATTEMPTS non è definito, utilizzo il valore predefinito di 5");
      }

      boolean discoveryEnabled = true;
      if (HOME_ASSISTANT_DISCOVERY != null) {
         discoveryEnabled = HOME_ASSISTANT_DISCOVERY.equalsIgnoreCase("true");
      } else {
         logger.warning("HOME_ASSISTANT_DISCOVERY non è definito, utilizzo il valore predefinito di true");
      }

      Level logLevel = parseLogLevel(LOG_LEVEL);
      logger.setLevel(logLevel);
      // Imposta anche il livello del ConsoleHandler
      java.util.logging.ConsoleHandler handler = new java.util.logging.ConsoleHandler();
      handler.setLevel(logLevel);
      logger.addHandler(handler);

      for(int i = 0; i < MQTT_CONNECT_ATTEMPTS;  i++) {
         try {
            logger.info("Tentativo di connessione numero: " + (i + 1));
            logger.fine("MQTT_ADDRESS=" + MQTT_ADDRESS);
            logger.fine("MQTT_PORT=" + MQTT_PORT);
            logger.fine("MQTT_USERNAME=" + Username);
            logger.fine("MQTT_PASSWORD=" + (Password != null ? "***" : "non definito"));
            logger.fine("ALARM_ADDRESS=" + ADDRESS);
            logger.fine("ALARM_PIN=" + (PIN != null ? "***" : "non definito"));
            logger.fine("ALARM_PORT=" + PORT);
            logger.fine("HOME_ASSISTANT_DISCOVERY=" + discoveryEnabled);
            // Controllo variabili obbligatorie
            if (MQTT_ADDRESS == null || MQTT_PORT == null || Username == null || Password == null) {
               throw new IllegalArgumentException("MQTT_ADDRESS, MQTT_PORT, MQTT_USERNAME e MQTT_PASSWORD devono essere valorizzati!");
            }
            String mqttServer = "tcp://" + MQTT_ADDRESS + ":" + MQTT_PORT;
            MqttClient mqttClient = new MqttClient(mqttServer, "absolutamqtt", memPers);
            MqttConnectOptions mqttOption = new MqttConnectOptions();
            mqttOption.setCleanSession(true);
            mqttOption.setUserName(Username);
            mqttOption.setPassword(Password.toCharArray());
            logger.info("Collegamento al broker: " + mqttServer);
            HashMap<String, String> map = new HashMap<>();
            map.put("pin", PIN);
            map.put("port", PORT);
            map.put("address", ADDRESS);
            PanelProvider provider = (new AbsolutaPlugin()).newPanel(map);
            Panel panel = new Panel(provider);
            Callback callback = new Callback(mqttClient, panel, mqttOption, discoveryEnabled);
            mqttClient.setCallback(callback);
            mqttClient.connect(mqttOption);
            logger.info("Connesso");
            provider.initialize(callback);

            int maxAttempts = 5;
            int attempt = 0;
            Panel.ConnStatus connStatus = Panel.ConnStatus.UNREACHABLE;
            while (attempt < maxAttempts && connStatus != Panel.ConnStatus.SUCCESS) {
               attempt++;
               logger.info("Tentativo di connessione alla centrale n° " + attempt);
               connStatus = panel.connect();
               if (connStatus != Panel.ConnStatus.SUCCESS) {
                  logger.warning("Connessione fallita: " + connStatus + ". Riprovo tra 90 secondi...");
                  try {
                        Thread.sleep(90000L);
                  } catch (InterruptedException e) {
                        logger.severe("Interrotto durante l'attesa di riconnessione: " + e.getMessage());
                        break;
                  }
               }
            }
            if (connStatus != Panel.ConnStatus.SUCCESS) {
               logger.severe("Impossibile connettersi alla centrale dopo " + maxAttempts + " tentativi.");
               System.exit(1);
            }
         } catch (MqttException ex) {
            logger.warning("Exception: " + ex.getReasonCode());
            logger.warning("Attendo 15 secondi prima del prossimo tentativo...");
            try {
               Thread.sleep(15000L);
            } catch (InterruptedException e) {
               logger.severe("Interruzione durante l'attesa tra i tentativi di connessione: " + e.getMessage());
            }
         }
         i = MQTT_CONNECT_ATTEMPTS; // Forza l'uscita dal ciclo dopo il primo tentativo
      }
   }

   private static Level parseLogLevel(String level) {
      if (level == null || level.isEmpty()) {
         return Level.WARNING;
      }
      switch (level.toUpperCase()) {
         case "SEVERE":
            return Level.SEVERE;
         case "WARNING":
            return Level.WARNING;
         case "INFO":
            return Level.INFO;
         case "CONFIG":
            return Level.CONFIG;
         case "FINE":
            return Level.FINE;
         case "FINER":
            return Level.FINER;
         case "FINEST":
            return Level.FINEST;
         default:
            return Level.WARNING;
      }
   }
}