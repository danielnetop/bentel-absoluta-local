package protocol.dsc.transport.command_handlers;

import io.netty.channel.Channel;
import protocol.dsc.commands.OpenSession;
import protocol.dsc.session.SessionInfo;

public class OpenSessionHandler extends HandshakeHandler<OpenSession> {

   public OpenSessionHandler() {
      super(OpenSession.class);
   }

   // Verifica la validità delle informazioni locali necessarie per aprire una sessione.
   public boolean validateOwnInfo(SessionInfo sessionInfo) {
      return sessionInfo.getDeviceTypeOrVendorID() != null
            && sessionInfo.getDeviceId() != null
            && sessionInfo.getSoftwareVersion() != null
            && sessionInfo.getProtocolVersion() != null
            && sessionInfo.getTxSize() != null
            && sessionInfo.getRxSize() != null
            && (sessionInfo.getEncryptionType() != null || !Boolean.TRUE.equals(sessionInfo.isClient()));
   }

   // Costruisce il comando OpenSession da inviare, popolando tutti i campi richiesti.
   protected OpenSession getCommand(Channel channel) {
      SessionInfo ownInfo = SessionInfo.getOwnInfo(channel);
      OpenSession openSessionCmd = new OpenSession();
      openSessionCmd.setDeviceTypeOrVendorID(ownInfo.getDeviceTypeOrVendorID());
      openSessionCmd.setDeviceId(ownInfo.getDeviceId());
      openSessionCmd.setSoftwareVersion(ownInfo.getSoftwareVersion());
      openSessionCmd.setProtocolVersion(ownInfo.getProtocolVersion());
      openSessionCmd.setTxSize(ownInfo.getTxSize());
      openSessionCmd.setRxSize(ownInfo.getRxSize());
      openSessionCmd.setEncriptionType(ownInfo.getEncryptionType());
      return openSessionCmd;
   }

   protected int commandReceived(Channel channel, OpenSession receivedSession) {
      SessionInfo peerInfo = SessionInfo.getPeerInfo(channel);
      peerInfo.setDeviceTypeOrVendorID(receivedSession.getDeviceTypeOrVendorID());
      peerInfo.setDeviceId(receivedSession.getDeviceId());
      peerInfo.setSoftwareVersion(receivedSession.getSoftwareVersion());
      peerInfo.setProtocolVersion(receivedSession.getProtocolVersion());
      peerInfo.setTxSize(receivedSession.getTxSize());
      peerInfo.setRxSize(receivedSession.getRxSize());
      peerInfo.setEncryptionType(receivedSession.getEncryptionType());

      // Accetta solo cifratura 0 (nessuna) o 1 (AES)
      if (peerInfo.getEncryptionType() != 0 && peerInfo.getEncryptionType() != 1) {
         return 3;
      } else {
         SessionInfo ownInfo = SessionInfo.getOwnInfo(channel);
         if (ownInfo.getEncryptionType() == null) {
            ownInfo.setEncryptionType(peerInfo.getEncryptionType());
         } else if (!ownInfo.getEncryptionType().equals(peerInfo.getEncryptionType())) {
            return 3; // Incompatibilità di cifratura
         }
         return 0; // Tutto ok
      }
   }
}
