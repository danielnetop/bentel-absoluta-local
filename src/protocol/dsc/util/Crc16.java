package protocol.dsc.util;

import io.netty.buffer.ByteBuf;

public class Crc16 {
   public static final Crc16 CRC_16_CCITT = new Crc16(0x1021, 0xFFFF, 0x0000); // polinomio, valore iniziale, xor finale
   private final int poly;
   private final int initialVal;
   private final int finalXor;
   private final int[] lookupTable;

   public Crc16(int poly, int initialVal, int finalXor) {
      this.poly = DscUtils.validateUShort(poly);
      this.initialVal = DscUtils.validateUShort(initialVal);
      this.finalXor = DscUtils.validateUShort(finalXor);
      this.lookupTable = new int[256];

      // Precalcola la tabella di lookup per velocizzare il calcolo CRC
      for (int i = 0; i < 256; ++i) {
         this.lookupTable[i] = computeCrcForByte(i);
      }
   }

   private int computeCrcForByte(int value) {
      int crc = value << 8;
      for (int bit = 0; bit < 8; ++bit) {
         if ((crc & 0x8000) != 0) {
            crc = (crc << 1) ^ this.poly;
         } else {
            crc <<= 1;
         }
         crc &= 0xFFFF;
      }
      return crc;
   }

   public int calculate(ByteBuf buf) {
      try {
         int crc = this.initialVal;
         while (buf.isReadable()) {
            short b = buf.readUnsignedByte();
            int idx = (crc >> 8) & 0xFF;
            crc = this.lookupTable[b ^ idx] ^ ((crc & 0xFF) << 8);
         }
         return crc ^ this.finalXor;
      } finally {
         buf.release();
      }
   }
}
