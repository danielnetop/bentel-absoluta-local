package protocol.dsc.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import io.netty.channel.ChannelHandlerContext;
import protocol.dsc.Message;
import protocol.dsc.NewValue;
import protocol.dsc.base.DscCharsets;
import protocol.dsc.commands.Configuration;

import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;

public class SingleLabelReading extends RequestableCommandReading<Void, String, Configuration> {
   private static final Logger logger = Logger.getLogger(SingleLabelReading.class.getName());
   private final int optionId;
   private final Integer number;
   private final Charset charset;

   public SingleLabelReading(int var1, Integer var2) {
      this(var1, var2, DscCharsets.UNICODE);
   }

   public SingleLabelReading(int var1, Integer var2, Charset var3) {
      super(Configuration.class);
      this.optionId = var1;
      this.number = var2;
      this.charset = (Charset)Preconditions.checkNotNull(var3);
   }

   protected Configuration prepareRequest(ChannelHandlerContext var1, Void var2) {
      Configuration var3 = new Configuration();
      var3.setOptionId(this.optionId);
      if (this.number != null) {
         var3.setOptionIdOffsetFrom(this.number);
         var3.setOptionIdOffsetTo(this.number);
      } else {
         var3.setOptionIdOffsetFrom((Integer)null);
      }

      return var3;
   }

   protected void parseResponse(ChannelHandlerContext var1, Configuration var2, List<Message.Response> var3) {
      if (var2.getOptionId() == this.optionId) {
         Integer var4 = var2.getOptionIdOffsetFrom();
         List<String> var5 = var2.getStrings(this.charset);
         if (this.number != null && var4 == null) {
            logger.warning(String.format("unexpected null from for option id 0x%02X", this.optionId));
            return;
         }

         if (Objects.equal(this.number, var4) || this.number == null && var4 == 0) {
            var3.add(new NewValue(this, var5.get(0)));
         }
      }

   }
}
