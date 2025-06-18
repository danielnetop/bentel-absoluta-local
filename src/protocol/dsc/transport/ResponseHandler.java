package protocol.dsc.transport;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import protocol.dsc.DscError;
import protocol.dsc.commands.DscCommandWithAppSeq;
import protocol.dsc.commands.DscGeneralResponse;
import protocol.dsc.commands.DscResponse;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class ResponseHandler extends ChannelDuplexHandler {
   private static final Logger logger = Logger.getLogger(ResponseHandler.class.getName());
   private static final int MAX_WAITING_CMDS = 32;
   // Coda dei comandi in attesa di risposta
   private final Queue<DscCommandWithAppSeq> waitingCmds = new LinkedList<DscCommandWithAppSeq>();

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      if (!this.waitingCmds.isEmpty()) {
         logger.fine("Removing " + this.waitingCmds.size() + " waiting commands");
         this.waitingCmds.clear();
      }
      super.channelInactive(ctx);
   }

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      // Solo i comandi con appSeq vengono messi in coda
      if (msg instanceof DscCommandWithAppSeq) {
         DscCommandWithAppSeq cmd = (DscCommandWithAppSeq) msg;
         // Limita la coda a MAX_WAITING_CMDS elementi
         if (this.waitingCmds.size() == MAX_WAITING_CMDS) {
               DscCommandWithAppSeq removed = this.waitingCmds.remove();
               logger.fine("Waiting command removed to limit the queue size: " + removed);
         }
         this.waitingCmds.add(cmd);
      }
      super.write(ctx, msg, promise);
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      boolean matched = false;
      if (msg instanceof DscResponse) {
         DscResponse resp = (DscResponse) msg;
         // Cerca il comando in attesa che corrisponde alla risposta
         for (DscCommandWithAppSeq cmd : waitingCmds) {
               if (cmd.matchAsResponse(resp)) {
                  logger.finer("Response " + resp + " received for " + cmd);
                  waitingCmds.remove(cmd);
                  matched = true;
                  // Notifica il comando se è una risposta generale
                  if (resp instanceof DscGeneralResponse) {
                     DscGeneralResponse genResp = (DscGeneralResponse) resp;
                     cmd.generalResponseReceived(ctx.channel(), genResp);
                  }
                  break;
               }
         }
      }

      if (msg instanceof DscGeneralResponse) {
         if (!matched) {
               DscGeneralResponse genResp = (DscGeneralResponse) msg;
               logger.fine("Unmatched general response " + genResp);
               // Propaga errore se la risposta generale non è di successo
               if (!genResp.isSuccess()) {
                  ctx.fireChannelRead(DscError.newGenericError(genResp.getDescription()));
               }
         }
      } else {
         super.channelRead(ctx, msg);
      }
   }
}