package protocol.dsc.base;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import protocol.dsc.util.DscUtils;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;

public final class DscVariableBytes implements DscSerializable {
   private static final BigInteger MAX_INT = BigInteger.valueOf(2147483647L);
   private byte[] bytes = DscUtils.emptyByteArray();

   public int length() {
      return this.bytes.length;
   }

   public void setLength(int newLength) {
      if (newLength != this.bytes.length) {
         this.bytes = new byte[DscUtils.validateUByte(newLength)];
      }
   }

   public byte[] bytes() {
      return this.bytes;
   }

   public String toString(Charset charset) {
      return new String(this.bytes, charset);
   }

   public DscVariableBytes setString(Charset charset, String value) {
      byte[] encoded = value.getBytes(charset);
      DscUtils.validateUByte(encoded.length);
      this.bytes = encoded;
      return this;
   }

   public BigInteger toUnsignedBigInteger() {
      return new BigInteger(1, this.bytes);
   }

   public DscVariableBytes setUnsignedBigInteger(BigInteger value) {
      this.bytes = toByteArray(value);
      return this;
   }

   public DscVariableBytes setUnsignedBigInteger(BigInteger value, int length) {
      byte[] arr = toByteArray(value);
      if (arr.length == length) {
         this.bytes = arr;
      } else if (arr.length < length) {
         this.setLength(length);
         // Allineamento a destra, padding a sinistra con zeri
         System.arraycopy(arr, 0, this.bytes, length - arr.length, arr.length);
      } else {
         if (length != 0 || !BigInteger.ZERO.equals(value)) {
               throw new IllegalArgumentException("value too big for the given length");
         }
         this.setLength(0);
      }
      return this;
   }

   // Rimuove eventuale byte di segno superfluo in BigInteger
   private static byte[] toByteArray(BigInteger value) {
      if (value.signum() < 0) {
         throw new IllegalArgumentException("unexpected negative value");
      }
      byte[] arr = value.toByteArray();
      return arr.length > 1 && arr[0] == 0 ? Arrays.copyOfRange(arr, 1, arr.length) : arr;
   }

   public int toPositiveInt() {
      BigInteger val = this.toUnsignedBigInteger();
      assert val.signum() >= 0;
      if (val.compareTo(MAX_INT) <= 0) {
         return val.intValue();
      } else {
         throw new IllegalStateException("overflow");
      }
   }

   public Integer toPositiveInteger() {
      return this.length() == 0 ? null : this.toPositiveInt();
   }

   public DscVariableBytes setPositiveInt(int value) {
      if (value >= 0) {
         this.setUnsignedBigInteger(BigInteger.valueOf((long) value));
         return this;
      } else {
         throw new IllegalArgumentException("unexpected negative value");
      }
   }

   public DscVariableBytes setPositiveInt(int value, int length) {
      if (value >= 0) {
         this.setUnsignedBigInteger(BigInteger.valueOf((long) value), length);
         return this;
      } else {
         throw new IllegalArgumentException("unexpected negative value");
      }
   }

   public DscVariableBytes setPositiveInteger(Integer value) {
      if (value == null) {
         this.setLength(0);
      } else {
         this.setPositiveInt(value);
      }
      return this;
   }

   public String toString() {
      return DscUtils.hexDump(this.bytes); // Rappresentazione esadecimale per debug/log
   }

   @Override
   public boolean equals(Object obj) {
      if (obj != null && this.getClass() == obj.getClass()) {
         DscVariableBytes other = (DscVariableBytes) obj;
         return Arrays.equals(this.bytes, other.bytes);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.bytes);
   }

   @Override
   public void readFrom(ByteBuf buffer) throws IndexOutOfBoundsException, DecoderException {
      this.setLength(buffer.readUnsignedByte()); // Primo byte: lunghezza
      buffer.readBytes(this.bytes);
   }

   @Override
   public void writeTo(ByteBuf buffer) {
      buffer.writeByte(this.bytes.length); // Scrive la lunghezza come primo byte
      buffer.writeBytes(this.bytes);
   }

   @Override
   public boolean isEquivalent(DscSerializable other) {
      // Ignora eventuali zeri di padding a sinistra
      if (other instanceof DscVariableBytes) {
         byte[] otherBytes = ((DscVariableBytes) other).bytes;
         int minLen = Math.min(this.bytes.length, otherBytes.length);

         // Confronto byte a byte partendo dalla fine (LSB)
         for (int i = 1; i <= minLen; ++i) {
               if (this.bytes[this.bytes.length - i] != otherBytes[otherBytes.length - i]) {
                  return false;
               }
         }
         // Verifica che il padding a sinistra sia zero
         for (int i = 0; i < this.bytes.length - minLen; ++i) {
               if (this.bytes[i] != 0) {
                  return false;
               }
         }
         for (int i = 0; i < otherBytes.length - minLen; ++i) {
               if (otherBytes[i] != 0) {
                  return false;
               }
         }
         return true;
      } else {
         return false;
      }
   }
}