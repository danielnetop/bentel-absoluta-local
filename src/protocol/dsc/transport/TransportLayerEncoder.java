package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.logging.Logger;

@Sharable
public class TransportLayerEncoder extends MessageToByteEncoder<ByteBuf> {
   private static final Logger logger = Logger.getLogger(TransportLayerEncoder.class.getName());

   @Override
   protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
      // Verifica se il messaggio è un ACK basso (ACK di livello basso)
      boolean isLowAck = SequenceHandlersHelper.isLowACK(msg);

      // Ottiene i contatori di sequenza associati al canale
      SequenceHandlersHelper.Counters counters = SequenceHandlersHelper.getCounters(ctx);

      // Se non è un ACK basso e non si è pronti per un nuovo comando, logga un warning
      if (!isLowAck && !counters.isReadyForANewCommand()) {
         logger.fine("Command sent before confirmation of the previous");
      }

      // Aggiorna il prossimo numero di sequenza in base al tipo di messaggio
      counters.setNextSequenceNumber(isLowAck);

      // Ottiene il numero di sequenza locale e remoto
      int localSeq = counters.sequenceNumber();
      int remoteSeq = counters.remoteSequenceNumber();

      // Scrive i numeri di sequenza nel buffer di output
      out.writeByte(localSeq);
      out.writeByte(remoteSeq);

      // Scrive il payload originale nel buffer di output
      out.writeBytes(msg);

      // Aggiorna il numero di sequenza remoto inviato
      counters.setSentRemoteSequenceNumber(remoteSeq);
   }

   // Ritorna true se si può inviare un nuovo comando (conferma ricevuta)
   public static boolean isReadyForANewCommand(ChannelHandlerContext ctx) {
      SequenceHandlersHelper.Counters counters = SequenceHandlersHelper.getCounters(ctx);
      return counters.isReadyForANewCommand();
   }

   // Ritorna true se è richiesto l'invio di un ACK in uscita
   public static boolean isOutgoingACKRequired(ChannelHandlerContext ctx) {
      SequenceHandlersHelper.Counters counters = SequenceHandlersHelper.getCounters(ctx);
      return counters.isOutgoingACKRequired();
   }
}
