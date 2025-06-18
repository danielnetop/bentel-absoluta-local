package protocol.dsc.transport;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Sharable
public class Aes128ecbEncrypter extends MessageToMessageEncoder<ByteBuf> {
   private static final int KEY_SIZE = 16;
   public static final int BLOCK_SIZE = 16;
   public static final AttributeKey<byte[]> ENCRYPT_KEY = AttributeKey.valueOf("Aes128ecbEncrypter.key");

   protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      byte[] key = (byte[]) ctx.channel().attr(ENCRYPT_KEY).get();
      if (key != null) {
         Preconditions.checkArgument(key.length == KEY_SIZE);
         Preconditions.checkArgument(in.readableBytes() % BLOCK_SIZE == 0);
         ByteBuffer src = in.nioBuffer();
         ByteBuffer encrypted = encrypt(key, src);
         out.add(Unpooled.wrappedBuffer(encrypted));
      } else {
         out.add(in.retain());
      }
   }

   // Cifra il buffer usando AES/ECB/NoPadding con la chiave fornita
   private static ByteBuffer encrypt(byte[] key, ByteBuffer src) throws Exception {
      ByteBuffer dst = ByteBuffer.allocate(src.remaining());
      Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec);
      cipher.doFinal(src, dst);
      return (ByteBuffer) dst.flip();
   }
}
