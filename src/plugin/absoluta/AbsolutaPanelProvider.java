package plugin.absoluta;

import com.google.common.collect.ImmutableMap;

import cms.device.api.Panel;
import cms.device.api.Output.Action;
import cms.device.api.Panel.Arming;
import cms.device.spi.PanelProvider;
import plugin.absoluta.connection.ConnectionHandler;
import plugin.absoluta.connection.ConnectionThread;
import plugin.absoluta.connection.PanelStatus;

import java.util.Map;
import java.util.logging.Logger;

public class AbsolutaPanelProvider implements PanelProvider {
   private static final Logger logger = Logger.getLogger(AbsolutaPanelProvider.class.getName());
   private final String address;
   private final int port;
   private final String pin;
   private final PanelStatus panelStatus;
   private PanelCallback callback;
   private ConnectionHandler connectionHandler;

   public AbsolutaPanelProvider(Map<String, String> settings) {
      this.address = settings.get("address");
      this.port = Integer.parseInt(settings.getOrDefault("port", "3064"));
      this.pin = settings.get("pin");
      this.panelStatus = new PanelStatus();
   }

   @Override
   public Map<String, String> getSettings() {
      return ImmutableMap.<String, String>builder()
         .put("address", this.address)
         .put("port", Integer.toString(this.port))
         .put("pin", this.pin)
         .build();
   }

   @Override
   public void initialize(PanelCallback callback) {
      this.callback = callback;
      // Listener per notificare i cambiamenti di stato al callback
      this.panelStatus.addPropertyChangeListener(new CallbackListener(callback, this.panelStatus));
   }

   @Override
   public Panel.ConnStatus connect() {
      this.connectionHandler = new ConnectionHandler(this.panelStatus, this.callback);
      if (!this.connectionHandler.setPin(this.pin)) {
         return Panel.ConnStatus.UNAUTHORIZED;
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
               return Panel.ConnStatus.UNREACHABLE;
         }
      }
   }

   @Override
   public void disconnect() {
      this.connectionHandler.disconnect();
   }

   @Override
   public void arming(Arming armingMode) {
      this.connectionHandler.getCommander().arming(armingMode);
   }

   @Override
   public void partitionArming(String partitionId, cms.device.api.Partition.Arming armingMode) {
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
   public void setBypassed(String zoneId, boolean setBypassed) {
      this.connectionHandler.getCommander().setBypassed(zoneId, setBypassed);
   }

   @Override
   public boolean getBypassed(String zoneId) {
      int zoneInt = Integer.parseInt(zoneId);
      return this.panelStatus.getZoneBypass(zoneInt);
   }

   @Override
   public void doOutputAction(String outputId, Action action) {
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
}