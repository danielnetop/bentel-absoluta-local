package protocol.dsc;

import java.util.EventListener;

public interface MessageListener extends EventListener {
   void newValue(NewValue newValue);

   void error(DscError error);
}
