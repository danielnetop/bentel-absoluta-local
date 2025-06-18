package protocol.dsc.transport.command_handlers;

import io.netty.channel.Channel;

import protocol.dsc.commands.RequestAccess;
import protocol.dsc.session.SessionInfo;
import protocol.dsc.transport.AESDecoder;
import protocol.dsc.transport.AESEncoder;
import protocol.dsc.util.DscUtils;

import java.util.logging.Logger;

public class RequestAccessHandler extends HandshakeHandler<RequestAccess> {
   private static final Logger logger = Logger.getLogger(RequestAccessHandler.class.getName());
   private byte[] sessionKey; // chiave di sessione generata per AES

   public RequestAccessHandler() {
      super(RequestAccess.class);
   }

   // Verifica la validità delle informazioni locali in base al tipo di cifratura
   public boolean validateOwnInfo(SessionInfo sessionInfo) {
      String identifierOrInitKey = sessionInfo.getIdentifierOrInitKey();
      switch(sessionInfo.getEncryptionType()) {
         case 0: // Nessuna cifratura
            return identifierOrInitKey != null;
         case 1: // Cifratura AES
            return identifierOrInitKey != null && identifierOrInitKey.length() >= 8;
         default:
            return false;
      }
   }

   // Costruisce il comando RequestAccess da inviare, gestendo la cifratura se necessario
   protected RequestAccess getCommand(Channel channel) {
      SessionInfo ownInfo = SessionInfo.getOwnInfo(channel);
      String identifierOrInitKey = ownInfo.getIdentifierOrInitKey();
      RequestAccess requestAccess = new RequestAccess();
      switch(ownInfo.getEncryptionType()) {
         case 0:
            this.sessionKey = null;
            requestAccess.setIdentifier(identifierOrInitKey);
            break;
         case 1:
            this.sessionKey = RequestAccessAESHelper.getRandomBytes();
            // Cifra la chiave di sessione con la chiave di inizializzazione
            (new RequestAccessAESHelper(requestAccess)).encryptKey(identifierOrInitKey, this.sessionKey);
            break;
         default:
            throw new IllegalStateException("Tipo di cifratura non previsto");
      }

      return requestAccess;
   }

   // Imposta la chiave di decodifica AES dopo l'invio del comando
   protected void commandSent(Channel channel) {
      if (this.sessionKey != null) {
         logger.fine("Decoding key: " + DscUtils.hexDump(this.sessionKey));
         AESDecoder.setKey(channel, this.sessionKey);
      }
   }

   // Gestisce la ricezione del comando RequestAccess, impostando la chiave di cifratura se necessario
   protected int commandReceived(Channel channel, RequestAccess requestAccess) {
      SessionInfo peerInfo = SessionInfo.getPeerInfo(channel);
      switch(peerInfo.getEncryptionType()) {
         case 0:
            String peerIdentifier = requestAccess.getIdentifier();
            peerInfo.setIdentifierOrInitKey(peerIdentifier);
            logger.fine("Peer identifier: " + peerIdentifier);
            return 0;
         case 1:
            String initKey = peerInfo.getIdentifierOrInitKey();
            if (initKey == null) {
               // Recupera l'ID multipunto se la chiave non è ancora impostata
               initKey = peerInfo.getMultiPointCommId();
               peerInfo.setIdentifierOrInitKey(initKey);
            }

            // Decifra la chiave di sessione ricevuta
            byte[] decodedSessionKey = (new RequestAccessAESHelper(requestAccess)).decryptKey(initKey);
            if (decodedSessionKey != null) {
               logger.fine("Encoding key: " + DscUtils.hexDump(decodedSessionKey));
               AESEncoder.setKey(channel, decodedSessionKey);
               return 0;
            }

            logger.warning("Richiesta di accesso non valida ricevuta");
            return 1;
         default:
            logger.warning("Tipo di cifratura non previsto");
            return 1;
      }
   }
}
