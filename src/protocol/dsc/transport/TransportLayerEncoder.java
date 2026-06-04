package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.logging.Logger;

@Sharable
public class TransportLayerEncoder extends MessageToByteEncoder<ByteBuf> {
   private static final Logger logger = Logger.getLogger(TransportLayerEncoder.class.getName());

   protected void encode(ChannelHandlerContext var1, ByteBuf var2, ByteBuf var3) {
      boolean var4 = SequenceHandlersHelper.isLowACK(var2);
      SequenceHandlersHelper.Counters var5 = SequenceHandlersHelper.getCounters(var1);
      if (!var4 && !var5.isReadyForANewCommand()) {
         logger.fine("Command sent before confirmation of the previous");
      }

      var5.setNextSequenceNumber(var4);
      int var6 = var5.sequenceNumber();
      int var7 = var5.remoteSequenceNumber();
      var3.writeByte(var6);
      var3.writeByte(var7);
      var3.writeBytes(var2);
      var5.setSentRemoteSequenceNumber(var7);
   }

   public static boolean isReadyForANewCommand(ChannelHandlerContext var0) {
      SequenceHandlersHelper.Counters var1 = SequenceHandlersHelper.getCounters(var0);
      return var1.isReadyForANewCommand();
   }

   public static boolean isOutgoingACKRequired(ChannelHandlerContext var0) {
      SequenceHandlersHelper.Counters var1 = SequenceHandlersHelper.getCounters(var0);
      return var1.isOutgoingACKRequired();
   }
}
