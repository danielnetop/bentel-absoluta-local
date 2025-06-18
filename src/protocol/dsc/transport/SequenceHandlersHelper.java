package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

class SequenceHandlersHelper {
    // Chiave per associare i contatori al canale Netty
   private static final AttributeKey<SequenceHandlersHelper.Counters> COUNTERS_KEY = AttributeKey.valueOf("SequenceHandlersHelper.counters");

   // Calcola il prossimo numero di sequenza (1..255, ricicla su 1)
   static int next(int n) {
      return n < 255 ? n + 1 : 1;
   }

   // Riconosce un ACK basso: buffer vuoto
   static boolean isLowACK(ByteBuf buf) {
      return !buf.isReadable();
   }

   // Recupera o crea i contatori associati al canale
   static SequenceHandlersHelper.Counters getCounters(ChannelHandlerContext ctx) {
      Attribute<SequenceHandlersHelper.Counters> attr = ctx.channel().attr(COUNTERS_KEY);
      SequenceHandlersHelper.Counters counters = attr.get();

      // Garantisce che i contatori siano usati solo dal thread che li ha creati
      assert counters == null || counters.thread == Thread.currentThread();

      if (counters == null) {
         counters = new SequenceHandlersHelper.Counters();
         attr.set(counters);
      }

      return counters;
   }

   private SequenceHandlersHelper() {}

   static class Counters {
      // Thread che ha creato l'istanza (per sicurezza)
      private final Thread thread;
      private boolean firstMessage;
      private int prevSequenceNumber;
      private int sequenceNumber;
      private int remoteSequenceNumber;
      private int lastSentACK;
      private int lastReceivedACK;
      private int appSeq;

      private Counters() {
         this.thread = Thread.currentThread();
         this.firstMessage = true;
         this.lastSentACK = -1;
         this.lastReceivedACK = -1;
      }

      int prevSequenceNumber() { return this.prevSequenceNumber; }
      int sequenceNumber() { return this.sequenceNumber; }
      int remoteSequenceNumber() { return this.remoteSequenceNumber; }

      // Aggiorna il numero di sequenza locale se non è un ACK basso e non è il primo messaggio
      void setNextSequenceNumber(boolean isLowAck) {
         if (!isLowAck && !this.firstMessage) {
               this.prevSequenceNumber = this.sequenceNumber;
               this.sequenceNumber = SequenceHandlersHelper.next(this.sequenceNumber);
         }
         this.firstMessage = false;
      }

      // Aggiorna i contatori dopo la ricezione di un messaggio
      void messageReceived(boolean isLowAck, int remoteSeq, int localSeq) {
         if (!isLowAck) {
               this.remoteSequenceNumber = remoteSeq;
         }
         this.lastReceivedACK = localSeq;
         this.firstMessage = false;
      }

      // Memorizza l'ultimo ACK remoto inviato
      void setSentRemoteSequenceNumber(int remoteSeq) {
         this.lastSentACK = remoteSeq;
      }

      // Pronto per nuovo comando se ACK ricevuto o primo messaggio
      boolean isReadyForANewCommand() {
         return this.sequenceNumber == this.lastReceivedACK || this.firstMessage;
      }

      // Serve inviare un ACK se il remoteSeq è cambiato e non è il primo messaggio
      boolean isOutgoingACKRequired() {
         return this.remoteSequenceNumber != this.lastSentACK && !this.firstMessage;
      }

      boolean isFirstMessage() { return this.firstMessage; }

      // Incrementa e ritorna il contatore di sequenza applicativo
      int nextAppSeq() {
         this.appSeq = SequenceHandlersHelper.next(this.appSeq);
         return this.appSeq;
      }

      // Costruttore sintetico (non usato direttamente)
      Counters(Object unused) { this(); }
   }
}
