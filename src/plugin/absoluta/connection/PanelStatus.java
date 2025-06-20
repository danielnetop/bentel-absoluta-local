package plugin.absoluta.connection;

import com.google.common.collect.ImmutableList;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

public class PanelStatus {
   public static final String CONNECTION_STATUS = "CONNECTION_STATUS";
   public static final String GLOBAL_ARMING = "GLOBAL_ARMING";
   public static final String SYSTEM_LABEL = "SYSTEM_LABEL";
   public static final String PARTITIONS = "PARTITIONS";
   public static final String ZONES = "ZONES";
   public static final String OUTPUTS = "OUTPUTS";
   public static final String PARTITION_ARMING = "PARTITION_ARMING";
   public static final String PARTITION_LABEL = "PARTITION_LABEL";
   public static final String PARTITION_STATUS = "PARTITION_STATUS";
   public static final String ZONE_STATUS = "ZONE_STATUS";
   public static final String ZONE_BYPASS = "ZONE_BYPASS";
   public static final String ZONE_LABEL = "ZONE_LABEL";
   public static final String OUTPUT_STATUS = "OUTPUT_STATUS";
   public static final String OUTPUT_LABEL = "OUTPUT_LABEL";
   public static final String ARMING_MODE_LABEL = "ARMING_MODE_LABEL";
   public static final String TROUBLES = "TROUBLES";
   private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
   private panelConnStatus connectionStatus;
   private globalArming globalArming;
   private String systemLabel;
   private ImmutableList<Integer> partitions;
   private ImmutableList<Integer> zones;
   private ImmutableList<Integer> outputs;
   private final Map<Integer, partitionArming> partitionArmings;
   private final Map<Integer, partitionStatus> partitionStatuses;
   private final Map<Integer, String> partitionLabels;
   private final Map<Integer, inputStatus> zoneStatuses;
   private final Map<Integer, Boolean> zoneBypass;
   private final Map<Integer, String> zoneLabels;
   private final Map<Integer, outputStatus> outputStatuses;
   private final Map<Integer, String> outputLabels;
   private final Map<Integer, String> armingModeLabels;

   public PanelStatus() {
      this.connectionStatus = panelConnStatus.DISCONNECTED;
      this.partitionArmings = new HashMap<Integer, partitionArming>();
      this.partitionStatuses = new HashMap<Integer, partitionStatus>();
      this.partitionLabels = new HashMap<Integer, String>();
      this.zoneStatuses = new HashMap<Integer, inputStatus>();
      this.zoneBypass = new HashMap<Integer, Boolean>();
      this.zoneLabels = new HashMap<Integer, String>();
      this.outputStatuses = new HashMap<Integer, outputStatus>();
      this.outputLabels = new HashMap<Integer, String>();
      this.armingModeLabels = new HashMap<Integer, String>();
   }

   public void addPropertyChangeListener(PropertyChangeListener var1) {
      this.changeSupport.addPropertyChangeListener(var1);
   }

   public void removePropertyChangeListener(PropertyChangeListener var1) {
      this.changeSupport.removePropertyChangeListener(var1);
   }

   void setConnectionStatus(panelConnStatus newConnectionStatus) {
      panelConnStatus oldConnectionStatus;
      synchronized(this) {
         oldConnectionStatus = this.connectionStatus;
         this.connectionStatus = newConnectionStatus;
      }
      this.changeSupport.firePropertyChange("CONNECTION_STATUS", oldConnectionStatus, newConnectionStatus);
   }

   public synchronized panelConnStatus getConnectionStatus() {
      return this.connectionStatus;
   }

   void setGlobalArming(globalArming newMode) {
      globalArming oldMode = this.globalArming;
      this.globalArming = newMode;
      this.changeSupport.firePropertyChange("GLOBAL_ARMING", oldMode, newMode);
   }

   public globalArming getGlobalArming() {
      return this.globalArming;
   }

   void setSystemLabel(String newSystemLabel) {
      String oldSystemLabel = this.systemLabel;
      this.systemLabel = newSystemLabel;
      this.changeSupport.firePropertyChange("SYSTEM_LABEL", oldSystemLabel, newSystemLabel);
   }

   public String getSystemLabel() {
      return this.systemLabel;
   }

   void setPartitions(ImmutableList<Integer> var1) {
      ImmutableList<Integer> var2 = this.partitions;
      this.partitions = var1;
      this.changeSupport.firePropertyChange("PARTITIONS", var2, var1);
   }

   public ImmutableList<Integer> getPartitions() {
      return this.partitions;
   }

   void setZones(ImmutableList<Integer> var1) {
      ImmutableList<Integer> var2 = this.zones;
      this.zones = var1;
      this.changeSupport.firePropertyChange("ZONES", var2, var1);
   }

   public ImmutableList<Integer> getZones() {
      return this.zones;
   }

   void setOutputs(ImmutableList<Integer> var1) {
      ImmutableList<Integer> var2 = this.outputs;
      this.outputs = var1;
      this.changeSupport.firePropertyChange("OUTPUTS", var2, var1);
   }

   public ImmutableList<Integer> getOutputs() {
      return this.outputs;
   }

   void setPartitionArming(int partitionID, partitionArming newMode) {
      if (this.partitions.contains(partitionID)) {
         partitionArming oldMode = this.partitionArmings.get(partitionID);
         this.partitionArmings.put(partitionID, newMode);
         this.changeSupport.fireIndexedPropertyChange("PARTITION_ARMING", partitionID, oldMode, newMode);
      }

   }

   public partitionArming getPartitionArming(int mode) {
      return this.partitionArmings.get(mode);
   }

   void setPartitionStatus(int partitionID, partitionStatus newStatus) {
      if (this.partitions.contains(partitionID)) {
         partitionStatus oldStatus = this.partitionStatuses.get(partitionID);
         this.partitionStatuses.put(partitionID, newStatus);
         this.changeSupport.fireIndexedPropertyChange("PARTITION_STATUS", partitionID, oldStatus, newStatus);
      }

   }

   public partitionStatus getPartitionStatus(int partitionID) {
      return this.partitionStatuses.get(partitionID);
   }

   void setPartitionLabel(int partitionID, String newLabel) {
      if (this.partitions.contains(partitionID)) {
         String oldLabel = (String)this.partitionLabels.get(partitionID);
         this.partitionLabels.put(partitionID, newLabel);
         this.changeSupport.fireIndexedPropertyChange("PARTITION_LABEL", partitionID, oldLabel, newLabel);
      }

   }

   public String getPartitionLabel(int partitionID) {
      return this.partitionLabels.get(partitionID);
   }

   void setZoneStatus(int zoneID, inputStatus newStatus) {
      if (this.zones.contains(zoneID)) {
         inputStatus oldStatus = (inputStatus)this.zoneStatuses.get(zoneID);
         this.zoneStatuses.put(zoneID, newStatus);
         this.changeSupport.fireIndexedPropertyChange("ZONE_STATUS", zoneID, oldStatus, newStatus);
      }

   }

   public inputStatus getZoneStatus(int zoneID) {
      return this.zoneStatuses.get(zoneID);
   }

   void setZoneBypass(int zoneID, Boolean newSet) {
      if (this.zones.contains(zoneID)) {
         Boolean oldSet = (Boolean)this.zoneBypass.get(zoneID);
         this.zoneBypass.put(zoneID, newSet);
         this.changeSupport.fireIndexedPropertyChange("ZONE_BYPASS", zoneID, oldSet, newSet);
      }

   }

   public Boolean getZoneBypass(int zoneID) {
      return this.zoneBypass.get(zoneID);
   }

   void setZoneLabel(int zoneID, String newLabel) {
      if (this.zones.contains(zoneID)) {
         String oldLabel = (String)this.zoneLabels.get(zoneID);
         this.zoneLabels.put(zoneID, newLabel);
         this.changeSupport.fireIndexedPropertyChange("ZONE_LABEL", zoneID, oldLabel, newLabel);
      }

   }

   public String getZoneLabel(int zoneID) {
      return this.zoneLabels.get(zoneID);
   }

   void setOutputStatus(int outputID, outputStatus newStatus) {
      if (this.outputs.contains(outputID)) {
         outputStatus oldStatus = (outputStatus)this.outputStatuses.get(outputID);
         this.outputStatuses.put(outputID, newStatus);
         this.changeSupport.fireIndexedPropertyChange("OUTPUT_STATUS", outputID, oldStatus, newStatus);
      }

   }

   public outputStatus getOutputStatus(int outputID) {
      return this.outputStatuses.get(outputID);
   }

   void setOutputLabel(int outputID, String newLabel) {
      if (this.outputs.contains(outputID)) {
         String oldLabel = (String)this.outputLabels.get(outputID);
         this.outputLabels.put(outputID, newLabel);
         this.changeSupport.fireIndexedPropertyChange("OUTPUT_LABEL", outputID, oldLabel, newLabel);
      }

   }

   public String getOutputLabel(int outputID) {
      return this.outputLabels.get(outputID);
   }

   void setArmingModeLabel(int modeID, String newLabel) {
      String oldLabel = (String)this.armingModeLabels.get(modeID);
      this.armingModeLabels.put(modeID, newLabel);
      this.changeSupport.fireIndexedPropertyChange("ARMING_MODE_LABEL", modeID, oldLabel, newLabel);
   }

   public String getArmingModeLabel(int modeID) {
      return this.armingModeLabels.get(modeID);
   }

   public static enum panelConnStatus {
      CONNECTED,
      DISCONNECTING,
      DISCONNECTED;
   }

   public static enum inputStatus {
      TAMPER,
      FAULT,
      ALARM,
      ACTIVE,
      BYPASSED,
      OK;
   }

   public static enum outputStatus {
      UNKNOWN,
      OPEN,
      CLOSED;
   }

   public enum partitionArming {
      DISARMED,
      AWAY,
      STAY,
      NODELAY,
      TRIGGERED,
      NOT_AVAILABLE
   }

   public enum partitionStatus {
      FIRE,
      TAMPER,
      FAULTS,
      ALARMS,
      ACTIVE,
      OK
   }

   public static enum outputAction {
      DO_IMPULSE,
      DO_OPEN,
      DO_CLOSE;
   }

   public enum globalArming {
      GLOBALLY_ARMED,
      PARTIALLY_ARMED,
      GLOBALLY_DISARMED,
      NOT_AVAILABLE,
      TRIGGERED
   }
}