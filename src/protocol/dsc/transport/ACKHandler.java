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
            logger.finest("Sending low ACK");
            var1.write(LowACK.getInstance(), var3);
         } else {
            var3.setSuccess();
         }
      } else {
         super.write(var1, var2, var3);
      }
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof DscCommand) {
         if (msg instanceof LowACK) {
            logger.finest("Low ACK received");
         } else if (msg instanceof EncapsulatedCommandForMultiplePackets) {
            logger.finer("Multiple packets received");
         } else {
            logger.finer("Command received: " + msg);
            ctx.fireChannelRead(msg);
         }

         ctx.fireChannelRead(SimpleMessage.COMMAND_RECEIVED);
      } else {
         super.channelRead(ctx, msg);
      }
   }
}