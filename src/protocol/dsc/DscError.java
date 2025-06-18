package protocol.dsc;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class DscError extends Message.Response {
   private final DscError.Type type;
   private final String description;
   private final Integer responseCode;

   private <P> DscError(DscError.Type type, String description, Message<P, ?> message, P param, Integer responseCode) {
      super(message, param);
      this.type = Preconditions.checkNotNull(type);
      this.description = Preconditions.checkNotNull(description);
      this.responseCode = responseCode;
   }

   public static DscError newFatalError(String description) {
      return new DscError(DscError.Type.FATAL, description, null, null, null);
   }

   public static DscError newFatalError(Throwable throwable) {
      return newFatalError(getDescription(throwable));
   }

   public static DscError newGenericError(String description) {
      return new DscError(DscError.Type.GENERIC, description, null, null, null);
   }

   public static DscError newGenericError(Throwable throwable) {
      return newGenericError(getDescription(throwable));
   }

   public static <P> DscError newMessageError(Message<P, ?> message, P param, Integer responseCode, String description) {
      return new DscError(DscError.Type.MESSAGE, description, Preconditions.checkNotNull(message), param, responseCode);
   }

   public static <P> DscError newMessageError(Message<P, ?> message, P param, Throwable throwable) {
      return newMessageError(message, param, null, getDescription(throwable));
   }

   public DscError.Type getType() {
      return this.type;
   }

   public String getDescription() {
      return this.description;
   }

   public Integer getResponseCode() {
      return this.responseCode;
   }

   @Override
   public String toString() {
      // Restituisce una descrizione leggibile dell'errore
      switch (this.type) {
         case FATAL:
               return "Fatal error: " + this.description;
         case GENERIC:
               return "Generic error: " + this.description;
         case MESSAGE:
               // Messaggio dettagliato solo per errori legati a messaggi
               return "Message related error for " + this.message + "(" + this.param + "): " + this.description +
                     (this.responseCode != null ? String.format(" [response code: 0x%02X]", this.responseCode) : "");
         default:
               throw new AssertionError(this.type);
      }
   }

   private static String getDescription(Throwable throwable) {
      // Cerca di estrarre un messaggio utile dall'eccezione
      return MoreObjects.firstNonNull(throwable.getMessage(),
               MoreObjects.firstNonNull(throwable.toString(), throwable.getClass().getSimpleName()));
   }

   public static enum Type {
      FATAL,
      GENERIC,
      MESSAGE;
   }
}