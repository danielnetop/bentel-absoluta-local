package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import protocol.dsc.util.Crc16;
import protocol.dsc.util.Decoders;

import java.nio.ByteOrder;
import java.util.List;

@Sharable
public class DataDecoder extends MessageToMessageDecoder<ByteBuf> {

   @SuppressWarnings("deprecation")
   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws CorruptedFrameException {
      assert in.order() == ByteOrder.BIG_ENDIAN;

      if (in.readableBytes() < 5) {
         throw new CorruptedFrameException("too short frame");
      } else {
         int startIdx = in.readerIndex();
         int len = Decoders.readBytesToFollow(in);
         if (in.readableBytes() < len) {
               throw new CorruptedFrameException("wrong frame length");
         } else {
               ByteBuf payload = in.readSlice(len - 2);
               int crcIdx = in.readerIndex();
               int crcRead = in.readUnsignedShort();
               int crcCalc = Crc16.CRC_16_CCITT.calculate(in.slice(startIdx, crcIdx - startIdx).retain());
               if (crcRead == crcCalc) {
                  out.add(payload.retain());
               } else {
                  throw new CorruptedFrameException(String.format("wrong crc (0x%04X instead of 0x%04X)", crcCalc, crcRead));
               }
         }
      }
   }
}
