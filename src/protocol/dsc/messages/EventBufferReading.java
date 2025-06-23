package protocol.dsc.messages;

import io.netty.channel.ChannelHandlerContext;
import protocol.dsc.Message;
import protocol.dsc.NewValue;
import protocol.dsc.commands.DscCommandWithAppSeq;
import protocol.dsc.commands.EventBufferRead;
import protocol.dsc.commands.EventBufferReadResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import org.javatuples.Pair;
import org.javatuples.Septet;


public class EventBufferReading extends Reading<Pair<Integer, Integer>, List<Septet<Calendar, Integer, Boolean, Integer, Integer, Integer, List<Integer>>>, EventBufferReadResponse> {
   private static final Logger logger = Logger.getLogger(EventBufferReading.class.getName());

   public EventBufferReading() {
      super(EventBufferReadResponse.class);
   }

   protected void parseResponse(ChannelHandlerContext var1, EventBufferReadResponse var2, List<Message.Response> var3) {
      if (var2.getBufferID() != 3) {
         logger.warning("unmanaged buffer ID: " + var2.getBufferID());
      } else {
         List<EventBufferReadResponse.Event> var4 = var2.getEvents();

         assert var4.size() == var2.getNumberOfEvents();

         Pair<Integer, Integer> var5 = Pair.with(var2.getEventNumber(), var2.getNumberOfEvents());
         List<Septet<Calendar, Integer, Boolean, Integer, Integer, Integer, List<Integer>>> var6 = new ArrayList<>(var4.size());

         for (int i = 0; i < var4.size(); i++) {
            EventBufferReadResponse.Event var8 = var4.get(i);
            if (var8.getFlags() != 9) {
               logger.warning("unexpected flags: " + var8.getFlags());
               return;
            }

            int var9 = var8.getPartitionMaskUnusedBytes();
            List<Integer> var10 = null;
            if (var9 == 0) {
               var10 = var8.getPartitions();
            } else if (var9 != 65535) {
               logger.warning("unexpected partition mask unused bytes: " + var9);
            }

            var6.add(Septet.with(var8.getDateTimeStamp(), var8.getEventClass(), var8.isRestore(), var8.getEventCode(), var8.getWhereWhy(), var8.getWho(), var10));
         }

         var3.add(new NewValue(this, var5, var6));
      }
   }

   protected DscCommandWithAppSeq prepareCommand(ChannelHandlerContext var1, Pair<Integer, Integer> var2) throws Exception {
      EventBufferRead var3 = new EventBufferRead();
      var3.setBufferID(3);
      var3.setEventNumber((Integer)var2.getValue0());
      var3.setNumberOfEvents((Integer)var2.getValue1());
      return var3;
   }
}
