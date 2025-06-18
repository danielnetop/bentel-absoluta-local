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

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      // Invia LowACK se richiesto dal marker COMMAND_RECEIVED
      if (msg == SimpleMessage.COMMAND_RECEIVED) {
         if (TransportLayerEncoder.isOutgoingACKRequired(ctx)) {
               logger.finer("Sending low ACK");
               ctx.write(LowACK.getInstance(), promise);
         } else {
               promise.setSuccess();
         }
      } else {
         super.write(ctx, msg, promise);
      }
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof DscCommand) {
         if (msg instanceof LowACK) {
               logger.finer("Low ACK received");
         } else if (msg instanceof EncapsulatedCommandForMultiplePackets) {
               logger.finer("Multiple packets received");
         } else {
               logger.finer("Command received: " + msg);
               ctx.fireChannelRead(msg);
         }
         // Notifica ricezione comando (per ACK)
         ctx.fireChannelRead(SimpleMessage.COMMAND_RECEIVED);
      } else {
         super.channelRead(ctx, msg);
      }
   }
}