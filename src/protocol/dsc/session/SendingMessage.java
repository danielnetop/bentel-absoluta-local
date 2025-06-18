package protocol.dsc.session;

import com.google.common.base.Preconditions;

import protocol.dsc.Message;
import protocol.dsc.Priority;

public class SendingMessage<P, V> {
   private final Message<P, V> message;
   private final P parameter;
   private final Priority priority;

   public SendingMessage(Message<P, V> message, P parameter, Priority priority) {
      this.message = Preconditions.checkNotNull(message);
      this.parameter = parameter;
      this.priority = Preconditions.checkNotNull(priority);
   }

   public Message<P, V> getMessage() {
      return this.message;
   }

   public P getParameter() {
      return this.parameter;
   }

   public Priority getPriority() {
      return this.priority;
   }

   @Override
   public String toString() {
      return message + "(" + parameter + ") " + priority;
   }
}
