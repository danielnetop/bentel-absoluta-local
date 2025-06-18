package protocol.dsc.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class BCDCharset extends Charset {
   public BCDCharset() {
      super("BCD", (String[])null);
   }

   @Override
   public boolean contains(Charset cs) {
      return this.equals(cs);
   }

   @Override
   public CharsetDecoder newDecoder() {
      // Decodifica: da byte BCD a caratteri
      return new CharsetDecoder(this, 2.0F, 2.0F) {
         @Override
         protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            while (in.remaining() > 0 && out.remaining() > 0) {
               byte b = in.get();
               out.append(toChar((b >> 4) & 0xF));
               out.append(toChar(b & 0xF));
            }
            return in.remaining() > 0 ? CoderResult.OVERFLOW : CoderResult.UNDERFLOW;
         }

         // Converte nibble in carattere ASCII ('0'-'9', 'A'-'F')
         private char toChar(int nibble) {
            return (char) (nibble < 10 ? '0' + nibble : 'A' + (nibble - 10));
         }
      };
   }

   @Override
   public CharsetEncoder newEncoder() {
      // Codifica: da caratteri a byte BCD
      return new CharsetEncoder(this, 0.5F, 1.0F, new byte[1]) {
         private final int[] nibbles = new int[2];
         private int nibbleIndex;

         @Override
         protected void implReset() {
            this.nibbleIndex = 0;
         }

         @Override
         protected CoderResult implFlush(ByteBuffer out) {
            // Se rimane un nibble dispari, lo scrive come nibble alto
            if (this.nibbleIndex == 1) {
               out.put((byte) (this.nibbles[0] << 4));
            }
            return CoderResult.UNDERFLOW;
         }

         @Override
         protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            while (in.remaining() > 0 && out.remaining() > 0) {
               char c = in.get();
               // Supporta cifre decimali e lettere esadecimali (case insensitive)
               if ('0' <= c && c <= '9') {
                  this.nibbles[this.nibbleIndex] = c - '0';
               } else if (c >= 'A' && c <= 'F') {
                  this.nibbles[this.nibbleIndex] = c - 'A' + 10;
               } else if (c >= 'a' && c <= 'f') {
                  this.nibbles[this.nibbleIndex] = c - 'a' + 10;
               } else {
                  continue; // ignora caratteri non validi
               }

               if (this.nibbleIndex == 1) {
                  out.put((byte) (this.nibbles[0] << 4 | this.nibbles[1]));
               }
               this.nibbleIndex ^= 1;
            }
            return in.remaining() > 0 ? CoderResult.OVERFLOW : CoderResult.UNDERFLOW;
         }
      };
   }
}