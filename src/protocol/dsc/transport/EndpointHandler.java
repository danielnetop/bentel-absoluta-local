package protocol.dsc.transport;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;

import protocol.dsc.DscEndpointState;
import protocol.dsc.DscError;
import protocol.dsc.NewValue;
import protocol.dsc.session.Consts;
import protocol.dsc.session.DscEndpoint;
import protocol.dsc.session.SendingMessage;
import protocol.dsc.session.SessionInfo;
import protocol.dsc.util.LogOnFailure;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class EndpointHandler extends ChannelDuplexHandler {
   private static final Logger logger = Logger.getLogger(EndpointHandler.class.getName());
   private static final AttributeKey<String> PIN_KEY = AttributeKey.valueOf("EndpointHandler.pin");
   private final DscEndpoint endpoint;
   private String panelId;

   public EndpointHandler(DscEndpoint endpoint) {
      this.endpoint = Preconditions.checkNotNull(endpoint);
   }

   // Blocca invio se endpoint non in stato READY
   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (msg instanceof SendingMessage && this.endpoint.getState() != DscEndpointState.READY) {
         promise.setFailure(new IllegalStateException("invalid state at the sending time"));
      } else {
         promise.addListener(LogOnFailure.INSTANCE);
         ctx.write(msg, promise);
      }
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      // Imposta panelId la prima volta che viene letto dal peer
      if (this.panelId == null) {
         this.panelId = SessionInfo.getPeerInfo(ctx.channel()).getMultiPointCommId();
         if (this.panelId != null) {
               this.endpoint.setPanelId(this.panelId);
         }
      }

      // Propaga nuovi valori o errori all'endpoint
      if (msg instanceof NewValue) {
         this.endpoint.broadcastNewValue((NewValue) msg);
      } else if (msg instanceof DscError) {
         this.endpoint.broadcastError((DscError) msg);
      } else {
         super.channelRead(ctx, msg);
      }
   }

   public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      // Gestione stato endpoint in base agli eventi handshake/chiusura
      if (evt == SimpleMessage.CLOSING_CHANNEL_EVENT) {
         this.closingChannel(ctx);
      } else if (evt == SimpleMessage.HANDSHAKE_BEGIN_EVENT) {
         this.endpoint.setState(DscEndpointState.HANDSHAKING);
      } else if (evt == SimpleMessage.HANDSHAKE_END_EVENT) {
         this.endpoint.setState(DscEndpointState.READY);
      } else {
         super.userEventTriggered(ctx, evt);
      }
   }

   // Forza la chiusura del canale dopo 20 secondi se ancora aperto
   private void closingChannel(ChannelHandlerContext ctx) {
      this.endpoint.setState(DscEndpointState.CLOSING);
      final Channel ch = ctx.channel();
      ctx.executor().schedule(new Runnable() {
         public void run() {
               if (ch.isOpen()) {
                  logger.warning("Forcing channel closure ...");
                  ch.close().addListener(LogOnFailure.INSTANCE);
               }
         }
      }, Consts.CLOSING_TIMEOUT, TimeUnit.SECONDS);
   }

   // Gestione PIN associato al canale tramite AttributeKey
   public static String getPin(Channel ch) {
      return ch.attr(PIN_KEY).get();
   }

   public static void setPin(Channel ch, String pin) {
      ch.attr(PIN_KEY).set(pin);
   }
}
