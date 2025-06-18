package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.CorruptedFrameException;
import protocol.dsc.util.DscUtils;

import java.io.IOException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

class AESHelper {
   static final int DECRYPT_MODE = 2;
   static final int ENCRYPT_MODE = 1;
   private static final int BLOCK_SIZE = 16;
   private final byte[] buffer = new byte[128];
   private final int mode;
   private final Cipher cipher;

   // Restituisce istanza solo se chiave non nulla
   static AESHelper getInstance(byte[] key, int mode) {
      return key == null ? null : new AESHelper(key, mode);
   }

   private AESHelper(byte[] key, int mode) {
      if (mode != DECRYPT_MODE && mode != ENCRYPT_MODE) {
         throw new IllegalArgumentException("invalid mode " + mode);
      } else {
         this.mode = mode;
         this.cipher = DscUtils.getAESCipher(key, mode);

         assert this.cipher.getBlockSize() == BLOCK_SIZE;
         assert this.buffer.length >= BLOCK_SIZE;
      }
   }

   // Esegue cifratura/decifratura su ByteBuf, gestendo padding e blocchi
   void process(ByteBuf in, ByteBuf out) {
      try {
         int padding = (BLOCK_SIZE - in.readableBytes() % BLOCK_SIZE) % BLOCK_SIZE;

         assert 0 <= padding && padding < BLOCK_SIZE;
         assert (in.readableBytes() + padding) % BLOCK_SIZE == 0;

         // In decifratura, la lunghezza deve essere multiplo del blocco
         if (this.mode == DECRYPT_MODE && padding != 0) {
               throw new CorruptedFrameException(String.format("length must be multiple of %d bytes", BLOCK_SIZE));
         } else {
               ByteBufInputStream bis = new ByteBufInputStream(in);
               Throwable bisEx = null;

               try {
                  CipherOutputStream cos = new CipherOutputStream(new ByteBufOutputStream(out), this.cipher);
                  Throwable cosEx = null;

                  try {
                     int read;
                     while ((read = bis.read(this.buffer)) != -1) {
                           cos.write(this.buffer, 0, read);
                     }
                     // Padding solo in cifratura
                     cos.write(this.buffer, 0, padding);
                  } catch (Throwable t) {
                     cosEx = t;
                     throw t;
                  } finally {
                     if (cos != null) {
                           if (cosEx != null) {
                              try { cos.close(); } catch (Throwable t2) { cosEx.addSuppressed(t2); }
                           } else {
                              cos.close();
                           }
                     }
                  }
               } catch (Throwable t) {
                  bisEx = t;
                  throw t;
               } finally {
                  if (bis != null) {
                     if (bisEx != null) {
                           try { bis.close(); } catch (Throwable t2) { bisEx.addSuppressed(t2); }
                     } else {
                           bis.close();
                     }
                  }
               }
         }
      } catch (IOException e) {
         throw new RuntimeException("unexpected exception", e);
      }
   }
}
