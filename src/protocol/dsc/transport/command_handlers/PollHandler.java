package protocol.dsc.transport.command_handlers;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

import protocol.dsc.commands.DscCommand;
import protocol.dsc.util.LogOnFailure;

import java.util.logging.Logger;

// Invia periodicamente un comando di poll quando il canale è inattivo in scrittura.
public class PollHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = Logger.getLogger(PollHandler.class.getName());
   private static final AttributeKey<Boolean> POLL_KEY = AttributeKey.valueOf("PollHandler.poll");
   private final PollFactory pollFactory;

   public PollHandler(PollFactory pollFactory) {
      this.pollFactory = Preconditions.checkNotNull(pollFactory);
   }

   // Invia un comando di poll quando scatta l'IdleStateEvent in scrittura, se abilitato.
   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof IdleStateEvent && ((IdleStateEvent)evt).state() == IdleState.WRITER_IDLE) {
         if (isPollEnabled(ctx.channel())) {
            logger.finer("Sending poll");
            DscCommand pollCommand = this.pollFactory.createPoll();
            ctx.write(pollCommand).addListener(LogOnFailure.INSTANCE);
         }
      } else {
         super.userEventTriggered(ctx, evt);
      }
   }

   public static boolean isPollEnabled(Channel channel) {
      return Boolean.TRUE.equals(channel.attr(POLL_KEY).get());
   }

   public static void setPollEnabled(Channel channel, boolean enabled) {
      channel.attr(POLL_KEY).set(enabled);
   }

   public interface PollFactory {
      DscCommand createPoll();
   }
}
