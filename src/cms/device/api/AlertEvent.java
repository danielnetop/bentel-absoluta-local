package cms.device.api;

import java.util.EventObject;

public class AlertEvent extends EventObject {
   private final String alertMessage;

   public AlertEvent(String alertMessage, Object eventSource) {
      super(eventSource);
      this.alertMessage = alertMessage;
   }

   public String getAlertMessage() {
      return this.alertMessage;
   }

   @Override
   public String toString() {
      return "AlertEvent{alertMessage=" + this.alertMessage + ", source=" + this.source + '}';
   }
}
