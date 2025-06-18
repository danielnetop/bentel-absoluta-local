package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;

import protocol.dsc.errors.WrongSequenceNumberException;

import java.nio.ByteOrder;
import java.util.List;
import java.util.logging.Logger;

@Sharable
public class TransportLayerDecoder extends MessageToMessageDecoder<ByteBuf> {
   private static final Logger logger = Logger.getLogger(TransportLayerDecoder.class.getName());

   @SuppressWarnings("deprecation")
   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
         throws WrongSequenceNumberException, CorruptedFrameException {
      // Assicura endianess corretta
      assert in.order() == ByteOrder.BIG_ENDIAN;

      // Frame valido: 2 byte (ACK) o almeno 4 byte (messaggio normale)
      if (in.readableBytes() != 2 && in.readableBytes() < 4) {
         throw new CorruptedFrameException("invalid frame lenght");
      } else {
         // Lettura sequenza locale e remota
         short remoteSeq = in.readUnsignedByte();
         short localSeq = in.readUnsignedByte();

         // Verifica se è un ACK basso
         boolean isLowAck = SequenceHandlersHelper.isLowACK(in);

         // Ottiene i contatori di sequenza associati al canale
         SequenceHandlersHelper.Counters counters = SequenceHandlersHelper.getCounters(ctx);

         // Per error reporting
         int errorSeq = isLowAck ? 0 : in.getUnsignedShort(in.readerIndex());

         try {
               if (!isLowAck) {
                  // Se reset, solo il primo messaggio può avere seq 0
                  if (remoteSeq == 0) {
                     if (!counters.isFirstMessage()) {
                           throw new WrongSequenceNumberException(errorSeq, "reset request");
                     }
                  } else {
                     // Se seq ripetuta, ignora il messaggio
                     if (remoteSeq == counters.remoteSequenceNumber()) {
                           logger.fine("Repeated sequence number " + remoteSeq + ": ignoring message");
                           return;
                     }
                     // Se seq inattesa, errore
                     if (remoteSeq != SequenceHandlersHelper.next(counters.remoteSequenceNumber())) {
                           throw new WrongSequenceNumberException(
                              errorSeq,
                              String.format("unexpected sequence number: %d instead of %d",
                                 remoteSeq, SequenceHandlersHelper.next(counters.remoteSequenceNumber()))
                           );
                     }
                  }
               }

               // Il numero di sequenza locale deve essere quello atteso o quello precedente (per ACK)
               if (localSeq != counters.sequenceNumber() && localSeq != counters.prevSequenceNumber()) {
                  throw new WrongSequenceNumberException(
                     errorSeq,
                     String.format("unexpected remote sequence number: %d instead of %d or %d",
                           localSeq, counters.sequenceNumber(), counters.prevSequenceNumber())
                  );
               } else {
                  // Passa il payload decodificato agli handler successivi
                  out.add(in.readSlice(in.readableBytes()).retain());
               }
         } finally {
               // Aggiorna i contatori di sequenza
               counters.messageReceived(isLowAck, remoteSeq, localSeq);
         }
      }
   }
}