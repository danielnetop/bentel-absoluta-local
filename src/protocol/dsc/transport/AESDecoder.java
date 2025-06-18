package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;
import java.util.List;

@Sharable
public class AESDecoder extends MessageToMessageDecoder<ByteBuf> {
   private static final AttributeKey<AESHelper> HELPER_KEY = AttributeKey.valueOf("AESDecoder.helper");

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws CorruptedFrameException {
      AESHelper helper = ctx.channel().attr(HELPER_KEY).get();
      if (helper != null) {
         ByteBuf decoded = ctx.alloc().buffer(in.readableBytes());
         helper.process(in, decoded); // Decifra se helper presente
         out.add(decoded);
      } else {
         out.add(in.retain()); // Passa dati in chiaro se nessuna chiave impostata
      }
   }

   // Imposta la chiave AES sul canale
   public static void setKey(Channel ch, byte[] key) {
      AESHelper helper = AESHelper.getInstance(key, 2);
      ch.attr(HELPER_KEY).set(helper);
   }
}
