package protocol.dsc.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import protocol.dsc.commands.DscCommand;

@Sharable
public class FlushCommandHandler extends ChannelOutboundHandlerAdapter {
    // Esegue writeAndFlush solo per DscCommand, altrimenti delega normalmente
   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (msg instanceof DscCommand) {
         ctx.writeAndFlush(msg, promise);
      } else {
         super.write(ctx, msg, promise);
      }
   }
}
