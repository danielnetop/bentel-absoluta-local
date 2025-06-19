import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import cms.device.api.Panel;
import cms.device.api.Panel.ConnStatus;
import cms.device.spi.PanelProvider;
import plugin.absoluta.connection.PanelStatus;

import java.util.logging.Logger;

class Callback implements PanelProvider.PanelCallback, MqttCallback {
   private static final Logger logger = Logger.getLogger(Callback.class.getName());
   private MqttClient mqttClient;
   private int[] sensorIDs;
   private int[] partitionIDs;
   private String[] sensorNames;
   private String[] partitionNames;
   private String[] sensorStatuses;
   private String[] sensorBypass;
   private String[] partitionArmStatuses;
   private String[] partitionStatuses;
   private String[] sensorTopics;
   private String[] partitionTopics;
   private String[] modeNames = new String[4];
   private Panel panel;
   private MqttConnectOptions connOpts;
   private int reconnectionAttempts = 0;
   private boolean isConnected = false;
   private static final int RECON_DELAY = 90;
   private static final int QOS = 1;
   private HashSet<Integer> sensorDiscoverySent = new HashSet<>();
   private HashSet<Integer> partitionDiscoverySent = new HashSet<>();
   private HashSet<Integer> modeDiscoverySent = new HashSet<>();
   private Boolean discoveryEnabled;
   private MqttMessageDispatcher mqttDispatcher;

   public Callback(MqttClient mqttClient, Panel panel, MqttConnectOptions mqttOption, Boolean discoveryEnabled) {
      this.mqttClient = mqttClient;
      this.panel = panel;
      this.connOpts = mqttOption;
      this.discoveryEnabled = discoveryEnabled;
      this.mqttDispatcher = new MqttMessageDispatcher(mqttClient);
   }

   // Helper per publish MQTT con gestione eccezioni
   private void safePublish(String topic, String payload, int qos, boolean retained, String context) {
      try {
         this.mqttDispatcher.publishString(topic, payload, qos, retained);
      } catch (Exception ex) {
         logger.warning("Invio messaggio: " + topic + " (" + context + ") Exception: " + ex.getMessage());
      }
   }

   public void connectionLost(Throwable ex) {
      logger.warning("Connessione persa con il broker MQTT: " + ex.getMessage() + ". Riconnessione...");
      this.reconnectWithDelay("broker MQTT");
   }

   public void connectionLost() {
      logger.warning("Connessione persa con la centrale! Riconnessione...");
      this.reconnectWithDelay("centrale");
   }

   public void alert(String var1) {
   }

   public void changeInputs(List<String> msg) {
      this.sensorIDs = new int[msg.size()];
      int maxId = 0;
      for (int i = 0; i < msg.size(); i++) {
         int id = Integer.parseInt(msg.get(i));
         this.sensorIDs[i] = id;
         if (id > maxId) maxId = id;
      }
      this.sensorNames = new String[maxId + 1];
      this.sensorTopics = new String[maxId + 1];
      this.sensorStatuses = new String[maxId + 1];
      this.sensorBypass = new String[maxId + 1];
      logger.fine("Sensore ID: " + msg);
   }

   public void changeOutputs(List<String> var1) {
   }

   public void changePartitions(List<String> msg) {
      /*
      Riceve come informazione quante partizioni ha il sistema
      e crea gli array per i nomi, i topic e gli stati delle partizioni.
      Sposta tutte le partizioni lasciando la zero dedicata alla partizione globale.
      */
      this.partitionIDs = new int[msg.size() + 1];
      this.partitionIDs[0] = 0;
      for (int i = 0; i < msg.size(); i++) {
         this.partitionIDs[i + 1] = Integer.parseInt(msg.get(i));
      }

      this.partitionNames = new String[this.partitionIDs.length];
      this.partitionTopics = new String[this.partitionIDs.length];
      this.partitionArmStatuses = new String[this.partitionIDs.length];
      this.partitionStatuses = new String[this.partitionIDs.length];
      this.partitionNames[0] = "Globale";
      this.partitionTopics[0] = "ABS/partition/0";

      // Invia discovery per la partizione globale (id 0)
      if (discoveryEnabled && !partitionDiscoverySent.contains(0)) {
         String topic = "homeassistant/alarm_control_panel/absoluta_partition_0/config";
         String payload = HomeAssistantManager.buildPartition("0", partitionNames[0]);
         safePublish(topic, payload, QOS, true, "discovery partizione globale");
         partitionDiscoverySent.add(0);
         // Subscribe al topic di homeassistant
         try {
            this.mqttClient.subscribe("homeassistant/status");
         } catch (Exception ex) {
            logger.warning("Subscribe to: homeassistant/status");
         }
      }

      if (!this.isConnected){
         this.isConnected = true;
         safePublish("ABS/conn", "Status: Connesso", QOS, false, "connessione iniziale");
      }

      try {
         this.mqttClient.subscribe("ABS/+/set");
         this.mqttClient.subscribe("ABS/+/+/set");
      } catch (Exception ex) {
         logger.warning("Errore durante la subscribe ai topic delle partizioni: " + ex.getMessage());
      }
      logger.fine("Partizione ID: " + String.valueOf(msg));
   }

   public void setArming(PanelStatus.globalArming actArming) {
      if (actArming == PanelStatus.globalArming.GLOBALLY_DISARMED) {
         this.partitionArmStatuses[0] = "disarmed";
      } else if (actArming == PanelStatus.globalArming.GLOBALLY_ARMED) {
         this.partitionArmStatuses[0] = "armed_away";
      } else if (actArming == PanelStatus.globalArming.PARTIALLY_ARMED) {
         this.partitionArmStatuses[0] = "armed_custom_bypass";
      }
      this.sendMessageOnsetArming();
   }

   public void sendMessageOnsetArming() {
      String str = "";
      if(discoveryEnabled){
         if (this.partitionArmStatuses[0] == null) {
            str = "disarmed";
         } else {
            str = this.partitionArmStatuses[0];
         }
      } else {
         str = "Name: " + this.partitionNames[0] + " Arming: " + this.partitionArmStatuses[0] + " Status: " + this.partitionStatuses[0];
      }
      safePublish(this.partitionTopics[0], str, QOS, false, "stato partizione globale");
      logger.fine("Partition Name: " + this.partitionNames[0] + " Arming: " + this.partitionArmStatuses[0] + " Status: " + this.partitionStatuses[0]);
   }

   public void setInputRemoteName(String sensorID, String sensorName) {
      int sensorIDInt = Integer.parseInt(sensorID);
      if (sensorIDInt < 0 || sensorIDInt >= sensorNames.length) {
         logger.warning("Indice sensore fuori dai limiti: " + sensorIDInt);
         return;
      }
      this.sensorNames[sensorIDInt] = sensorName;
      this.sensorTopics[sensorIDInt] = "ABS/sensor/" + sensorIDInt;
      // Invia discovery solo la prima volta per ogni sensore
      if (discoveryEnabled && !sensorDiscoverySent.contains(sensorIDInt)) {
         String topic = "homeassistant/binary_sensor/absoluta_sensor_" + sensorIDInt + "/config";
         String payload = HomeAssistantManager.buildSensor(sensorID, sensorNames[sensorIDInt]);
         safePublish(topic, payload, QOS, true, "discovery sensore");
         sensorDiscoverySent.add(sensorIDInt);

         topic = "homeassistant/switch/absoluta_sensor_" + sensorIDInt + "_bypass/config";
         payload = HomeAssistantManager.buildSensorBypass(sensorID, sensorNames[sensorIDInt]);
         safePublish(topic, payload, QOS, true, "discovery sensore Bypass");
      }
      String str = "";
      if(discoveryEnabled){
         str = (this.sensorStatuses[sensorIDInt] == null) ? "OFF" : this.sensorStatuses[sensorIDInt].toUpperCase();
      } else {
         str = "Name: " + this.sensorNames[sensorIDInt] + " Status: " + this.sensorStatuses[sensorIDInt] + " Bypass: " + this.sensorBypass[sensorIDInt];
      }
      safePublish(this.sensorTopics[sensorIDInt], str, QOS, false, "stato sensore");
      logger.fine("Sensor Name: " + this.sensorNames[sensorIDInt] + " Status: " + this.sensorStatuses[sensorIDInt] + " Bypass: " + this.sensorBypass[sensorIDInt]);
   }

   public void setInputStatus(String sensorID, PanelStatus.inputStatus sensorStatus) {
      int sensorIDInt = Integer.parseInt(sensorID);
      if (sensorIDInt < 0 || sensorIDInt >= sensorStatuses.length) {
         logger.warning("Indice sensore fuori dai limiti: " + sensorIDInt);
         return;
      }
      if (sensorStatus != PanelStatus.inputStatus.ACTIVE && sensorStatus != PanelStatus.inputStatus.ALARM) {
         this.sensorStatuses[sensorIDInt] = "Off";
      } else {
         this.sensorStatuses[sensorIDInt] = "On";
      }

      if (this.panel.getBypassInput(sensorID)) {
         this.sensorBypass[sensorIDInt] = "ON";
      } else {
         this.sensorBypass[sensorIDInt] = "OFF";
      }

      this.sendMessageOnsetInputStatus(sensorIDInt);
   }

   public void sendMessageOnsetInputStatus(int sensorID) {
      if (sensorID < 0 || sensorID >= sensorNames.length) {
         logger.warning("Indice sensore fuori dai limiti: " + sensorID);
         return;
      }
      if (this.sensorNames[sensorID] == null) {
         logger.fine("Nome sensore nullo: " + sensorID + ", non invio stato.");
         return;
      }
      String str = "";
      if(discoveryEnabled){
         String topic = "ABS/sensor/" + sensorID + "_bypass";
         str = (this.sensorBypass[sensorID] == null) ? "OFF" : this.sensorBypass[sensorID].toUpperCase();
         safePublish(topic, str, QOS, false, "bypass sensore");
         str = (this.sensorStatuses[sensorID] == null) ? "OFF" : this.sensorStatuses[sensorID].toUpperCase();
      } else {
         str = "Name: " + this.sensorNames[sensorID] + " Status: " + this.sensorStatuses[sensorID] + " Bypass: " + this.sensorBypass[sensorID];
      }
      safePublish(this.sensorTopics[sensorID], str, QOS, false, "stato sensore");
      logger.fine("Sensor Name: " + this.sensorNames[sensorID] + " Status: " + this.sensorStatuses[sensorID] + " Bypass: " + this.sensorBypass[sensorID]);
   }

   public void setLabelArming(char modeChar, String modeLabel) {
      int modeIDInt = -1;
      switch (modeChar) {
         case 'A':
            if (modeLabel == null || modeLabel.trim().isEmpty()) {
               modeDiscoverySent.add(modeIDInt);
               break;
            }
            modeIDInt = 0;
            this.modeNames[modeIDInt] = modeLabel;
            break;
         case 'B':
            if (modeLabel == null || modeLabel.trim().isEmpty()) {
               modeDiscoverySent.add(modeIDInt);
               break;
            }
            modeIDInt = 1;
            this.modeNames[modeIDInt] = modeLabel;
            break;
         case 'C':
            if (modeLabel == null || modeLabel.trim().isEmpty()) {
               modeDiscoverySent.add(modeIDInt);
               break;
            }
            modeIDInt = 2;
            this.modeNames[modeIDInt] = modeLabel;
            break;
         case 'D':
            if (modeLabel == null || modeLabel.trim().isEmpty()) {
               modeDiscoverySent.add(modeIDInt);
               break;
            }
            modeIDInt = 3;
            this.modeNames[modeIDInt] = modeLabel;
            break;
      }
      // Invia discovery solo la prima volta per ogni modalità
      if (discoveryEnabled && !modeDiscoverySent.contains(modeIDInt)) {
         String topic = "homeassistant/button/absoluta_mode_" + modeIDInt + "/config";
         String payload = HomeAssistantManager.buildMode(modeChar, modeLabel);
         safePublish(topic, payload, QOS, true, "discovery modalità");
         modeDiscoverySent.add(modeIDInt);
      }
   }

   public void setOutputRemoteName(String var1, String var2) {
   }

   public void setOutputStatus(String var1, PanelStatus.outputStatus var2) {
   }

   public void setPartitionArming(String partitionID, PanelStatus.partitionArming actArming) {
      int partitionIDInt = Integer.parseInt(partitionID);
      if (partitionIDInt < 0 || partitionIDInt >= partitionArmStatuses.length) {
         logger.warning("Indice partizione fuori dai limiti: " + partitionIDInt);
         return;
      }
      if (actArming == PanelStatus.partitionArming.DISARMED) {
         this.partitionArmStatuses[partitionIDInt] = "disarmed";
      } else if (actArming == PanelStatus.partitionArming.AWAY) {
         this.partitionArmStatuses[partitionIDInt] = "armed_away";
      } else if (actArming == PanelStatus.partitionArming.STAY) {
         this.partitionArmStatuses[partitionIDInt] = "armed_home";
      } else if (actArming == PanelStatus.partitionArming.NODELAY) {
         this.partitionArmStatuses[partitionIDInt] = "armed_night";
      } else if (actArming == PanelStatus.partitionArming.TRIGGERED) {
         this.partitionArmStatuses[partitionIDInt] = "triggered";
      }

      this.sendMessageOnsetPartitionArming(partitionIDInt);
   }

   public void sendMessageOnsetPartitionArming(int partitionID) {
      if (partitionID < 0 || partitionID >= partitionNames.length || this.partitionNames[partitionID] == null) {
         logger.warning("Indice partizione fuori dai limiti o nome nullo: " + partitionID);
         return;
      }
      String str = "";
      if(discoveryEnabled){
         if (this.partitionArmStatuses[partitionID] == null) {
            str = "disarmed";
         } else {
            str = this.partitionArmStatuses[partitionID];
         }
      } else {
         str = "Name: " + this.partitionNames[partitionID] + " Arming: " + this.partitionArmStatuses[partitionID] + " Status: " + this.partitionStatuses[partitionID];
      }
            this.mqttDispatcher.publishString(this.partitionTopics[partitionID], str, QOS, false);
      safePublish(this.partitionTopics[partitionID], str, QOS, false, "stato partizione");
      logger.fine("Partition Name: " + this.partitionNames[partitionID] + " Arming: " + this.partitionArmStatuses[partitionID] + " Status: " + this.partitionStatuses[partitionID]);
   }

   public void setPartitionRemoteName(String partitionID, String partitionName) {
      int partitionIDInt = Integer.parseInt(partitionID);
      // Controlla se l'array è sufficientemente grande per contenere la partizione
      if (partitionIDInt >= partitionNames.length) {
         int newSize = partitionIDInt + 1;
         partitionNames = java.util.Arrays.copyOf(partitionNames, newSize);
         partitionTopics = java.util.Arrays.copyOf(partitionTopics, newSize);
         partitionArmStatuses = java.util.Arrays.copyOf(partitionArmStatuses, newSize);
         partitionStatuses = java.util.Arrays.copyOf(partitionStatuses, newSize);
      }
      this.partitionNames[partitionIDInt] = partitionName;
      this.partitionTopics[partitionIDInt] = "ABS/partition/" + partitionIDInt;
      // Invia discovery solo la prima volta per ogni partizione
      if (discoveryEnabled && !partitionDiscoverySent.contains(partitionIDInt)  && partitionIDInt > 0) {
         String topic = "homeassistant/alarm_control_panel/absoluta_partition_" + partitionID + "/config";
         String payload = HomeAssistantManager.buildPartition(partitionID, partitionName);
         safePublish(topic, payload, QOS, true, "discovery partizione");
         partitionDiscoverySent.add(partitionIDInt);
      }

      String str = "";
      if(discoveryEnabled){
         if (this.partitionArmStatuses[partitionIDInt] == null) {
            str = "disarmed";
         } else {
            str = this.partitionArmStatuses[partitionIDInt];
         }
      } else {
         str = "Name: " + this.partitionNames[partitionIDInt] + " Arming: " + this.partitionArmStatuses[partitionIDInt] + " Status: " + this.partitionStatuses[partitionIDInt];
      }
      safePublish(this.partitionTopics[partitionIDInt], str, QOS, false, "stato partizione");
      logger.fine("Partition Name: " + this.partitionNames[partitionIDInt] + " Arming: " + this.partitionArmStatuses[partitionIDInt] + " Status: " + this.partitionStatuses[partitionIDInt]);
   }

   public void setPartitionStatus(String partitionID, PanelStatus.partitionStatus actStatus) {
      int partitionIDInt = Integer.parseInt(partitionID);
      if (partitionIDInt < 0 || partitionIDInt >= partitionStatuses.length) {
         logger.warning("Indice partizione fuori dai limiti: " + partitionIDInt);
         return;
      }
      switch(actStatus) {
         case FIRE:
            this.partitionStatuses[partitionIDInt] = "Fire";
            break;
         case FAULTS:
            this.partitionStatuses[partitionIDInt] = "Faults";
            break;
         case ALARMS:
            this.partitionStatuses[partitionIDInt] = "Alarms";
            break;
         case OK:
            this.partitionStatuses[partitionIDInt] = "Ok";
            break;
         default:
            break;
      }

      if (this.partitionNames[partitionIDInt] != null) {
         String str = "";
         if(discoveryEnabled){
            if (this.partitionArmStatuses[partitionIDInt] == null) {
               str = "disarmed";
            } else {
               str = this.partitionArmStatuses[partitionIDInt].toUpperCase();
            }
         } else {
            str = "Name: " + this.partitionNames[partitionIDInt] + " Arming: " + this.partitionArmStatuses[partitionIDInt] + " Status: " + this.partitionStatuses[partitionIDInt];
         }
         safePublish(this.partitionTopics[partitionIDInt], str, QOS, false, "stato partizione");
      }
      logger.fine("Partition Name: " + this.partitionNames[partitionIDInt] + " Arming: " + this.partitionArmStatuses[partitionIDInt] + " Status: " + this.partitionStatuses[partitionIDInt]);
   }

   public void setRemoteName(String var1) {
   }

   public void setStatus(Panel.Status var1) {
   }

   public void tagInputIntoPartition(String var1, List<String> var2) {
   }

   // Smistamento dei comandi ricevuti via MQTT
   public void messageArrived(String topic, MqttMessage msg) {
      String parentTopic = "";
      if(topic.startsWith("ABS/") && topic.endsWith("/set")) {
         parentTopic = topic.replace("/set", "");
         if (ArrayUtils.contains(this.partitionTopics, parentTopic)) {
            int idArray = ArrayUtils.indexOf(this.partitionTopics, parentTopic);
            if (idArray > 0 && idArray <= this.partitionIDs.length) {
               // Se partizione
               this.commandPartition(idArray, msg);
            } else if (idArray == 0) {
               // Se globale
               this.commandGlobal(idArray, msg);
            } else {
               // Errore
               logger.warning("ID " + idArray + " non valido per il topic: " + topic);
            }
         } else if (ArrayUtils.contains(this.sensorTopics, parentTopic)) {
            int idSensor = ArrayUtils.indexOf(this.sensorTopics, parentTopic);
            int idArray = ArrayUtils.indexOf(this.sensorIDs, idSensor);
            if (idArray >= 0 && idArray < this.sensorIDs.length) {
               // Se sensore
               this.commandSensor(idArray, msg);
            } else {
               // Errore
               logger.warning("ID " + idArray + " non valido per il topic: " + topic);
            }
         } else if (parentTopic.contains("mode")) {
            this.commandMode(msg);
         }
      } else if (topic.equals("homeassistant/status")) {
         if(msg.toString().equals("online")){
            commandOnline();
         }
      } else {
         // Comando non riconosciuto
         logger.warning("Comando non riconosciuto per il topic: " + topic);
      }
   }

   private void commandPartition(int idArray, MqttMessage msg) {
      logger.fine("Comando ricevuto per partizione numero: " + idArray + " nuovo stato: " + msg.toString());
      switch (msg.toString().toUpperCase()) {
         case "DISARM":
            this.panel.partitionArming(String.valueOf(this.partitionIDs[idArray]), PanelStatus.partitionArming.DISARMED);
            return;
         case "ARM_HOME":
            this.panel.partitionArming(String.valueOf(this.partitionIDs[idArray]), PanelStatus.partitionArming.STAY);
            return;
         case "ARM_AWAY":
            this.panel.partitionArming(String.valueOf(this.partitionIDs[idArray]), PanelStatus.partitionArming.AWAY);
            return;
         case "ARM_NIGHT":
            this.panel.partitionArming(String.valueOf(this.partitionIDs[idArray]), PanelStatus.partitionArming.NODELAY);
            return;
         default:
            logger.warning("Comando " + msg.toString() + " non valido");
      }
   }

   private void commandGlobal(int idArray, MqttMessage msg) {
         logger.fine("Comando ricevuto per stato globale: " + msg.toString());
         switch (msg.toString().toUpperCase()) {
            case "DISARM":
               this.panel.arming(PanelStatus.globalArming.GLOBALLY_DISARMED);
               return;
            case "ARM_AWAY":
               this.panel.arming(PanelStatus.globalArming.GLOBALLY_ARMED);
               return;
            default:
               logger.warning("Comando " + msg.toString() + " non valido");
         }
   }

   private void commandMode(MqttMessage msg) {
      logger.fine("Comando ricevuto per modalità: " + msg.toString());
      switch (msg.toString().toUpperCase()) {
         case "MODE_A" :
            this.panel.modalityArming('A');
            return;
         case "MODE_B" :
            this.panel.modalityArming('B');
            return;
         case "MODE_C" :
            this.panel.modalityArming('C');
            return;
         case "MODE_D" :
            this.panel.modalityArming('D');
            return;
         default:
            logger.warning("Comando " + msg.toString() + " non valido");
      }
   }

   private void commandSensor(int idArray, MqttMessage msg) {
      if(msg.toString().equals("ON")) {
         this.panel.bypassInput(String.valueOf(this.sensorIDs[idArray]), true);
      } else if(msg.toString().equals("OFF")){
         this.panel.bypassInput(String.valueOf(this.sensorIDs[idArray]), false);
      } else {
         logger.warning("Comando " + msg.toString() + " non valido per il sensore ID: " + idArray);
      }
   }

   public void deliveryComplete(IMqttDeliveryToken var1) {
   }

   private void commandOnline() {
      logger.fine("Comando ricevuto per Home Assistant online");
      // Stato globale (centrale)
      if (this.partitionArmStatuses != null && this.partitionArmStatuses.length > 0) {
         this.sendMessageOnsetArming();
      }
      // Stati partizioni
      if (this.partitionIDs != null) {
         for (int i = 1; i < this.partitionIDs.length; i++) {
            if (this.partitionArmStatuses != null && this.partitionArmStatuses[i] != null) {
               this.sendMessageOnsetPartitionArming(this.partitionIDs[i]);
            }
         }
      }
      // Stati sensori
      if (this.sensorIDs != null) {
         for (int i = 0; i < this.sensorIDs.length; i++) {
            if (this.sensorStatuses != null && this.sensorStatuses[this.sensorIDs[i]] != null) {
               this.sendMessageOnsetInputStatus(this.sensorIDs[i]);
            }
         }
      }
   }

   private void reconnectWithDelay(String objName) {
      this.isConnected = false;
      ++this.reconnectionAttempts;
      logger.warning("Tentativo di riconnessione " + this.reconnectionAttempts + " a " + objName + " in " + RECON_DELAY + " secondi...");
      safePublish("ABS/conn", "Status: Scollegato", QOS, false, "disconnessione forzata");
      try {
         TimeUnit.SECONDS.sleep((long)RECON_DELAY);
         if (objName.equals("centrale")) {
            ConnStatus status = this.panel.connect();
            if (status == ConnStatus.UNREACHABLE) {
               logger.fine("Rilevato 'busy panel'. Tentativo di riconnessione forzata...");
               throw new Exception("Busy panel");
            }
         } else if (objName.equals("broker MQTT")) {
            this.mqttClient.connect(this.connOpts);
         }

         this.reconnectionAttempts = 0;
         this.isConnected = true;
         safePublish("ABS/conn", "Status: Connesso", QOS, false, "riconnessione riuscita");
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         this.handleReconnectionFailure(objName, ex);
      } catch (Exception ex) {
         this.handleReconnectionFailure(objName, ex);
      }
   }

   private void handleReconnectionFailure(String name, Exception ex) {
      logger.warning("Impossibile riconnettersi a " + name + " Causa: " + ex.getMessage());
      ex.printStackTrace();
      this.reconnectWithDelay(name);
   }
}