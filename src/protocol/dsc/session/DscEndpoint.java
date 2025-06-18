package protocol.dsc.session;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import protocol.dsc.DscEndpointState;
import protocol.dsc.DscError;
import protocol.dsc.Endpoint;
import protocol.dsc.Message;
import protocol.dsc.MessageListener;
import protocol.dsc.Messenger;
import protocol.dsc.NewValue;
import protocol.dsc.Priority;
import protocol.dsc.commands.EndSession;
import protocol.dsc.transport.EndpointHandler;
import protocol.dsc.transport.command_handlers.PollHandler;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class DscEndpoint implements Endpoint, Messenger {
   private static final Logger logger = Logger.getLogger(DscEndpoint.class.getName());
   private final Channel channel;
   private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
   private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
   private String panelId;
   private String pin;
   private boolean sessionActive;
   private DscEndpointState state;

   public DscEndpoint(Channel channel) {
      this.channel = Preconditions.checkNotNull(channel);
   }

   public String getPanelId() {
      return this.panelId;
   }

   public void setPanelId(String newPanelId) {
      logger.fine("Imposto panel id: " + newPanelId);
      String oldPanelId = this.panelId;
      this.panelId = newPanelId;
      this.propertyChangeSupport.firePropertyChange("panelId", oldPanelId, newPanelId);
   }

   public String getPin() {
      return this.pin;
   }

   public void setPin(String newPin) {
      logger.fine("Imposto pin: " + newPin);
      String oldPin = this.pin;
      this.pin = newPin;
      EndpointHandler.setPin(this.channel, newPin);
      this.propertyChangeSupport.firePropertyChange("pin", oldPin, newPin);
   }

   public boolean isSessionActive() {
      return this.sessionActive;
   }

   public void setSessionActive(boolean sessionActive) {
      logger.fine("Imposto sessionActive: " + sessionActive);
      boolean oldSessionActive = this.sessionActive;
      this.sessionActive = sessionActive;
      this.updatePoller();
      this.propertyChangeSupport.firePropertyChange("sessionActive", oldSessionActive, sessionActive);
   }

   public DscEndpointState getState() {
      return this.state;
   }

   public void setState(DscEndpointState newState) {
      DscEndpointState oldState = this.state;
      if (oldState == DscEndpointState.CLOSED) {
         logger.warning("Lo stato attuale è CLOSED: ignoro la richiesta di cambio stato a " + newState);
      } else {
         logger.info("Imposto stato: " + newState);
         this.state = newState;
         this.updatePoller();
         this.propertyChangeSupport.firePropertyChange("state", oldState, newState);
      }
   }

   public void addPropertyChangeListener(PropertyChangeListener listener) {
      this.propertyChangeSupport.addPropertyChangeListener(listener);
   }

   public void removePropertyChangeListener(PropertyChangeListener listener) {
      this.propertyChangeSupport.removePropertyChangeListener(listener);
   }

   // Chiude la sessione o il canale a seconda dello stato
   public void close() {
      if (this.state != DscEndpointState.CLOSING && this.state != DscEndpointState.CLOSED) {
         if (this.channel.isActive()) {
               logger.fine("Chiusura endpoint: invio EndSession");
               EndSession endSession = new EndSession();
               endSession.setPriority(Priority.HIGH);
               this.channel.write(endSession);
         } else {
               logger.fine("Chiusura endpoint: chiudo il canale");
               this.channel.close();
         }
      }
   }

   public Messenger getMessenger() {
      return this;
   }

   public ScheduledExecutorService getExecutor() {
      return this.channel.eventLoop();
   }

   public <V> void send(Message<Void, V> message) {
      this.send(message, Priority.NORMAL);
   }

   public <V> void send(Message<Void, V> message, Priority priority) {
      this.send(message, null, priority);
   }

   public <P, V> void send(Message<P, V> message, P param) {
      this.send(message, param, Priority.NORMAL);
   }

   // Invio asincrono di un messaggio sulla channel Netty
   public <P, V> void send(final Message<P, V> message, final P param, Priority priority) {
      Preconditions.checkNotNull(message);
      Preconditions.checkNotNull(priority);
      SendingMessage<P, V> sendingMessage = new SendingMessage<>(message, param, priority);
      logger.fine("Invio: " + sendingMessage);
      this.channel.write(sendingMessage).addListener(new ChannelFutureListener() {
         @Override
         public void operationComplete(ChannelFuture future) throws Exception {
               Throwable cause = future.cause();
               if (cause != null) {
                  DscEndpoint.this.broadcastError(DscError.newMessageError(message, param, cause));
               }
         }
      });
   }

   public void addMessageListener(MessageListener listener) {
      this.messageListeners.add(listener);
   }

   public void removeMessageListener(MessageListener listener) {
      this.messageListeners.remove(listener);
   }

   public void broadcastNewValue(NewValue value) {
      logger.fine("Nuovo valore ricevuto: " + value);
      for (MessageListener listener : this.messageListeners) {
         listener.newValue(value);
      }
   }

   public void broadcastError(DscError error) {
      logger.fine("Errore ricevuto: " + error);
      for (MessageListener listener : this.messageListeners) {
         listener.error(error);
      }
   }

   // Abilita/disabilita il poller in base allo stato della sessione
   private void updatePoller() {
      PollHandler.setPollEnabled(this.channel, this.sessionActive && this.state == DscEndpointState.READY);
   }
}