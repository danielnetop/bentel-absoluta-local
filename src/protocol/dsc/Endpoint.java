package protocol.dsc;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ScheduledExecutorService;

public interface Endpoint {

   String getPanelId();

   String getPin();

   void setPin(String pin);

   boolean isSessionActive();

   void setSessionActive(boolean active);

   DscEndpointState getState();

   void addPropertyChangeListener(PropertyChangeListener listener);

   void removePropertyChangeListener(PropertyChangeListener listener);

   void close();

   Messenger getMessenger();

   ScheduledExecutorService getExecutor();
}