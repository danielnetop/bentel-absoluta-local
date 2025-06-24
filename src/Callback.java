import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import absoluta.AbsolutaPanelProvider;
import absoluta.AbsolutaPanelProvider.providerConnStatus;
import absoluta.AbsolutaPanelProvider.providerStatus;
import absoluta.connection.PanelStatus;

import java.util.logging.Logger;

class Callback implements AbsolutaPanelProvider.PanelCallback, MqttCallback {
   private static final Logger logger = Logger.getLogger(Callback.class.getName());
   private MqttClient mqttClient;
   private int[] zoneIds;
   private int[] partitionIDs;
   private String[] zoneNames;
   private String[] partitionNames;
   private String[] sensorStatuses;
   private String[] sensorBypass;
   private String[] partitionArmStatuses;
   private String[] partitionStatuses;
   private String[] sensorTopics;
   private String[] partitionTopics;
   private String[] modeNames = new String[4];
   private AbsolutaPanelProvider provider;
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

   private String errorMessages = "[]"; // JSON array of error notifications
   private boolean hasError = false;

   public Callback(MqttClient mqttClient, AbsolutaPanelProvider provider, MqttConnectOptions mqttOption, Boolean discoveryEnabled) {
      this.mqttClient = mqttClient;
      this.provider = provider;
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
      notifyError("Connessione persa con la centrale! Riconnessione...");
      this.reconnectWithDelay("centrale");
   }

   public void alert(String var1) {
      //TODO: @Stefano: è questa la callback per gli errori? Leggendo AlertListener mi sembra di sì.
      String topic = "ABS/errors";
      //TODO: sarebbe meglio diversificare per tipo di errore.
      notifyError(var1);
   }

   public void setStatus(providerStatus status){}

   public void notifyError(String errorMessage) {
      // Build new error notification as a JSON object with required field names
      String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
      String newError = String.format("{\"Time\":\"%s\",\"Message\":\"%s\"}", timestamp, errorMessage);

      // Add to errorMessages JSON array
      if (errorMessages == null || errorMessages.isEmpty()) {
         errorMessages = "[" + newError + "]";
      } else {
         String trimmed = errorMessages.trim();
         if (trimmed.equals("[]")) {
            errorMessages = "[" + newError + "]";
         } else if (trimmed.endsWith("]")) {
            errorMessages = trimmed.substring(0, trimmed.length() - 1) + "," + newError + "]";
         } else {
            errorMessages = "[" + newError + "]";
         }
      }

      // Publish as an object with 'errors' key for Home Assistant attributes
      String attributesPayload = "{\"errors\":" + errorMessages + "}";
      safePublish("ABS/errors/attributes", attributesPayload, QOS, true, "errore centrale");
      hasError = true;
      safePublish("ABS/errors", "Errore!", QOS, true, "errore centrale");
   }


   public void getAllZones(List<Integer> zones) {
      this.zoneIds = new int[zones.size()];
      int maxId = 0;
      for (int i = 0; i < zones.size(); i++) {
         int id = zones.get(i);
         this.zoneIds[i] = id;
         if (id > maxId) maxId = id;
      }
      this.zoneNames = new String[maxId + 1];
      this.sensorTopics = new String[maxId + 1];
      this.sensorStatuses = new String[maxId + 1];
      this.sensorBypass = new String[maxId + 1];
      logger.fine("Sensore ID: " + zones);
   }

   public void getAllOutputs(List<Integer> outputs) {
   }

   public void getAllPartitions(List<Integer> partitions) {
      /*
      Riceve come informazione quante partizioni ha il sistema
      e crea gli array per i nomi, i topic e gli stati delle partizioni.
      Sposta tutte le partizioni lasciando la zero dedicata alla partizione globale.
      */
      this.partitionIDs = new int[partitions.size() + 1];
      this.partitionIDs[0] = 0;
      for (int i = 0; i < partitions.size(); i++) {
         this.partitionIDs[i + 1] = partitions.get(i);
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
         String payload = HomeAssistantManager.buildPartition(0, partitionNames[0]);
         safePublish(topic, payload, QOS, true, "discovery partizione globale");
         partitionDiscoverySent.add(0);

         // Invia discovery per pulsante reset errori
         topic = "homeassistant/button/absoluta_errors_reset/config";
         payload = HomeAssistantManager.buildResetErrors();
         safePublish(topic, payload, QOS, true, "discovery reset errors");

         // Invia discovery per i messaggi di errore 
         topic = "homeassistant/sensor/absoluta_errors/config";
         payload = HomeAssistantManager.buildErrorSensor();
         safePublish(topic, payload, QOS, true, "discovery errori");

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
      logger.fine("Partizione ID: " + String.valueOf(partitions));
   }

   public void updateGlobalArming(PanelStatus.GlobalArming actArming) {
      if (actArming == PanelStatus.GlobalArming.GLOBALLY_DISARMED) {
         this.partitionArmStatuses[0] = "disarmed";
      } else if (actArming == PanelStatus.GlobalArming.GLOBALLY_ARMED) {
         this.partitionArmStatuses[0] = "armed_away";
      } else if (actArming == PanelStatus.GlobalArming.PARTIALLY_ARMED) {
         this.partitionArmStatuses[0] = "armed_custom_bypass";
      }
      this.sendMessageOnUpdateGlobalArming();
   }

   public void sendMessageOnUpdateGlobalArming() {
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

   public void updateZoneName(int zoneId, String zoneName) {
      if (zoneId < 0 || zoneId >= zoneNames.length) {
         logger.warning("Indice sensore fuori dai limiti: " + zoneId);
         return;
      }
      this.zoneNames[zoneId] = zoneName;
      this.sensorTopics[zoneId] = "ABS/sensor/" + zoneId;
      // Invia discovery solo la prima volta per ogni sensore
      if (discoveryEnabled && !sensorDiscoverySent.contains(zoneId)) {
         String topic = "homeassistant/binary_sensor/absoluta_sensor_" + zoneId + "/config";
         String payload = HomeAssistantManager.buildSensor(zoneId, zoneNames[zoneId]);
         safePublish(topic, payload, QOS, true, "discovery sensore");
         sensorDiscoverySent.add(zoneId);

         topic = "homeassistant/switch/absoluta_sensor_" + zoneId + "_bypass/config";
         payload = HomeAssistantManager.buildSensorBypass(zoneId, zoneNames[zoneId]);
         safePublish(topic, payload, QOS, true, "discovery sensore Bypass");
      }
      String str = "";
      if(discoveryEnabled){
         str = (this.sensorStatuses[zoneId] == null) ? "OFF" : this.sensorStatuses[zoneId].toUpperCase();
      } else {
         str = "Name: " + this.zoneNames[zoneId] + " Status: " + this.sensorStatuses[zoneId] + " Bypass: " + this.sensorBypass[zoneId];
      }
      safePublish(this.sensorTopics[zoneId], str, QOS, false, "stato sensore");
      logger.fine("Sensor Name: " + this.zoneNames[zoneId] + " Status: " + this.sensorStatuses[zoneId] + " Bypass: " + this.sensorBypass[zoneId]);
   }

   public void updateZoneStatus(int zoneId, PanelStatus.InputStatus zoneStatus) {
      if (zoneId < 0 || zoneId >= sensorStatuses.length) {
         logger.warning("Indice sensore fuori dai limiti: " + zoneId);
         return;
      }
      if (zoneStatus != PanelStatus.InputStatus.ACTIVE && zoneStatus != PanelStatus.InputStatus.ALARM) {
         this.sensorStatuses[zoneId] = "Off";
      } else {
         this.sensorStatuses[zoneId] = "On";
      }

      this.sendMessageOnUpdateZoneStatus(zoneId);
   }

   public void updateZoneBypass(int zoneId, boolean bypass) {
      if (zoneId < 0 || zoneId >= sensorStatuses.length) {
         logger.warning("Indice sensore fuori dai limiti: " + zoneId);
         return;
      }

      if (bypass) {
         this.sensorBypass[zoneId] = "ON";
      } else {
         this.sensorBypass[zoneId] = "OFF";
      }

      this.sendMessageOnUpdateZoneStatus(zoneId);
   }

   public void sendMessageOnUpdateZoneStatus(int zoneId) {
      if (zoneId < 0 || zoneId >= zoneNames.length) {
         logger.warning("Indice sensore fuori dai limiti: " + zoneId);
         return;
      }
      if (this.zoneNames[zoneId] == null) {
         logger.finest("Nome sensore nullo: " + zoneId + ", non invio stato.");
         return;
      }
      String str = "";
      if(discoveryEnabled){
         String topic = "ABS/sensor/" + zoneId + "_bypass";
         str = (this.sensorBypass[zoneId] == null) ? "OFF" : this.sensorBypass[zoneId].toUpperCase();
         safePublish(topic, str, QOS, false, "bypass sensore");
         str = (this.sensorStatuses[zoneId] == null) ? "OFF" : this.sensorStatuses[zoneId].toUpperCase();
      } else {
         str = "Name: " + this.zoneNames[zoneId] + " Status: " + this.sensorStatuses[zoneId] + " Bypass: " + this.sensorBypass[zoneId];
      }
      safePublish(this.sensorTopics[zoneId], str, QOS, false, "stato sensore");
      logger.fine("Sensor Name: " + this.zoneNames[zoneId] + " Status: " + this.sensorStatuses[zoneId] + " Bypass: " + this.sensorBypass[zoneId]);
   }

   public void updateModeLabel(char modeChar, String modeLabel) {
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

   public void updateOutputName(int outputID, String name) {
   }

   public void updateOutputStatus(int outputID, PanelStatus.OutputStatus status) {
   }

   public void updatePartitionArming(int partitionID, PanelStatus.PartitionArming actArming) {
      if (partitionID < 0 || partitionID >= partitionArmStatuses.length) {
         logger.warning("Indice partizione fuori dai limiti: " + partitionID);
         return;
      }
      if (actArming == PanelStatus.PartitionArming.DISARMED) {
         this.partitionArmStatuses[partitionID] = "disarmed";
      } else if (actArming == PanelStatus.PartitionArming.AWAY) {
         this.partitionArmStatuses[partitionID] = "armed_away";
      } else if (actArming == PanelStatus.PartitionArming.STAY) {
         this.partitionArmStatuses[partitionID] = "armed_home";
      } else if (actArming == PanelStatus.PartitionArming.NODELAY) {
         this.partitionArmStatuses[partitionID] = "armed_night";
      } else if (actArming == PanelStatus.PartitionArming.TRIGGERED) {
         this.partitionArmStatuses[partitionID] = "triggered";
      }

      this.sendMessageOnUpdatePartitionArming(partitionID);
   }

   public void sendMessageOnUpdatePartitionArming(int partitionID) {
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
      safePublish(this.partitionTopics[partitionID], str, QOS, false, "stato partizione");
      logger.fine("Partition Name: " + this.partitionNames[partitionID] + " Arming: " + this.partitionArmStatuses[partitionID] + " Status: " + this.partitionStatuses[partitionID]);
   }

   public void updatePartitionName(int partitionID, String name) {
      // Controlla se l'array è sufficientemente grande per contenere la partizione
      if (partitionID >= partitionNames.length) {
         int newSize = partitionID + 1;
         partitionNames = java.util.Arrays.copyOf(partitionNames, newSize);
         partitionTopics = java.util.Arrays.copyOf(partitionTopics, newSize);
         partitionArmStatuses = java.util.Arrays.copyOf(partitionArmStatuses, newSize);
         partitionStatuses = java.util.Arrays.copyOf(partitionStatuses, newSize);
      }
      this.partitionNames[partitionID] = name;
      this.partitionTopics[partitionID] = "ABS/partition/" + partitionID;
      // Invia discovery solo la prima volta per ogni partizione
      if (discoveryEnabled && !partitionDiscoverySent.contains(partitionID)  && partitionID > 0) {
         String topic = "homeassistant/alarm_control_panel/absoluta_partition_" + partitionID + "/config";
         String payload = HomeAssistantManager.buildPartition(partitionID, name);
         safePublish(topic, payload, QOS, true, "discovery partizione");
         partitionDiscoverySent.add(partitionID);
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
      safePublish(this.partitionTopics[partitionID], str, QOS, false, "stato partizione");
      logger.fine("Partition Name: " + this.partitionNames[partitionID] + " Arming: " + this.partitionArmStatuses[partitionID] + " Status: " + this.partitionStatuses[partitionID]);
   }

   public void updatePartitionStatus(int partitionID, PanelStatus.PartitionStatus actStatus) {
      if (partitionID < 0 || partitionID >= partitionStatuses.length) {
         logger.warning("Indice partizione fuori dai limiti: " + partitionID);
         return;
      }
      switch(actStatus) {
         case FIRE:
            this.partitionStatuses[partitionID] = "Fire";
            break;
         case FAULTS:
            this.partitionStatuses[partitionID] = "Faults";
            break;
         case ALARMS:
            this.partitionStatuses[partitionID] = "Alarms";
            break;
         case OK:
            this.partitionStatuses[partitionID] = "Ok";
            break;
         default:
            break;
      }

      if (this.partitionNames[partitionID] != null) {
         String str = "";
         if(discoveryEnabled){
            if (this.partitionArmStatuses[partitionID] == null) {
               str = "disarmed";
            } else {
               str = this.partitionArmStatuses[partitionID].toUpperCase();
            }
         } else {
            str = "Name: " + this.partitionNames[partitionID] + " Arming: " + this.partitionArmStatuses[partitionID] + " Status: " + this.partitionStatuses[partitionID];
         }
         safePublish(this.partitionTopics[partitionID], str, QOS, false, "stato partizione");
      }
      logger.fine("Partition Name: " + this.partitionNames[partitionID] + " Arming: " + this.partitionArmStatuses[partitionID] + " Status: " + this.partitionStatuses[partitionID]);
   }

   public void tagZoneIntoPartition(int partitionId, List<Integer> zones) {
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
            int idArray = ArrayUtils.indexOf(this.zoneIds, idSensor);
            if (idArray >= 0 && idArray < this.zoneIds.length) {
               // Se sensore
               this.commandSensor(idArray, msg);
            } else {
               // Errore
               logger.warning("ID " + idArray + " non valido per il topic: " + topic);
            }
         } else if (parentTopic.contains("mode")) {
            this.commandMode(msg);
         } else if (parentTopic.contains("absoluta_errors")) {
            if (msg.toString().equals("RESET_ERRORS")) {
               logger.info("Resetting errors...");
               this.provider.cleanTroubles();
               this.errorMessages = "[]";
               this.hasError = false;
               safePublish("ABS/errors/attributes", "{\"errors\":[]}", QOS, true, "reset errori");
               safePublish("ABS/errors", "Funzionamento Regolare", QOS, true, "reset errori");
            } else {
               logger.warning("Comando " + msg.toString() + " non valido per il topic: " + topic);
            }
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
            safePublish(this.partitionTopics[idArray], "disarming", QOS, false, "transitorio partizione" + idArray); 
            this.provider.setPartitionArming(this.partitionIDs[idArray], PanelStatus.PartitionArming.DISARMED);
            return;
         case "ARM_HOME":
            safePublish(this.partitionTopics[idArray], "arming", QOS, false, "transitorio partizione" + idArray);
            safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale per partizione " + idArray); 
            this.provider.setPartitionArming(this.partitionIDs[idArray], PanelStatus.PartitionArming.STAY);
            return;
         case "ARM_AWAY":
            safePublish(this.partitionTopics[idArray], "arming", QOS, false, "transitorio partizione" + idArray);
            safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale per partizione " + idArray); 
            this.provider.setPartitionArming(this.partitionIDs[idArray], PanelStatus.PartitionArming.AWAY);
            return;
         case "ARM_NIGHT":
            safePublish(this.partitionTopics[idArray], "arming", QOS, false, "transitorio partizione" + idArray);
            safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale per partizione " + idArray); 
            this.provider.setPartitionArming(this.partitionIDs[idArray], PanelStatus.PartitionArming.NODELAY);
            return;
         default:
            logger.warning("Comando " + msg.toString() + " non valido");
      }
   }

   private void commandGlobal(int idArray, MqttMessage msg) {
         logger.fine("Comando ricevuto per stato globale: " + msg.toString());
         switch (msg.toString().toUpperCase()) {
            case "DISARM":
               safePublish(this.partitionTopics[0], "disarming", QOS, false, "transitorio globale"); 
               this.provider.setGlobalArming(PanelStatus.GlobalArming.GLOBALLY_DISARMED);
               return;
            case "ARM_AWAY":
               safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale");
               this.provider.setGlobalArming(PanelStatus.GlobalArming.GLOBALLY_ARMED);
               return;
            default:
               logger.warning("Comando " + msg.toString() + " non valido");
         }
   }

   private void commandMode(MqttMessage msg) {
      logger.fine("Comando ricevuto per modalità: " + msg.toString());
      switch (msg.toString().toUpperCase()) {
         case "MODE_A" :
            safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale per modalità A");
            this.provider.setModeArming('A');
            return;
         case "MODE_B" :
            safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale per modalità B");
            this.provider.setModeArming('B');
            return;
         case "MODE_C" :
            safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale per modalità C");
            this.provider.setModeArming('C');
            return;
         case "MODE_D" :
            safePublish(this.partitionTopics[0], "arming", QOS, false, "transitorio globale per modalità D");
            this.provider.setModeArming('D');
            return;
         default:
            logger.warning("Comando " + msg.toString() + " non valido");
      }
   }

   private void commandSensor(int idArray, MqttMessage msg) {
      if(msg.toString().equals("ON")) {
         this.provider.setZoneBypass(this.zoneIds[idArray], true);
      } else if(msg.toString().equals("OFF")){
         this.provider.setZoneBypass(this.zoneIds[idArray], false);
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
         this.sendMessageOnUpdateGlobalArming();
      }
      // Stati partizioni
      if (this.partitionIDs != null) {
         for (int i = 1; i < this.partitionIDs.length; i++) {
            if (this.partitionArmStatuses != null && this.partitionArmStatuses[i] != null) {
               this.sendMessageOnUpdatePartitionArming(this.partitionIDs[i]);
            }
         }
      }
      // Stati sensori
      if (this.zoneIds != null) {
         for (int i = 0; i < this.zoneIds.length; i++) {
            if (this.sensorStatuses != null && this.sensorStatuses[this.zoneIds[i]] != null) {
               this.sendMessageOnUpdateZoneStatus(this.zoneIds[i]);
            }
         }
      }
   }

   private void reconnectWithDelay(String objName) {
      try {
         if (objName.equals("centrale")) {
            this.isConnected = false;
            ++this.reconnectionAttempts;
            logger.warning("Tentativo di riconnessione " + this.reconnectionAttempts + " a " + objName + " in " + RECON_DELAY + " secondi...");
            safePublish("ABS/conn", "Status: Scollegato", QOS, false, "disconnessione forzata");
            TimeUnit.SECONDS.sleep((long)RECON_DELAY);
            providerConnStatus status = this.provider.connect();
            if (status == providerConnStatus.UNREACHABLE) {
               throw new Exception("Busy panel");
            } else if (status == providerConnStatus.SUCCESS) {
               this.reconnectionAttempts = 0;
               this.isConnected = true;
               safePublish("ABS/conn", "Status: Connesso", QOS, false, "riconnessione riuscita");
            }
         } else if (objName.equals("broker MQTT")) {
            this.mqttClient.connect(this.connOpts);
         }
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