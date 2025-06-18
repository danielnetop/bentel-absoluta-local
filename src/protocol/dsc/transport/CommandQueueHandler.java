package protocol.dsc.transport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import protocol.dsc.Priority;
import protocol.dsc.commands.DscCommand;
import protocol.dsc.session.Consts;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;

public class CommandQueueHandler extends ChannelOutboundHandlerAdapter {
   private static final Logger logger = Logger.getLogger(CommandQueueHandler.class.getName());
   // Una coda per ogni priorità
   private final Map<Priority, Queue<CommandQueueHandler.WaitingMsg>> queues;

   public CommandQueueHandler() {
      Builder<Priority, Queue<CommandQueueHandler.WaitingMsg>> builder = ImmutableMap.builder();
      for (Priority p : Priority.values()) {
         builder.put(p, new ArrayDeque<CommandQueueHandler.WaitingMsg>());
      }
      this.queues = builder.build();
   }

   public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      // Svuota tutte le code e fallisce le promise se il canale viene rimosso
      for (Priority p : Priority.values()) {
         Queue<CommandQueueHandler.WaitingMsg> q = this.queues.get(p);
         if (!q.isEmpty()) {
               logger.finer("Removing " + q.size() + " enqueued commands with priority " + p);
               CancellationException ex = new CancellationException("channel inactivated before sending");
               for (CommandQueueHandler.WaitingMsg msg : q) {
                  msg.promise.setFailure(ex);
               }
               q.clear();
         }
      }
   }

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (msg instanceof DscCommand) {
         DscCommand cmd = (DscCommand) msg;
         if (cmd.getPriority() == null) {
               logger.finer("Sending immediately: " + cmd);
               ctx.write(cmd, promise);
         } else {
               this.enqueue(ctx, cmd, promise);
               this.tryToSend(ctx);
         }
      } else {
         this.tryToSend(ctx);
         super.write(ctx, msg, promise);
      }
   }

   // Inserisce il comando nella coda della sua priorità, con limite massimo
   private void enqueue(ChannelHandlerContext ctx, DscCommand cmd, ChannelPromise promise) throws IllegalStateException {
      Priority prio = cmd.getPriority();
      Queue<CommandQueueHandler.WaitingMsg> q = this.queues.get(prio);
      logger.finer("Enqueuing a command with priority " + prio + ": " + cmd);
      if (q.size() < Consts.MAX_COMMAND_QUEUE_LEN) {
         q.add(new CommandQueueHandler.WaitingMsg(cmd, promise));
      } else {
         logger.warning("Command queue is full");
         IllegalStateException ex = new IllegalStateException("command queue is full");
         ctx.fireExceptionCaught(ex);
         throw ex;
      }
   }

   // Tenta di inviare il primo comando disponibile in ordine di priorità se pronto
   private void tryToSend(ChannelHandlerContext ctx) {
      if (TransportLayerEncoder.isReadyForANewCommand(ctx)) {
         for (Priority p : Priority.values()) {
               CommandQueueHandler.WaitingMsg msg = this.queues.get(p).poll();
               if (msg != null) {
                  logger.finer("Sending enqueued command with priority " + p + ": " + msg.cmd);
                  ctx.write(msg.cmd, msg.promise);
                  break;
               }
         }
      }
   }

   private static class WaitingMsg {
      final DscCommand cmd;
      final ChannelPromise promise;

      WaitingMsg(DscCommand cmd, ChannelPromise promise) {
         this.cmd = cmd;
         this.promise = promise;
      }

      public String toString() {
         return "Waiting message: " + this.cmd;
      }
   }
}
