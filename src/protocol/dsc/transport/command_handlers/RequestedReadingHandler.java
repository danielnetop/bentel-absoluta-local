package protocol.dsc.transport.command_handlers;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import protocol.dsc.Message;
import protocol.dsc.commands.DscCommandWithAppSeq;
import protocol.dsc.messages.Reading;

import java.util.ArrayList;
import java.util.List;

public class RequestedReadingHandler extends ChannelDuplexHandler {
   private final List<Message.Response> out = new ArrayList<Message.Response>();

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      // Se il messaggio può essere preparato come DscCommandWithAppSeq, lo scrive direttamente
      DscCommandWithAppSeq prepared = Reading.tryToPrepare(ctx, msg);
      if (prepared != null) {
         ctx.write(prepared, promise);
      } else {
         super.write(ctx, msg, promise);
      }
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      try {
         // Prova a parsare la risposta come Reading, propaga tutte le risposte trovate
         Reading.tryToParse(ctx, msg, this.out);
         if (!this.out.isEmpty()) {
               for (Message.Response response : this.out) {
                  ctx.fireChannelRead(response);
               }
         } else {
               super.channelRead(ctx, msg);
         }
      } finally {
         this.out.clear();
      }
   }
}
