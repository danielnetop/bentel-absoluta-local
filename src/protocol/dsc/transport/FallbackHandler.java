package protocol.dsc.transport;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import protocol.dsc.DscError;
import protocol.dsc.Priority;
import protocol.dsc.commands.CommandError;
import protocol.dsc.commands.DscCommand;
import protocol.dsc.errors.DscProtocolException;
import protocol.dsc.util.LogOnFailure;

import java.util.logging.Logger;

@Sharable
public class FallbackHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = Logger.getLogger(FallbackHandler.class.getName());

   // Gestione delle eccezioni: invia errore fatale e chiude il canale
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      ctx.fireChannelRead(DscError.newFatalError(cause));
      ctx.fireUserEventTriggered(SimpleMessage.CLOSING_CHANNEL_EVENT);
      if (cause instanceof DscProtocolException) {
            logger.severe("Protocol exception received (sending an error message and closing the channel): " + cause);
            CommandError err = new CommandError((DscProtocolException) cause);
            err.setPriority((Priority) null);
            ctx.write(err).addListener(ChannelFutureListener.CLOSE).addListener(LogOnFailure.INSTANCE);
      } else {
            logger.severe("Exception received (closing the channel) " + cause);
            ctx.close().addListener(LogOnFailure.INSTANCE);
      }
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      // Propaga comando ricevuto se è il marker COMMAND_RECEIVED
      if (msg == SimpleMessage.COMMAND_RECEIVED) {
            ctx.write(msg);
      }

      // Logga comandi non gestiti
      if (msg instanceof DscCommand) {
            logger.fine("Command received, but not handled: " + msg);
      } else {
            super.channelRead(ctx, msg);
      }
   }
}
