package absoluta.connection;

import com.google.common.collect.ImmutableList;

import protocol.dsc.DscError;
import protocol.dsc.Message;
import protocol.dsc.MessageListener;
import protocol.dsc.NewValue;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.javatuples.Pair;

class StatusListener implements MessageListener {
   private static final Logger logger = Logger.getLogger(StatusListener.class.getName());

   // Partition status indices
   private static final int PARTITION_ARMED = 0;
   private static final int PARTITION_STAY = 1;
   private static final int PARTITION_AWAY = 2;
   private static final int PARTITION_NIGHT = 3;
   private static final int PARTITION_NODELAY = 4;
   private static final int PARTITION_ALARM = 8;
   private static final int PARTITION_TROUBLES = 9;
   private static final int PARTITION_ALARM_IN_MEMORY = 12;
   private static final int PARTITION_FIRE = 17;

   // Zone status indices
   private static final int ZONE_OPEN = 0;
   private static final int ZONE_TAMPER = 1;
   private static final int ZONE_FAULT = 2;
   private static final int ZONE_LOW_BATTERY = 3;
   private static final int ZONE_DELINQUENCY = 4;
   private static final int ZONE_ALARM = 5;
   private static final int ZONE_ALARM_IN_MEMORY = 6;
   private static final int ZONE_BYPASSED = 7;

   private final PanelStatus panelStatus;
   private int partitionStatusUpdateSkipCount = 0;

   StatusListener(PanelStatus panelStatus) {
      this.panelStatus = Objects.requireNonNull(panelStatus);
   }

   @Override
   @SuppressWarnings("unchecked")
   public void newValue(NewValue msg) {
      if (msg.isFor(Message.PARTITION_ASSIGNMENT_CONFIGURATION)) {
         List<Integer> partitions = (List<Integer>) msg.getValue(Message.PARTITION_ASSIGNMENT_CONFIGURATION);
         panelStatus.setPartitions(ImmutableList.copyOf(partitions));
      } else if (msg.isFor(Message.PARTITION_ZONES)) {
         Integer partitionNumber = (Integer) msg.getParam(Message.PARTITION_ZONES);
         if (partitionNumber == null) {
               List<Integer> zones = (List<Integer>) msg.getValue(Message.PARTITION_ZONES);
               panelStatus.setZones(ImmutableList.copyOf(zones));
         } else {
            logger.warning("Unexpected partition number for partition zones: " + partitionNumber);
         }
      } else if (msg.isFor(Message.ABSOLUTA_ENABLED_OUTPUTS_AND_REMOTE_COMMANDS)) {
         List<Integer> outputs = (List<Integer>) ((Pair<?, ?>) msg.getValue(Message.ABSOLUTA_ENABLED_OUTPUTS_AND_REMOTE_COMMANDS)).getValue0();
         panelStatus.setOutputs(ImmutableList.copyOf(outputs));
      } else if (msg.isFor(Message.PARTITION_STATUSES)) {
         List<Integer> partitionIds = (List<Integer>) msg.getParam(Message.PARTITION_STATUSES);
         List<List<Boolean>> partitionStatuses = (List<List<Boolean>>) msg.getValue(Message.PARTITION_STATUSES);

            if (partitionIds.size() != partitionStatuses.size()) {
               logger.warning("Partition IDs and statuses size mismatch");
               return;
            }

            for (int i = 0; i < partitionIds.size(); ++i) {
               int partitionId = partitionIds.get(i);
               List<Boolean> statusMask = partitionStatuses.get(i);
               updatePartitionStatus(partitionId, statusMask);
            }
      } else if (msg.isFor(Message.ZONE_STATUSES)) {
         int zoneId = (Integer) ((Pair<?, ?>) msg.getParam(Message.ZONE_STATUSES)).getValue0();
         List<List<Boolean>> zoneStatuses = (List<List<Boolean>>) msg.getValue(Message.ZONE_STATUSES);

         for (List<Boolean> statusMask : zoneStatuses) {
               updateZoneStatus(zoneId, statusMask);
               zoneId++;
         }
      } else if (msg.isFor(Message.ABSOLUTA_COMMAND_OUTPUT_ACTIVATION)) {
         List<Integer> activeOutputs = (List<Integer>) msg.getValue(Message.ABSOLUTA_COMMAND_OUTPUT_ACTIVATION);
         for (Integer outputId : panelStatus.getOutputs()) {
               PanelStatus.OutputStatus outputStatus = activeOutputs.contains(outputId) ? PanelStatus.OutputStatus.CLOSED : PanelStatus.OutputStatus.OPEN;
               panelStatus.setOutputStatus(outputId, outputStatus);
         }
      } else if (msg.isFor(Message.ABSOLUTA_SYSTEM_LABEL)) {
         String systemLabel = ((String) msg.getValue(Message.ABSOLUTA_SYSTEM_LABEL)).trim();
         panelStatus.setSystemLabel(systemLabel);
      } else if (msg.isFor(Message.ABSOLUTA_PARTITION_LABEL)) {
         int partitionId = (Integer) msg.getParam(Message.ABSOLUTA_PARTITION_LABEL);
         String label = ((String) msg.getValue(Message.ABSOLUTA_PARTITION_LABEL)).trim();
         panelStatus.setPartitionLabel(partitionId, label);
      } else if (msg.isFor(Message.ABSOLUTA_ZONE_LABEL)) {
         int zoneId = (Integer) msg.getParam(Message.ABSOLUTA_ZONE_LABEL);
         String label = ((String) msg.getValue(Message.ABSOLUTA_ZONE_LABEL)).trim();
         panelStatus.setZoneLabel(zoneId, label);
      } else if (msg.isFor(Message.ABSOLUTA_OUTPUT_LABEL)) {
         int outputId = (Integer) msg.getParam(Message.ABSOLUTA_OUTPUT_LABEL);
         String label = ((String) msg.getValue(Message.ABSOLUTA_OUTPUT_LABEL)).trim();
         panelStatus.setOutputLabel(outputId, label);
      } else if (msg.isFor(Message.ABSOLUTA_ARMING_MODE_LABEL)) {
         int armingModeId = (Integer) msg.getParam(Message.ABSOLUTA_ARMING_MODE_LABEL);
         String label = ((String) msg.getValue(Message.ABSOLUTA_ARMING_MODE_LABEL)).trim();
         panelStatus.setArmingModeLabel(armingModeId, label);
      }
   }

   @Override
   public void error(DscError error) {
      // Optionally log error
   }

   private void updatePartitionStatus(int partitionId, List<Boolean> statusMask) {
      if (partitionStatusUpdateSkipCount < 20) {
         partitionStatusUpdateSkipCount++;
         return;
      }

      PanelStatus.PartitionArming armingMode;
      if (!statusMask.get(PARTITION_ARMED)) {
         armingMode = PanelStatus.PartitionArming.DISARMED;
      } else if (statusMask.get(PARTITION_AWAY)) {
         armingMode = PanelStatus.PartitionArming.AWAY;
      } else if (statusMask.get(PARTITION_STAY)) {
         armingMode = PanelStatus.PartitionArming.STAY;
      } else if (!statusMask.get(PARTITION_NODELAY) && !statusMask.get(PARTITION_NIGHT)) {
         logger.warning("Arming status ambiguous for partition " + partitionId);
         armingMode = PanelStatus.PartitionArming.AWAY;
      } else {
         armingMode = PanelStatus.PartitionArming.NODELAY;
      }

      // Partition status detection
      PanelStatus.PartitionStatus partitionStatus;
      if (statusMask.get(PARTITION_FIRE)) {
         partitionStatus = PanelStatus.PartitionStatus.FIRE;
      } else if (statusMask.get(PARTITION_TROUBLES)) {
         partitionStatus = PanelStatus.PartitionStatus.FAULTS;
      } else if (!statusMask.get(PARTITION_ALARM) && !statusMask.get(PARTITION_ALARM_IN_MEMORY)) {
         partitionStatus = PanelStatus.PartitionStatus.OK;
      } else {
         partitionStatus = PanelStatus.PartitionStatus.ALARMS;
      }

      panelStatus.setPartitionStatus(partitionId, partitionStatus);

      // If partition is in alarm, set TRIGGERED
      if (partitionStatus == PanelStatus.PartitionStatus.ALARMS) {
         armingMode = PanelStatus.PartitionArming.TRIGGERED;
      }

      panelStatus.setPartitionArming(partitionId, armingMode);

      boolean anyPartitionArmed = false;
      boolean anyPartitionDisarmed = false;
      boolean missingData = false;
      boolean anyPartitionTriggered = false;

      for (Integer id : panelStatus.getPartitions()) {
         PanelStatus.PartitionArming mode = panelStatus.getPartitionArming(id);
         if (mode == null) {
            missingData = true;
         } else if (mode == PanelStatus.PartitionArming.TRIGGERED){
            anyPartitionTriggered = true;
         }else if (mode == PanelStatus.PartitionArming.DISARMED) {
            anyPartitionDisarmed = true;
         } else {
            anyPartitionArmed = true;
         }
      }

      PanelStatus.GlobalArming globalArming;
      if (missingData) {
         globalArming = null;
      } else if (anyPartitionTriggered) {
         globalArming = PanelStatus.GlobalArming.TRIGGERED;
      } else if (anyPartitionArmed && anyPartitionDisarmed) {
         globalArming = PanelStatus.GlobalArming.PARTIALLY_ARMED;
      } else if (anyPartitionArmed) {
         globalArming = PanelStatus.GlobalArming.GLOBALLY_ARMED;
      } else {
         globalArming = PanelStatus.GlobalArming.GLOBALLY_DISARMED;
      }

      panelStatus.setGlobalArming(globalArming);
   }

   private void updateZoneStatus(int zoneId, List<Boolean> statusMask) {
      PanelStatus.InputStatus zoneStatus;
      if (statusMask.get(ZONE_BYPASSED)) {
         zoneStatus = statusMask.get(ZONE_OPEN) ? PanelStatus.InputStatus.ACTIVE : PanelStatus.InputStatus.OK;
      } else if (!statusMask.get(ZONE_TAMPER) && !statusMask.get(ZONE_DELINQUENCY)) {
         if (!statusMask.get(ZONE_FAULT) && !statusMask.get(ZONE_LOW_BATTERY)) {
               if (!statusMask.get(ZONE_ALARM) && !statusMask.get(ZONE_ALARM_IN_MEMORY)) {
                  zoneStatus = statusMask.get(ZONE_OPEN) ? PanelStatus.InputStatus.ACTIVE : PanelStatus.InputStatus.OK;
               } else {
                  zoneStatus = PanelStatus.InputStatus.ALARM;
               }
         } else {
               zoneStatus = PanelStatus.InputStatus.FAULT;
         }
      } else {
         zoneStatus = PanelStatus.InputStatus.TAMPER;
      }

      panelStatus.setZoneBypass(zoneId, statusMask.get(ZONE_BYPASSED));
      panelStatus.setZoneStatus(zoneId, zoneStatus);
   }
}