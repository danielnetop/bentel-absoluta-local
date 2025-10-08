import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class PingKeepAlive implements Runnable {
    private static final Logger logger = Logger.getLogger(PingKeepAlive.class.getName());
    private final String host;
    private final int port;
    private final AtomicBoolean running;
    private final Thread thread;
    private static final int PING_INTERVAL = 2000; // 2 secondi
    private static final int PING_TIMEOUT = 1000; // 1 secondo di timeout per il ping

    public PingKeepAlive(String host, int port) {
        this.host = host;
        this.port = port;
        this.running = new AtomicBoolean(true);
        this.thread = new Thread(this, "TCP-Ping-Thread");
        this.thread.setDaemon(true); // Il thread si chiuderà automaticamente quando l'applicazione termina
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        running.set(false);
        thread.interrupt();
    }

    @Override
    public void run() {
        logger.info("Avvio thread di ping TCP verso " + host + ":" + port);
        
        while (running.get()) {
            try {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), PING_TIMEOUT);
                }
                Thread.sleep(PING_INTERVAL);
            } catch (IOException e) {
                logger.warning("Errore durante il ping TCP: " + e.getMessage());
            } catch (InterruptedException e) {
                if (running.get()) {
                    logger.warning("Thread di ping interrotto inaspettatamente");
                }
                break;
            }
        }
        
        logger.info("Thread di ping TCP terminato");
    }
}