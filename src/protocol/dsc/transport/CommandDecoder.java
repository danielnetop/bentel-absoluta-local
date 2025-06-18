package protocol.dsc.transport;

import com.google.common.collect.ImmutableMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;

import protocol.dsc.commands.*;
import protocol.dsc.errors.DscProtocolException;
import protocol.dsc.errors.WrongCommandLengthException;
import protocol.dsc.util.DscUtils;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Sharable
public class CommandDecoder extends MessageToMessageDecoder<ByteBuf> {
   private static final Logger logger = Logger.getLogger(CommandDecoder.class.getName());
   // Mappa: numero comando -> classe comando
   private static final Map<Integer, Class<? extends DscCommand>> COMMANDS = initCommandsWith(
      AccessCodeLengthNotification.class, AccessCodes.class, AccessCodesResponse.class, AccessLevelLeadInOut.class,
      AlarmMemoryInformation.class, ArmingDisarmingNotification.class, ArmingPreAlertNotification.class,
      CommandError.class, CommandOutput.class, CommandOutputActivation.class, CommandRequest.class,
      CommandResponse.class, Configuration.class, EncapsulatedCommandForMultiplePackets.class, EndSession.class,
      EnterAccessLevel.class, EnterConfigurationMode.class, EntryDelayNotification.class, EventBufferRead.class,
      EventBufferReadResponse.class, EventReportingConfigurationRead.class, EventReportingConfigurationReadResponse.class,
      EventReportingConfigurationWrite.class, ExitConfigurationMode.class, ExitDelayNotification.class,
      LifeStyleZoneStatus.class, MiscellaneousPreAlertNotification.class, OpenSession.class,
      PartitionAlarmMemoryNotification.class, PartitionArmControl.class, PartitionAssignmentConfiguration.class,
      PartitionAssignments.class, PartitionAssignmentsResponse.class, PartitionDisarmControl.class,
      PartitionQuickExitNotification.class, PartitionReadyStatusNotification.class, PartitionStatus.class, Poll.class,
      RequestAccess.class, SectionRead.class, SectionReadResponse.class, SingleZoneBypassStatus.class,
      SingleZoneBypassWrite.class, SoftwareVersion.class, SystemCapabilities.class, SystemTroubleStatus.class,
      TextNotification.class, TimeDateBroadcastNotification.class, TroubleDetail.class, TroubleDetailNotification.class,
      UserActivity.class, UserPartitionAssignmentConfiguration.class, ZoneAlarmStatus.class,
      ZoneAssignmentConfiguration.class, ZoneBypassStatus.class, ZoneStatus.class);

   @SafeVarargs
   private static Map<Integer, Class<? extends DscCommand>> initCommandsWith(Class<? extends DscCommand>... commandClasses) {
      try {
         Map<Integer, Class<? extends DscCommand>> commandMap = new HashMap<>();
         for (Class<? extends DscCommand> clazz : commandClasses) {
               DscCommand instance = clazz.getDeclaredConstructor().newInstance();
               Integer commandNumber = DscUtils.validateUShort(instance.getCommandNumber());
               if (commandMap.containsKey(commandNumber)) {
                  throw new IllegalStateException(String.format("Duplicated command number 0x%04X", commandNumber));
               }
               commandMap.put(commandNumber, clazz);
         }
         return ImmutableMap.copyOf(commandMap);
      } catch (ReflectiveOperationException e) {
         throw new AssertionError("Error initializing command map", e);
      }
   }

   public static boolean knows(Class<? extends DscCommand> clazz) {
      return COMMANDS.containsValue(clazz);
   }

   @Override
   @SuppressWarnings("deprecation")
   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
         throws DscProtocolException, CorruptedFrameException {
      assert in.order() == ByteOrder.BIG_ENDIAN;

      if (SequenceHandlersHelper.isLowACK(in)) {
         out.add(LowACK.getInstance());
         return;
      }

      if (in.readableBytes() < 2) {
         throw new CorruptedFrameException("Invalid frame length");
      }

      int commandNumber = in.readUnsignedShort();

      try {
         DscCommand command = this.newCommand(commandNumber);
         // Gestisce sequenza applicativa se richiesta dal comando
         if (command instanceof DscCommandWithAppSeq) {
               short appSeq = in.readUnsignedByte();
               ((DscCommandWithAppSeq) command).setAppSeq(appSeq);
         }

         if (command instanceof CommandRequest) {
               this.parseCommandRequest((CommandRequest) command, in);
         } else {
               command.readFrom(in);
         }

         out.add(command);
      } catch (IndexOutOfBoundsException ex) {
         throw new WrongCommandLengthException(commandNumber, "Too short command data");
      }
   }

   private DscCommand newCommand(int commandNumber) {
      try {
         Class<? extends DscCommand> clazz = COMMANDS.get(commandNumber);
         if (clazz != null) {
               return clazz.getDeclaredConstructor().newInstance();
         } else {
               return new UnknownCommand(commandNumber);
         }
      } catch (ReflectiveOperationException e) {
         throw new AssertionError("Unexpected error creating command instance", e);
      }
   }

   // Parsing specifico per CommandRequest (comando richiesto annidato)
   private void parseCommandRequest(CommandRequest request, ByteBuf in) {
      int requestedCmdNumber = in.readUnsignedShort();
      DscCommand requestedCmd = this.newCommand(requestedCmdNumber);
      if (requestedCmd instanceof DscRequestableCommand) {
         DscRequestableCommand reqCmd = (DscRequestableCommand) requestedCmd;
         reqCmd.readRequestDataFrom(in);
         request.setRequestedCmd(reqCmd);
         request.readCodeFrom(in);
      } else {
         logger.warning("Unknown or unexpected requested command: " + requestedCmd.getCommandNumber());
      }
   }
}