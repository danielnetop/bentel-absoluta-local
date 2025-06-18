package protocol.dsc.transport.command_handlers;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;

import protocol.dsc.commands.EndSession;
import protocol.dsc.commands.LowACK;
import protocol.dsc.transport.SimpleMessage;

import java.util.logging.Logger;

@Sharable
public class EndSessionHandler extends ChannelDuplexHandler {
   private static final Logger logger = Logger.getLogger(EndSessionHandler.class.getName());

   // Intercetta la scrittura di EndSession e notifica la chiusura del canale.
   @Override
   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (msg instanceof EndSession) {
         logger.fine("Sending end session");
         ctx.fireUserEventTriggered(SimpleMessage.CLOSING_CHANNEL_EVENT);
      }
      super.write(ctx, msg, promise);
   }

   // Notifica la chiusura e risponde con LowACK chiudendo il canale.
   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof EndSession) {
         logger.info("Mobile App o Boss hanno richiesto la chiusura sessione, richiesta ricevuta");
         ctx.fireUserEventTriggered(SimpleMessage.CLOSING_CHANNEL_EVENT);
         ctx.write(LowACK.getInstance()).addListener(ChannelFutureListener.CLOSE);
      } else {
         super.channelRead(ctx, msg);
      }
   }
}
