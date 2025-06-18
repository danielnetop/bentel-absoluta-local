package protocol.dsc.transport.command_handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;

import protocol.dsc.commands.DscCommandWithAppSeq;
import protocol.dsc.messages.Writing;

@Sharable
public class WritingHandler extends ChannelOutboundHandlerAdapter {
   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      // Se il messaggio può essere preparato come DscCommandWithAppSeq, lo scrive direttamente
      DscCommandWithAppSeq prepared = Writing.tryToPrepare(ctx, msg);
      if (prepared != null) {
         ctx.write(prepared, promise);
      } else {
         super.write(ctx, msg, promise);
      }
   }
}