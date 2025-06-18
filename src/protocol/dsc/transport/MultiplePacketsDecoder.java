package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;
import protocol.dsc.errors.WrongCommandLengthException;
import protocol.dsc.util.Decoders;

import java.nio.ByteOrder;
import java.util.List;

@Sharable
public class MultiplePacketsDecoder extends MessageToMessageDecoder<ByteBuf> {

   @SuppressWarnings("deprecation")
   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      assert in.order() == ByteOrder.BIG_ENDIAN;

      // Se il pacchetto inizia con 1571 (0x0623), gestisce pacchetti multipli
      if (in.readableBytes() >= 2 && in.getShort(in.readerIndex()) == 1571) {
         out.add(in.readSlice(2).retain()); // Header

         try {
               while (in.isReadable()) {
                  int len = Decoders.readBytesToFollow(in); // Legge lunghezza pacchetto
                  out.add(in.readSlice(len).retain());
               }
         } catch (IndexOutOfBoundsException e) {
               throw new WrongCommandLengthException(1571, "unexpected packet length");
         }
      } else {
         out.add(in.retain()); // Pacchetto singolo, passa direttamente
      }
   }
}