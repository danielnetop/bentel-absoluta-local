package protocol.dsc.transport.command_handlers;

import com.google.common.base.Preconditions;

import io.netty.handler.codec.DecoderException;

import protocol.dsc.base.DscVariableBytes;
import protocol.dsc.commands.RequestAccess;
import protocol.dsc.util.DscUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import java.util.logging.Logger;

public class RequestAccessAESHelper {
   private static final Logger logger = Logger.getLogger(RequestAccessAESHelper.class.getName());
   public static final int INIT_KEY_LENGTH = 8; // lunghezza chiave inizializzazione (in caratteri esadecimali)
   private static final int SESSION_KEY_LENGTH = 16; // lunghezza chiave di sessione AES (byte)
   private static final int IDENTIFIER_TOTAL_LENGTH = 48; // lunghezza totale campo identifier
   private final DscVariableBytes identifierField;
   private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

   public RequestAccessAESHelper(RequestAccess requestAccess) {
      this.identifierField = requestAccess.identifier();
      assert this.identifierField != null;
   }

   // Cifra la chiave di sessione e la inserisce nell'identifier (usando chiave di inizializzazione)
   public void encryptKey(String initKeyHex, byte[] sessionKey) {
      this.encryptKey(initKeyHex, sessionKey, getRandomBytes());
   }

   // Cifra la chiave di sessione usando un vettore casuale (nonce)
   public void encryptKey(String initKeyHex, byte[] sessionKey, byte[] randomVector) {
      Preconditions.checkArgument(sessionKey.length == SESSION_KEY_LENGTH);
      Preconditions.checkArgument(randomVector.length == SESSION_KEY_LENGTH);
      this.identifierField.setLength(IDENTIFIER_TOTAL_LENGTH);
      byte[] identifierBytes = this.identifierField.bytes();

      // Inserisce randomVector e sessionKey in identifierBytes secondo protocollo
      for(int i = 0; i < SESSION_KEY_LENGTH; ++i) {
         identifierBytes[i] = randomVector[i];
         identifierBytes[SESSION_KEY_LENGTH + 2 * i] = randomVector[i];
         identifierBytes[SESSION_KEY_LENGTH + 2 * i + 1] = sessionKey[i];
      }

      Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, initKeyHex);

      try {
         // Cifra solo la parte finale dell'identifier (32 byte)
         cipher.doFinal(identifierBytes, SESSION_KEY_LENGTH, 32, identifierBytes, SESSION_KEY_LENGTH);
      } catch (IllegalBlockSizeException | BadPaddingException | ShortBufferException ex) {
         throw new RuntimeException("Eccezione inattesa durante cifratura", ex);
      }
   }

   // Decifra la chiave di sessione dall'identifier usando la chiave di inizializzazione
   public byte[] decryptKey(String initKeyHex) {
      try {
         if (this.identifierField.length() != IDENTIFIER_TOTAL_LENGTH) {
            throw new DecoderException(String.format("lunghezza identifier inattesa (%d invece di %d)", this.identifierField.length(), IDENTIFIER_TOTAL_LENGTH));
         }
         byte[] identifierBytes = this.identifierField.bytes();
         Cipher cipher = getCipher(Cipher.DECRYPT_MODE, initKeyHex);
         cipher.doFinal(identifierBytes, SESSION_KEY_LENGTH, 32, identifierBytes, SESSION_KEY_LENGTH);

         byte[] sessionKey = new byte[SESSION_KEY_LENGTH];
         // Verifica coerenza e ricostruisce la chiave di sessione
         for(int i = 0; i < SESSION_KEY_LENGTH; ++i) {
            byte randomByte = identifierBytes[i];
            byte randomByteCheck = identifierBytes[SESSION_KEY_LENGTH + 2 * i];
            if (randomByte != randomByteCheck) {
               throw new DecoderException(String.format("byte inatteso (0x%02X invece di 0x%02X)", randomByteCheck, randomByte));
            }
            sessionKey[i] = identifierBytes[SESSION_KEY_LENGTH + 2 * i + 1];
         }
         return sessionKey;
      } catch (RuntimeException ex) {
         logger.severe("Richiesta di accesso non valida: " + ex);
         return null;
      } catch (IllegalBlockSizeException | BadPaddingException | ShortBufferException ex) {
         throw new RuntimeException("Eccezione inattesa durante decifratura", ex);
      }
   }

   // Costruisce la chiave AES a partire dalla chiave di inizializzazione esadecimale
   private static Cipher getCipher(int cipherMode, String initKeyHex) {
      long initKeyLong;
      try {
         initKeyLong = DscUtils.validateUInt(Long.parseLong(initKeyHex.substring(0, 8), 16));
      } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
         throw new IllegalArgumentException(String.format("init key non valida '%s': %s", initKeyHex, ex.getMessage()), ex);
      }

      byte[] aesKey = new byte[16];
      // Espande la chiave a 16 byte secondo schema protocollo
      for(int i = 0; i < 4; ++i) {
         byte keyByte = (byte)((int)(initKeyLong >> 8 * (3 - i)));
         for(int j = 0; j < 4; ++j) {
            aesKey[4 * j + i] = keyByte;
         }
      }
      return DscUtils.getAESCipher(aesKey, cipherMode);
   }

   // Genera 16 byte casuali (per chiave di sessione)
   public static byte[] getRandomBytes() {
      byte[] bytes = new byte[16];
      SECURE_RANDOM.nextBytes(bytes);
      return bytes;
   }
}