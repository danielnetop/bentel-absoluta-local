package protocol.dsc.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public final class DscUtils {
   private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

   public static int validateUByte(int value) throws IllegalArgumentException {
      if (value >= 0 && value <= 255) {
         return value;
      } else {
         throw new IllegalArgumentException(String.format("%d out of uint8 range", value));
      }
   }

   public static int validateUShort(int value) throws IllegalArgumentException {
      if (value >= 0 && value <= 65535) {
         return value;
      } else {
         throw new IllegalArgumentException(String.format("%d out of uint16 range", value));
      }
   }

   public static long validateUInt(long value) throws IllegalArgumentException {
      if (value >= 0L && value <= 4294967295L) {
         return value;
      } else {
         throw new IllegalArgumentException(String.format("%d out of uint32 range", value));
      }
   }

   public static Cipher getAESCipher(byte[] key, int mode) throws IllegalArgumentException {
      try {
         Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
         cipher.init(mode, new SecretKeySpec(key, "AES"));
         return cipher;
      } catch (InvalidKeyException ex) {
         throw new IllegalArgumentException("invalid key", ex);
      } catch (NoSuchPaddingException | NoSuchAlgorithmException ex) {
         throw new RuntimeException("unexpected exception", ex);
      }
   }

   public static String hexDump(byte[] data) {
      if (data == null) {
         return "null";
      } else if (data.length == 0) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder(data.length * 3);
         sb.append(String.format("%02X", data[0]));

         for(int i = 1; i < data.length; ++i) {
            sb.append(' ').append(String.format("%02X", data[i]));
         }

         return sb.toString();
      }
   }

   public static byte[] emptyByteArray() {
      return EMPTY_BYTE_ARRAY;
   }

   private DscUtils() { }
}