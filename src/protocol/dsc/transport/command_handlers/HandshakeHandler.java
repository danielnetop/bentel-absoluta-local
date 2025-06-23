package protocol.dsc.transport.command_handlers;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import protocol.dsc.commands.CommandResponse;
import protocol.dsc.commands.DscCommand;
import protocol.dsc.commands.DscCommandWithAppSeq;
import protocol.dsc.commands.DscGeneralResponse;
import protocol.dsc.commands.EndSession;
import protocol.dsc.session.SessionInfo;
import protocol.dsc.transport.SimpleMessage;
import protocol.dsc.util.LogOnFailure;

import java.util.logging.Logger;

public abstract class HandshakeHandler<C extends DscCommand> extends ChannelInboundHandlerAdapter {
   private static final Logger logger = Logger.getLogger(HandshakeHandler.class.getName());
   private final Class<C> cmdClass;
   private ChannelHandlerContext ctx;
   private boolean sent;
   private boolean received;

   protected HandshakeHandler(Class<C> var1) {
      if (this.isSharable()) {
         throw new IllegalStateException("@Sharable annotation is not allowed");
      } else {
         this.cmdClass = Preconditions.checkNotNull(var1);
      }
   }

   public abstract boolean validateOwnInfo(SessionInfo var1);

   protected abstract C getCommand(Channel var1);

   protected int commandReceived(Channel var1, C var2) {
      return 0;
   }

   protected void commandSent(Channel var1) {
   }

   public void startHandshakeStage() {
      if (this.ctx == null) {
         throw new IllegalStateException("no context");
      } else {
         this.sendCommand(this.ctx);
      }
   }

   public final void handlerAdded(ChannelHandlerContext var1) throws Exception {
      this.ctx = var1;
      super.handlerAdded(var1);
   }

   public final void handlerRemoved(ChannelHandlerContext var1) throws Exception {
      this.ctx = null;
      super.handlerRemoved(var1);
   }

   public final void channelRead(ChannelHandlerContext var1, Object var2) throws Exception {
      if (this.cmdClass.isInstance(var2)) {
         this.received = true;
         C var3 = this.cmdClass.cast(var2);
         int var4 = this.commandReceived(var1.channel(), var3);
         if (var3 instanceof DscCommandWithAppSeq) {
            DscCommandWithAppSeq var5 = (DscCommandWithAppSeq)var3;
            CommandResponse var6 = new CommandResponse();
            var6.setCommandSeq(var5.getAppSeq());
            var6.setResponseCode(var4);
            var1.write(var6).addListener(LogOnFailure.INSTANCE);
         }

         if (var4 == 0) {
            this.receptionSuccessfullyCompleted(var1);
         } else {
            this.failure(var1);
         }
      } else {
         super.channelRead(var1, var2);
      }

   }

   private void sendingSuccessfullyCompleted(ChannelHandlerContext var1) {
      this.sent = true;
      if (this.received) {
         this.success(var1);
      }

   }

   private void receptionSuccessfullyCompleted(ChannelHandlerContext var1) {
      this.received = true;
      if (this.sent) {
         this.success(var1);
      } else {
         this.sendCommand(var1);
      }

   }

   private void success(ChannelHandlerContext var1) {
      logger.fine("handshake stage completed for " + this.cmdClass.getSimpleName());
      var1.fireUserEventTriggered(SimpleMessage.HANDSHAKE_STAGE_COMPLETED_EVENT);
   }

   private void failure(ChannelHandlerContext var1) {
      var1.write(new EndSession()).addListener(LogOnFailure.INSTANCE);
   }

   private void sendCommand(ChannelHandlerContext var1) {
      C var2 = this.getCommand(var1.channel());
      boolean var3;
      if (var2 instanceof DscCommandWithAppSeq) {
         DscCommandWithAppSeq var4 = (DscCommandWithAppSeq)var2;

         assert !var4.hasResponseCallback();

         var4.setResponseCallback(new ResponseReceivedCallback());
         var3 = true;
      } else {
         var3 = false;
      }

      var1.write(var2).addListener(new CommandSentCallback()).addListener(LogOnFailure.INSTANCE);
      if (!var3) {
         this.sendingSuccessfullyCompleted(var1);
      }

   }

   private class CommandSentCallback implements ChannelFutureListener {
      private CommandSentCallback() {
      }

      public void operationComplete(ChannelFuture var1) throws Exception {
         if (var1.isSuccess()) {
            HandshakeHandler.this.commandSent(var1.channel());
         } else {
            HandshakeHandler.logger.warning("sending failed for " + HandshakeHandler.this.cmdClass.getSimpleName() + " " + var1.cause());
         }
      }
   }

   private class ResponseReceivedCallback implements DscCommandWithAppSeq.ResponseCallback {
      private ResponseReceivedCallback() {
      }

      public void generalResponseReceived(Channel var1, DscGeneralResponse var2) {
         ChannelHandlerContext var3 = var1.pipeline().context(HandshakeHandler.this);
         if (var2.isSuccess()) {
            HandshakeHandler.this.sendingSuccessfullyCompleted(var3);
         } else {
            HandshakeHandler.logger.warning("negative response for " + HandshakeHandler.this.cmdClass.getSimpleName() + ": " + var2);
            HandshakeHandler.this.failure(var3);
         }
      }
   }
}
