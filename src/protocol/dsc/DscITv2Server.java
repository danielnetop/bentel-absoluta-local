package protocol.dsc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import protocol.dsc.session.DscChannelInitializer;
import protocol.dsc.session.DscEndpoint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DscITv2Server implements DscServer {
   private final int port;
   private final List<IncomingConnectionListener> incomingConnectionListeners = new CopyOnWriteArrayList<>();
   private EventLoopGroup bossEventLoopGroup;
   private EventLoopGroup workerEventLoopGroup;

   public DscITv2Server(int port) {
      this.port = port;
   }

   @SuppressWarnings("deprecation")
   public void start() {
      // Avvia solo se non già avviato
      if (this.bossEventLoopGroup == null && this.workerEventLoopGroup == null) {
         try {
               this.bossEventLoopGroup = new NioEventLoopGroup();
               this.workerEventLoopGroup = new NioEventLoopGroup();
               ServerBootstrap serverBootstrap = new ServerBootstrap();
               serverBootstrap.group(this.bossEventLoopGroup, this.workerEventLoopGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new DscChannelInitializer() {
                           @Override
                           protected void onInitialized(DscEndpoint endpoint, SocketChannel channel) {
                              // Notifica tutti i listener della nuova connessione
                              for (IncomingConnectionListener listener : incomingConnectionListeners) {
                                 listener.deviceConnected(endpoint);
                              }
                           }
                     })
                     .option(ChannelOption.SO_BACKLOG, 128)
                     .childOption(ChannelOption.SO_KEEPALIVE, true);
               serverBootstrap.bind(this.port);
         } catch (RuntimeException ex) {
               try {
                  this.stop(false);
               } catch (InterruptedException ie) {
                  throw new AssertionError(ie);
               }
               throw ex;
         }
      } else {
         throw new IllegalStateException("Server is already running");
      }
   }

   public void stop() throws InterruptedException {
      this.stop(true);
   }

   // Gestisce la chiusura dei gruppi di thread, opzionalmente attende la terminazione
   private void stop(boolean wait) throws InterruptedException {
      Future<?> workerShutdownFuture = null;
      Future<?> bossShutdownFuture = null;
      if (this.workerEventLoopGroup != null) {
         workerShutdownFuture = this.workerEventLoopGroup.shutdownGracefully();
         this.workerEventLoopGroup = null;
      }
      if (this.bossEventLoopGroup != null) {
         bossShutdownFuture = this.bossEventLoopGroup.shutdownGracefully();
         this.bossEventLoopGroup = null;
      }
      if (wait) {
         if (workerShutdownFuture != null) {
               workerShutdownFuture.sync();
         }
         if (bossShutdownFuture != null) {
               bossShutdownFuture.sync();
         }
      }
   }

   public void addIncomingConnectionListener(IncomingConnectionListener listener) {
      this.incomingConnectionListeners.add(listener);
   }

   public void removeIncomingConnectionListener(IncomingConnectionListener listener) {
      this.incomingConnectionListeners.remove(listener);
   }
}
