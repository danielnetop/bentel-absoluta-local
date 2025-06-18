package protocol.dsc.transport.command_handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import protocol.dsc.Message;
import protocol.dsc.NewValue;
import protocol.dsc.commands.UserPartitionAssignmentConfiguration;

import java.util.List;
import java.util.Map.Entry;
import org.javatuples.Pair;

@Sharable
public class UserPartitionAssignmentConfigurationHandler extends ChannelInboundHandlerAdapter {
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof UserPartitionAssignmentConfiguration) {
         UserPartitionAssignmentConfiguration conf = (UserPartitionAssignmentConfiguration) msg;
         // Propaga ogni assegnazione come NewValue con Pair (user, partizioni)
         for (Entry<Integer, List<Integer>> entry : conf.getPartitionAssignments().entrySet()) {
               Integer user = entry.getKey();
               List<Integer> partitions = entry.getValue();
               ctx.fireChannelRead(new NewValue(Message.USER_PARTITION_ASSIGNMENT_CONFIGURATION, Pair.with(user, partitions)));
         }
      } else {
         super.channelRead(ctx, msg);
      }
   }
}