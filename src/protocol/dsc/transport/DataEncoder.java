package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import protocol.dsc.util.Crc16;

import java.nio.ByteOrder;

@Sharable
public class DataEncoder extends MessageToByteEncoder<ByteBuf> {

   @SuppressWarnings("deprecation")
   protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws EncoderException {
      assert out.order() == ByteOrder.BIG_ENDIAN;

      int totalLen = in.readableBytes() + 2; // +2 per CRC
      if (totalLen >= 4 && totalLen <= 32767) {
         // Scrive la lunghezza: 1 byte se <=127, 2 byte con MSB a 1 se >127
         if (totalLen <= 127) {
               out.writeByte(totalLen);
         } else {
               out.writeShort(totalLen | 0x8000);
         }

         out.writeBytes(in);
         // Calcola e scrive CRC16 su tutto il messaggio (inclusa lunghezza)
         int crc = Crc16.CRC_16_CCITT.calculate(out.slice().retain());
         out.writeShort(crc);
      } else {
         throw new EncoderException("invalid message length");
      }
   }
}
