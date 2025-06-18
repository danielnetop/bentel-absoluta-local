package protocol.dsc;

import com.google.common.base.Preconditions;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import protocol.dsc.commands.DscCommand;
import protocol.dsc.commands.UserActivity;
import protocol.dsc.session.DscChannelInitializer;
import protocol.dsc.session.DscEndpoint;
import protocol.dsc.session.SessionInfo;
import protocol.dsc.transport.command_handlers.HandshakeHandler;
import protocol.dsc.transport.command_handlers.OpenSessionHandler;
import protocol.dsc.transport.command_handlers.PollHandler;
import protocol.dsc.transport.command_handlers.RequestAccessHandler;
import protocol.dsc.transport.command_handlers.SoftwareVersionHandler;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

public class AbsolutaClient implements ITv2Client {
   private static final Logger logger = Logger.getLogger(AbsolutaClient.class.getName());
   private final String host;
   private final int port;

   public AbsolutaClient(String host, int port) {
      this.host = Preconditions.checkNotNull(host);
      this.port = port;
   }

   @SuppressWarnings("deprecation")
   public void connect(ITv2Client.Callback callback) throws InterruptedException {
      Preconditions.checkNotNull(callback);
      InetSocketAddress remoteAddress = new InetSocketAddress(this.host, this.port);
      logger.info("Connecting to " + remoteAddress);
      NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();

      try {
         Bootstrap bootstrap = new Bootstrap();
         bootstrap.group(eventLoopGroup);
         bootstrap.channel(NioSocketChannel.class);
         bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
         bootstrap.handler(new AbsolutaChannelInitializer(callback));
         ChannelFuture connectFuture = bootstrap.connect(remoteAddress).sync();
         Channel channel = connectFuture.channel();
         // Blocco fino a chiusura canale (gestione sincrona)
         channel.closeFuture().sync();
      } finally {
         logger.info("Disconnected from " + remoteAddress);
         eventLoopGroup.shutdownGracefully();
      }
   }

   // Inizializzatore canale custom per Absoluta
   private class AbsolutaChannelInitializer extends DscChannelInitializer {
      private final ITv2Client.Callback callback;

      AbsolutaChannelInitializer(ITv2Client.Callback callback) {
         this.callback = callback;
      }

      // Costruisce le info di sessione da inviare al server
      @Override
      protected SessionInfo buildOwnSessionInfo() {
         SessionInfo sessionInfo = new SessionInfo();
         sessionInfo.setClient(true);
         sessionInfo.setDeviceTypeOrVendorID(143); // 0x8F
         sessionInfo.setDeviceId(0);
         sessionInfo.setSoftwareVersion("0100");
         sessionInfo.setProtocolVersion("0203");
         sessionInfo.setTxSize(50);
         sessionInfo.setRxSize(1024);
         sessionInfo.setEncryptionType(0);
         sessionInfo.setIdentifierOrInitKey("00000000");
         // Campi software version custom (probabilmente richiesti dal protocollo)
         sessionInfo.setSoftwareVersionFields("35 00 00 1E 02 03 00 00 01 03 01");
         return sessionInfo;
      }

      // Sequenza di handshake da eseguire all'avvio della sessione
      @Override
      protected Queue<HandshakeHandler<?>> buildHandshakeHandlers() {
         Queue<HandshakeHandler<?>> handshakeHandlers = new ArrayDeque<>(3);
         handshakeHandlers.add(new OpenSessionHandler());
         handshakeHandlers.add(new RequestAccessHandler());
         handshakeHandlers.add(new SoftwareVersionHandler());
         return handshakeHandlers;
      }

      // Timeout scrittura (idle) per il canale
      @Override
      protected int getWriterIdleTimeSeconds() {
         return 5;
      }

      // Factory per la generazione dei poll (keepalive)
      @Override
      protected PollHandler.PollFactory pollFactory() {
         return new PollHandler.PollFactory() {
               @Override
               public DscCommand createPoll() {
                  UserActivity userActivity = new UserActivity();
                  userActivity.setPartitionNumber(null); // Nessuna partizione specifica
                  userActivity.setType(4); // Tipo 4: keepalive/user activity
                  return userActivity;
               }
         };
      }

      // Callback chiamata quando il canale è pronto e autenticato
      @Override
      protected void onInitialized(DscEndpoint endpoint, SocketChannel channel) {
         this.callback.connected(endpoint);
      }
   }
}