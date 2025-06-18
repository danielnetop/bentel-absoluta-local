package protocol.dsc.base;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import protocol.dsc.util.DscUtils;

import java.nio.charset.Charset;
import java.util.Arrays;

public final class DscString implements DscSerializable {
   private final boolean fixedSize;
   private final Charset charset;
   private byte[] bytes;

   public DscString(int length, Charset charset) {
      Preconditions.checkArgument(length > 0);
      Preconditions.checkNotNull(charset);
      this.fixedSize = true;
      this.charset = charset;
      this.bytes = new byte[length];
   }

   public DscString(Charset charset) {
      Preconditions.checkNotNull(charset);
      this.fixedSize = false;
      this.charset = charset;
      this.bytes = DscUtils.emptyByteArray();
   }

   public int length() {
      return this.bytes.length;
   }

   public void setLength(int newLength) {
      if (this.fixedSize) {
         throw new UnsupportedOperationException("fixed size");
      }
      if (newLength != this.bytes.length) {
         this.bytes = new byte[DscUtils.validateUByte(newLength)];
      }
   }

   public boolean isFixedSize() {
      return this.fixedSize;
   }

   public Charset getCharset() {
      return this.charset;
   }

   public byte[] bytes() {
      return this.bytes;
   }

   /**
    * Imposta il valore della stringa, gestendo padding e taglio se necessario.
    */
   public DscString setString(String value) {
      byte[] encoded = value.getBytes(this.charset);
      if (!this.fixedSize) {
         this.bytes = encoded;
      } else {
         if (encoded.length > this.bytes.length) {
               throw new IllegalArgumentException(String.format("too long string: %s", value));
         }
         System.arraycopy(encoded, 0, this.bytes, 0, encoded.length);
         Arrays.fill(this.bytes, encoded.length, this.bytes.length, (byte) 0); // Padding con zeri
      }
      return this;
   }

   public String toString() {
      return new String(this.bytes, this.charset);
   }

   @Override
   public boolean equals(Object obj) {
      // Confronto sia dei byte che del charset
      if (obj != null && this.getClass() == obj.getClass()) {
         DscString other = (DscString) obj;
         return Arrays.equals(this.bytes, other.bytes) && this.charset.equals(other.charset);
      }
      return false;
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.bytes) ^ this.charset.hashCode();
   }

   @Override
   public void readFrom(ByteBuf buffer) throws IndexOutOfBoundsException, DecoderException {
      if (!this.fixedSize) {
         this.bytes = new byte[buffer.readableBytes()]; // Alloca in base ai byte disponibili
      }
      buffer.readBytes(this.bytes);
   }

   @Override
   public void writeTo(ByteBuf buffer) {
      buffer.writeBytes(this.bytes);
   }

   @Override
   public boolean isEquivalent(DscSerializable other) {
      // Equivalenza basata sul valore stringa, non sui byte
      if (other instanceof DscString) {
         DscString otherString = (DscString) other;
         return this.toString().equals(otherString.toString());
      }
      return false;
   }

   // Factory methods per istanze con charset predefiniti
   public static DscString newBCDString(int length) {
      return new DscString(length, DscCharsets.BCD);
   }

   public static DscString newBCDString() {
      return new DscString(DscCharsets.BCD);
   }

   public static DscString newUnicodeString(int length) {
      return new DscString(length, DscCharsets.UNICODE);
   }

   public static DscString newUnicodeString() {
      return new DscString(DscCharsets.UNICODE);
   }
}
