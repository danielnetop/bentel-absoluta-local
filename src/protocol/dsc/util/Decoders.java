package protocol.dsc.util;

import io.netty.buffer.ByteBuf;

public final class Decoders {

   public static int readBytesToFollow(ByteBuf buf) {
      short firstByte = buf.readUnsignedByte();
      int length;
      if ((firstByte & 0x80) == 0) {
         length = firstByte;
      } else {
         short secondByte = buf.readUnsignedByte();
         length = ((firstByte & 0x7F) << 8) | secondByte;
      }
      return length;
   }

   private Decoders() { }
}
