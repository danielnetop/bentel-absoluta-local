package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;
import protocol.dsc.commands.DscCommand;
import protocol.dsc.commands.DscCommandWithAppSeq;
import protocol.dsc.commands.LowACK;

import java.nio.ByteOrder;
import java.util.logging.Logger;

@Sharable
public class CommandEncoder extends MessageToByteEncoder<DscCommand> {
   private static final Logger logger = Logger.getLogger(CommandEncoder.class.getName());

   @SuppressWarnings("deprecation")
   protected void encode(ChannelHandlerContext var1, DscCommand var2, ByteBuf var3) throws Exception {
      assert var3.order() == ByteOrder.BIG_ENDIAN;

      if (var2 instanceof LowACK) {
         logger.fine("low ACK encoded");
      } else {
         var3.writeShort(var2.getCommandNumber());
         if (var2 instanceof DscCommandWithAppSeq) {
            int var4 = SequenceHandlersHelper.getCounters(var1).nextAppSeq();
            ((DscCommandWithAppSeq)var2).setAppSeq(var4);
            var3.writeByte(var4);
         }

         var2.writeTo(var3);
         logger.fine("command encoded: " + var2);
      }
   }
}
