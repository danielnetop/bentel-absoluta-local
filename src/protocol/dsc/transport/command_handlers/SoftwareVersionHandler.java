package protocol.dsc.transport.command_handlers;

import io.netty.channel.Channel;

import protocol.dsc.commands.SoftwareVersion;
import protocol.dsc.session.SessionInfo;

import java.util.logging.Logger;

public class SoftwareVersionHandler extends HandshakeHandler<SoftwareVersion> {
   private static final Logger logger = Logger.getLogger(SoftwareVersionHandler.class.getName());

   public SoftwareVersionHandler() {
      super(SoftwareVersion.class);
   }

   // Valida che i campi versione software siano presenti nelle info locali
   public boolean validateOwnInfo(SessionInfo var1) {
      return var1.getSoftwareVersionFields() != null;
   }

   // Costruisce il comando SoftwareVersion con i campi versione locali
   protected SoftwareVersion getCommand(Channel var1) {
      String var2 = SessionInfo.getOwnInfo(var1).getSoftwareVersionFields();
      SoftwareVersion var3 = new SoftwareVersion();
      var3.setVersionFields(var2);
      return var3;
   }

   // Aggiorna le info peer con la versione ricevuta e logga
   protected int commandReceived(Channel var1, SoftwareVersion var2) {
      String var3 = var2.getVersionFields();
      SessionInfo.getPeerInfo(var1).setIdentifierOrInitKey(var3);
      logger.fine("Peer software version fields:" + var3);
      return 0;
   }
}
