package absoluta;

import absoluta.connection.ConnectionHandler;
import absoluta.connection.ConnectionThread;
import absoluta.connection.PanelStatus;
import absoluta.spi.PanelProvider;

import java.util.logging.Logger;

public class AbsolutaPanelProvider implements PanelProvider {
   private static final Logger logger = Logger.getLogger(AbsolutaPanelProvider.class.getName());
   private final String address;
   private final int port;
   private final String pin;
   private final PanelStatus panelStatus;
   private PanelCallback callback;
   private ConnectionHandler connectionHandler;

   public AbsolutaPanelProvider(String address, String pin, String port) {
      this.address = address;
      this.port = Integer.parseInt(port);
      this.pin = pin;
      this.panelStatus = new PanelStatus();
   }

   @Override
   public void initialize(PanelCallback callback) {
      this.callback = callback;
      // Listener per notificare i cambiamenti di stato al callback
      this.panelStatus.addPropertyChangeListener(new CallbackListener(callback, this.panelStatus));
   }

   @Override
   public providerConnStatus connect() {
      this.connectionHandler = new ConnectionHandler(this.panelStatus, this.callback);
      if (!this.connectionHandler.setPin(this.pin)) {
         return providerConnStatus.UNAUTHORIZED;
      } else {
         // Avvia il thread di connessione TCP/IP
         Thread connThread = new ConnectionThread(this.address, this.port, this.connectionHandler);
         connThread.setName("Absoluta-ConnectionThread");
         connThread.start();

         try {
               // Attende l'esito della connessione
               return this.connectionHandler.waitConnection();
         } catch (InterruptedException ex) {
               logger.severe("Connessione interrotta: " + ex.getMessage());
               Thread.currentThread().interrupt();
               return providerConnStatus.UNREACHABLE;
         }
      }
   }

   @Override
   public void disconnect() {
      this.connectionHandler.disconnect();
   }

   @Override
   public void arming(PanelStatus.GlobalArming armingMode) {
      this.connectionHandler.getCommander().arming(armingMode);
   }

   @Override
   public void PartitionArming(int partitionId, PanelStatus.PartitionArming armingMode) {
      this.connectionHandler.getCommander().partitionArming(partitionId, armingMode);
   }

   @Override
   public boolean armingSupport(char presetMode) {
      return this.connectionHandler.getCommander().armingSupport(presetMode);
   }

   @Override
   public void armingSet(char presetMode) {
      this.connectionHandler.getCommander().armingSet(presetMode);
   }

   @Override
   public void setBypassed(int zoneId, boolean setBypassed) {
      this.connectionHandler.getCommander().setBypassed(zoneId, setBypassed);
   }

   @Override
   public boolean getBypassed(int zoneId) {
      return this.panelStatus.getZoneBypass(zoneId);
   }

   @Override
   public void doOutputAction(int outputId, PanelStatus.OutputAction action) {
      switch (action) {
         case DO_CLOSE:
               this.connectionHandler.getCommander().setOutput(outputId, true);
               break;
         case DO_OPEN:
               this.connectionHandler.getCommander().setOutput(outputId, false);
               break;
         default:
               break;
      }
   }

   // Pulisce le segnalazioni di guasto
   void cleanTroubles() {
      this.connectionHandler.getCommander().cleanTroubles();
   }

   public enum providerConnStatus {
      USER_DISCONNECTED,
      SUCCESS,
      INCOMPATIBLE,
      UNAUTHORIZED,
      UNREACHABLE
   }
}