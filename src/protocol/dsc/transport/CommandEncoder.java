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
   protected void encode(ChannelHandlerContext ctx, DscCommand cmd, ByteBuf buffer) throws Exception {
      assert buffer.order() == ByteOrder.BIG_ENDIAN;

      if (cmd instanceof LowACK) {
         logger.finer("Low ACK encoded");
      } else {
         buffer.writeShort(cmd.getCommandNumber());
         // Gestisce sequenza applicativa se necessario
         if (cmd instanceof DscCommandWithAppSeq) {
               int appSeq = SequenceHandlersHelper.getCounters(ctx).nextAppSeq();
               ((DscCommandWithAppSeq) cmd).setAppSeq(appSeq);
               buffer.writeByte(appSeq);
         }

         cmd.writeTo(buffer);
         logger.finer("Command encoded: " + cmd);
      }
   }
}
