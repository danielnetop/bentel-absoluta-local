package protocol.dsc;

public interface DscServer {
   void start();

   void stop() throws InterruptedException;

   void addIncomingConnectionListener(IncomingConnectionListener listener);

   void removeIncomingConnectionListener(IncomingConnectionListener listener);

}