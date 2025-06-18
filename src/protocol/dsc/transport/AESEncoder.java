package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;

@Sharable
public class AESEncoder extends MessageToByteEncoder<ByteBuf> {
   private static final AttributeKey<AESHelper> HELPER_KEY = AttributeKey.valueOf("AESEncoder.helper");

   protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
      AESHelper helper = ctx.channel().attr(HELPER_KEY).get();
      if (helper != null) {
         helper.process(in, out); // Cifra se helper presente
      } else {
         out.writeBytes(in); // Passa dati in chiaro se nessuna chiave impostata
      }
   }

   // Imposta la chiave AES sul canale
   public static void setKey(Channel ch, byte[] key) {
      AESHelper helper = AESHelper.getInstance(key, 1);
      ch.attr(HELPER_KEY).set(helper);
   }
}
