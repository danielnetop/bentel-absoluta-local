package protocol.dsc.transport;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import protocol.dsc.commands.DscCommand;
import protocol.dsc.commands.EncapsulatedCommandForMultiplePackets;
import protocol.dsc.commands.LowACK;

import java.util.logging.Logger;

@Sharable
public class ACKHandler extends ChannelDuplexHandler {
   private static final Logger logger = Logger.getLogger(ACKHandler.class.getName());

   public void write(ChannelHandlerContext var1, Object var2, ChannelPromise var3) throws Exception {
      if (var2 == SimpleMessage.COMMAND_RECEIVED) {
         if (TransportLayerEncoder.isOutgoingACKRequired(var1)) {
            logger.finer("sending low ACK");
            var1.write(LowACK.getInstance(), var3);
         } else {
            var3.setSuccess();
         }
      } else {
         super.write(var1, var2, var3);
      }

   }

   public void channelRead(ChannelHandlerContext var1, Object var2) throws Exception {
      if (var2 instanceof DscCommand) {
         if (var2 instanceof LowACK) {
            logger.finer("low ACK received");
         } else if (var2 instanceof EncapsulatedCommandForMultiplePackets) {
            logger.finer("multiple packets received");
         } else {
            logger.finer("command received: " + var2);
            var1.fireChannelRead(var2);
         }

         var1.fireChannelRead(SimpleMessage.COMMAND_RECEIVED);
      } else {
         super.channelRead(var1, var2);
      }

   }
}
