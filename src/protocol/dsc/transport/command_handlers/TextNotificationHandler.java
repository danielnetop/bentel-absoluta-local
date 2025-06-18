package protocol.dsc.transport.command_handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import protocol.dsc.DscError;
import protocol.dsc.Message;
import protocol.dsc.NewValue;
import protocol.dsc.commands.TextNotification;

@Sharable
public class TextNotificationHandler extends ChannelInboundHandlerAdapter {
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof TextNotification) {
         TextNotification notif = (TextNotification) msg;
         try {
               String text = notif.getMessage();
               ctx.fireChannelRead(new NewValue(Message.TEXT_NOTIFICATION, text));
         } catch (UnsupportedOperationException ex) {
               ctx.fireChannelRead(DscError.newMessageError(Message.TEXT_NOTIFICATION, null, ex));
         }
      } else {
         super.channelRead(ctx, msg);
      }
   }
}
