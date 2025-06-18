package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

@Sharable
public class Aes128ecbPadder extends MessageToMessageEncoder<ByteBuf> {
   private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

   protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      byte[] key = (byte[]) ctx.channel().attr(Aes128ecbEncrypter.ENCRYPT_KEY).get();
      // Padding solo se chiave presente e lunghezza non multipla di 16
      if (key != null && in.readableBytes() % 16 != 0) {
         int padLen = 16 - in.readableBytes() % 16;

         assert 0 < padLen && padLen < 16;

         byte[] bytes = new byte[padLen];
         SECURE_RANDOM.nextBytes(bytes);
         out.add(Unpooled.wrappedBuffer(new ByteBuf[]{in.retain(), Unpooled.wrappedBuffer(bytes)}));
      } else {
         out.add(in.retain());
      }
   }
}
