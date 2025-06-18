package protocol.dsc.transport.command_handlers;

import com.google.common.collect.ImmutableSet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

import protocol.dsc.Message;
import protocol.dsc.NewValue;
import protocol.dsc.commands.AbstractPartitionReqCommand;
import protocol.dsc.commands.AccessLevelLeadInOut;
import protocol.dsc.commands.ArmingDisarmingNotification;
import protocol.dsc.commands.ArmingPreAlertNotification;
import protocol.dsc.commands.EntryDelayNotification;
import protocol.dsc.commands.ExitDelayNotification;
import protocol.dsc.commands.LifeStyleZoneStatus;
import protocol.dsc.commands.MiscellaneousPreAlertNotification;
import protocol.dsc.commands.PartitionAlarmMemoryNotification;
import protocol.dsc.commands.PartitionQuickExitNotification;
import protocol.dsc.commands.PartitionReadyStatusNotification;
import protocol.dsc.commands.TimeDateBroadcastNotification;

import java.util.Set;
import java.util.logging.Logger;

import org.javatuples.Quartet;

@Sharable
public class MiscNotificationHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = Logger.getLogger(MiscNotificationHandler.class.getName());

   // Set di classi che rappresentano le notifiche di stato partizione
   private static final Set<Class<? extends AbstractPartitionReqCommand>> PARTITION_STATUS_NOTIFICATIONS =
      ImmutableSet.of(
         ArmingDisarmingNotification.class,
         ArmingPreAlertNotification.class,
         EntryDelayNotification.class,
         ExitDelayNotification.class,
         MiscellaneousPreAlertNotification.class,
         PartitionAlarmMemoryNotification.class,
         PartitionQuickExitNotification.class,
         PartitionReadyStatusNotification.class
      );

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      // Notifiche di stato partizione
      if (PARTITION_STATUS_NOTIFICATIONS.contains(msg.getClass())) {
         AbstractPartitionReqCommand partitionCommand = (AbstractPartitionReqCommand) msg;
         Integer partitionNumber = partitionCommand.getPartitionNumber();
         if (partitionNumber != null) {
            ctx.fireChannelRead(new NewValue(Message.PARTITION_STATUS_CHANGED, partitionNumber));
            // Notifica specifica per memoria allarme partizione
            if (partitionCommand instanceof PartitionAlarmMemoryNotification) {
               ctx.fireChannelRead(new NewValue(Message.PARTITION_ALARM_MEMORY_CHANGED, partitionNumber));
            }
         }
      }
      // Notifica di stato zona stile di vita
      else if (msg instanceof LifeStyleZoneStatus) {
         LifeStyleZoneStatus zoneStatus = (LifeStyleZoneStatus) msg;
         int zoneNumber = zoneStatus.getZoneNumber();
         ctx.fireChannelRead(new NewValue(Message.ZONE_STATUS_CHANGED, zoneNumber));
      }
      // Notifica di accesso livello utente (senza partizione)
      else if (msg instanceof AccessLevelLeadInOut) {
         AccessLevelLeadInOut accessLevel = (AccessLevelLeadInOut) msg;
         if (accessLevel.getPartitionNumber() == null) {
            ctx.fireChannelRead(new NewValue(
               Message.ACCESS_LEVEL_LEAD_IN_OUT,
               Quartet.with(
                  accessLevel.getType(),
                  accessLevel.getUser(),
                  accessLevel.getAccess(),
                  accessLevel.getMode()
               )
            ));
         } else {
            logger.warning("Unexpected partition for AccessLevelLeadInOut: " + accessLevel.getPartitionNumber());
         }
      }
      // Notifica broadcast di data/ora
      else if (msg instanceof TimeDateBroadcastNotification) {
         logger.fine("Time and date received: " + msg.toString());
      }
      // Altri messaggi non gestiti: passa al prossimo handler
      else {
         super.channelRead(ctx, msg);
      }
   }
}
