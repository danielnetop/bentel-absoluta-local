package absoluta.connection;

import protocol.dsc.Message;

import java.util.Objects;
import java.util.logging.Logger;

import org.javatuples.Pair;
import org.javatuples.Triplet;

public class Commander {
   private static final Logger logger = Logger.getLogger(Commander.class.getName());
   private static final Integer SYSTEM = null;
   private static final int STAY_ARM = 1;
   private static final int AWAY_ARM = 2;
   private static final int INSTANT_STAY_ARM = 7;
   private static final int CLEAR_FAULTS = 10;
   static final int ACTIVATE_OUTPUT = 1;
   static final int DEACTIVATE_OUTPUT = 2;
   private final MessageHandler messageHandler;
   private final PanelStatus panelStatus;

   Commander(MessageHandler messageHandler, PanelStatus panelStatus) {
      this.messageHandler = Objects.requireNonNull(messageHandler);
      this.panelStatus = Objects.requireNonNull(panelStatus);
   }

   public void arming(PanelStatus.GlobalArming newArmingStatus) {
      logger.fine("Setting global arming to: " + newArmingStatus);
      switch(newArmingStatus) {
      case GLOBALLY_DISARMED:
         this.messageHandler.sendCommand(Message.DISARM, SYSTEM);
         break;
      case GLOBALLY_ARMED:
         this.messageHandler.sendCommand(Message.ARM, Pair.with(SYSTEM, AWAY_ARM));
         break;
      default:
         break;
      }

   }

   public void partitionArming(int partitionID, PanelStatus.PartitionArming newArmingStatus) {
      logger.fine("Setting partition " + partitionID + " arming to: " + newArmingStatus);
      switch(newArmingStatus) {
      case DISARMED:
         this.messageHandler.sendCommand(Message.DISARM, partitionID);
         break;
      case AWAY:
         this.messageHandler.sendCommand(Message.ARM, Pair.with(partitionID, AWAY_ARM));
         break;
      case STAY:
         this.messageHandler.sendCommand(Message.ARM, Pair.with(partitionID, STAY_ARM));
         break;
      case NODELAY:
         this.messageHandler.sendCommand(Message.ARM, Pair.with(partitionID, INSTANT_STAY_ARM));
         break;
      default:
         break;
      }

   }

   public boolean armingSupport(char presetMode) {
      return CustomizedArmingModes.CUSTOMIZED_ARMING_MODES.containsKey(presetMode);
   }

   public void armingSet(char presetMode) {
      logger.fine("Setting global arming to preset " + presetMode);
      Integer presetModeInteger = (Integer)CustomizedArmingModes.CUSTOMIZED_ARMING_MODES.get(presetMode);
      if (presetModeInteger != null) {
         this.messageHandler.sendCommand(Message.ARM, Pair.with(SYSTEM, presetModeInteger));
      }
   }

   public void setBypassed(int zoneID, boolean setBypassed) {
      Boolean currentBypassed = panelStatus.getZoneBypass(zoneID);
      if (currentBypassed == null || currentBypassed != setBypassed) {
         logger.fine("Setting zone " + zoneID + " bypass to " + setBypassed);
         panelStatus.updateZoneBypass(zoneID, setBypassed);
         this.messageHandler.sendCommand(Message.SINGLE_ZONE_BYPASS_WRITE, Triplet.with((Integer)null, zoneID, setBypassed));
      } else {
         logger.fine("Zone " + zoneID + " bypass already " + setBypassed + ", skipping command.");
      }
   }

   public void setOutput(int outputID, boolean setStatus) {
      logger.fine((setStatus ? "closing" : "opening") + " output  " + outputID);
      Integer statusInteger = setStatus ? 1 : 2;
      this.messageHandler.sendCommand(Message.SET_OUTPUT, Triplet.with((Integer)null, outputID, statusInteger));
   }

   public void cleanTroubles() {
      logger.fine("Cleaning troubles");
      this.messageHandler.sendCommand(Message.USER_ACTIVITY, CLEAR_FAULTS);
   }
}
