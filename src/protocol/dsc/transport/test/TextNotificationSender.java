package protocol.dsc.transport.test;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import protocol.dsc.commands.EndSession;
import protocol.dsc.commands.TextNotification;
import protocol.dsc.util.LogOnFailure;

@Sharable
public class TextNotificationSender extends ChannelHandlerAdapter {
   public void handlerAdded(ChannelHandlerContext var1) throws Exception {
      TextNotification var2 = new TextNotification();
      var2.setMessage(3, "UNICODE test message!");
      var1.write(var2).addListener(LogOnFailure.INSTANCE);
      TextNotification var3 = new TextNotification();
      var3.setMessage(2, "ASCII test message!");
      var1.write(var3).addListener(LogOnFailure.INSTANCE);
      var1.write(new EndSession()).addListener(LogOnFailure.INSTANCE);
   }
}
