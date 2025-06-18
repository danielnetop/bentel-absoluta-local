package protocol.dsc.transport;

import com.google.common.base.Preconditions;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

import protocol.dsc.session.SessionInfo;
import protocol.dsc.transport.command_handlers.HandshakeHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

public class PipelineHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = Logger.getLogger(PipelineHandler.class.getName());
   private final Queue<HandshakeHandler<?>> handshakeHandlers;
   private final List<ChannelHandler> normalModeHandlers;
   private final List<String> managedHandlerNames = new ArrayList<>();

   public PipelineHandler(Queue<HandshakeHandler<?>> handshakeHandlers, List<ChannelHandler> normalModeHandlers) {
      this.handshakeHandlers = Preconditions.checkNotNull(handshakeHandlers);
      this.normalModeHandlers = Preconditions.checkNotNull(normalModeHandlers);
   }

   @Override
   public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      SessionInfo sessionInfo = SessionInfo.getOwnInfo(ctx.channel());
      if (sessionInfo.getClient() == null) {
         throw new IllegalStateException("invalid own info (null client)");
      }
      // Validazione info sessione tramite tutti gli handshake handler
      for (HandshakeHandler<?> handler : this.handshakeHandlers) {
         if (!handler.validateOwnInfo(sessionInfo)) {
               throw new IllegalStateException(
                  String.format("invalid own info (reported by %s)", handler.getClass().getSimpleName())
               );
         }
      }
   }

   @Override
   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      super.channelActive(ctx);
      logger.fine("Handshake begin");
      ctx.fireUserEventTriggered(SimpleMessage.HANDSHAKE_BEGIN_EVENT);
      this.nextStage(ctx);
   }

   @Override
   public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      // Rimuove tutti gli handler gestiti dal pipeline
      this.setManagedHandlers(ctx, Collections.emptyList());
      super.handlerRemoved(ctx);
   }

   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      // Avanza di stage handshake su evento specifico
      if (evt == SimpleMessage.HANDSHAKE_STAGE_COMPLETED_EVENT) {
         this.nextStage(ctx);
      } else {
         super.userEventTriggered(ctx, evt);
      }
   }

   // Gestisce avanzamento handshake o passaggio a modalità normale
   private void nextStage(ChannelHandlerContext ctx) {
      HandshakeHandler<?> handler = this.handshakeHandlers.poll();
      if (handler != null) {
         logger.fine("Handshake next stage");
         this.setManagedHandlers(ctx, Collections.singletonList(handler));
         SessionInfo sessionInfo = SessionInfo.getOwnInfo(ctx.channel());
         if (sessionInfo.isClient()) {
               handler.startHandshakeStage();
         }
      } else {
         logger.fine("Handshake end");
         this.setManagedHandlers(ctx, this.normalModeHandlers);
         ctx.fireUserEventTriggered(SimpleMessage.HANDSHAKE_END_EVENT);
      }
   }

   // Gestisce dinamicamente gli handler nel pipeline (aggiunta/rimozione)
   private void setManagedHandlers(ChannelHandlerContext ctx, List<ChannelHandler> handlers) {
      ChannelPipeline pipeline = ctx.pipeline();

      // Rimuove tutti gli handler gestiti precedentemente
      for (String handlerName : this.managedHandlerNames) {
         logger.finer("Removing " + handlerName + " from pipeline");
         pipeline.remove(handlerName);
      }
      this.managedHandlerNames.clear();

      String previousName = ctx.name();

      // Aggiunge i nuovi handler in ordine inverso per mantenere la sequenza
      for (int i = handlers.size() - 1; i >= 0; --i) {
         ChannelHandler handler = handlers.get(i);
         String handlerName = String.format("%s:%s#%d", ctx.name(), handler.getClass().getSimpleName(), i);
         logger.finer("Adding " + handlerName + " to pipeline before " + previousName);
         pipeline.addBefore(previousName, handlerName, handler);
         this.managedHandlerNames.add(handlerName);
         previousName = handlerName;
      }
   }
}