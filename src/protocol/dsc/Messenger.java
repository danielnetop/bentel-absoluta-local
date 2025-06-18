package protocol.dsc;

public interface Messenger {
   <V> void send(Message<Void, V> message);

   <V> void send(Message<Void, V> message, Priority priority);

   <P, V> void send(Message<P, V> message, P payload);

   <P, V> void send(Message<P, V> message, P payload, Priority priority);

   void addMessageListener(MessageListener listener);

   void removeMessageListener(MessageListener listener);
}