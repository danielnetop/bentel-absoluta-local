package protocol.dsc.transport.command_handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import protocol.dsc.Message;
import protocol.dsc.NewValue;
import protocol.dsc.commands.TroubleDetailNotification;

import java.util.ArrayList;
import java.util.List;
import org.javatuples.Quartet;

@Sharable
public class TroubleDetailNotificationHandler extends ChannelInboundHandlerAdapter {
   private static final Integer ZONE_DEVICE = 1;

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof TroubleDetailNotification) {
         TroubleDetailNotification notif = (TroubleDetailNotification) msg;
         List<TroubleDetailNotification.Trouble> troubles = notif.getTroubles();
         List<Quartet<Integer, Integer, Integer, Integer>> troubleList = new ArrayList<>(troubles.size());
         for (TroubleDetailNotification.Trouble trouble : troubles) {
               troubleList.add(Quartet.with(
                  trouble.getDeviceModuleType(),
                  trouble.getTroubleType(),
                  trouble.getDeviceModuleNumber(),
                  trouble.getStatus()
               ));
               // Propaga evento ZONE_STATUS_CHANGED se il tipo è zona
               if (ZONE_DEVICE.equals(trouble.getDeviceModuleType())) {
                  ctx.fireChannelRead(new NewValue(Message.ZONE_STATUS_CHANGED, trouble.getDeviceModuleNumber()));
               }
         }
         // Propaga lista dettagli trouble
         ctx.fireChannelRead(new NewValue(Message.TROUBLE_DETAIL_NOTIFICATION, troubleList));
      } else {
         super.channelRead(ctx, msg);
      }
   }
}
