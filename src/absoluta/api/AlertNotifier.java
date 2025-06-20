package absoluta.api;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

public final class AlertNotifier {
   private static final Logger logger = Logger.getLogger(AlertNotifier.class.getName());
   // Singleton per la gestione centralizzata delle notifiche di allarme
   private static final AlertNotifier INSTANCE = new AlertNotifier();

   // Lista thread-safe di listener registrati
   private final List<AlertListener> listeners = new CopyOnWriteArrayList<>();

   public static AlertNotifier getDefault() {
      return INSTANCE;
   }

   //Notifica tutti i listener di un nuovo evento di allarme.
   void fire(Object source, String message) {
      final AlertEvent alertEvent = new AlertEvent(
         Preconditions.checkNotNull(message),
         Preconditions.checkNotNull(source)
      );
      logger.finer("Firing new event: " + alertEvent);

      // Esegue la notifica dei listener
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
               for (AlertListener listener : AlertNotifier.this.listeners) {
                  try {
                     listener.alertEventReceived(alertEvent);
                  } catch (RuntimeException ex) {
                     logger.severe("Exception in event listener: " + ex);
                  }
               }
         }
      });
   }
}