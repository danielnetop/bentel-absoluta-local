package protocol.dsc.util;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;

public class LogOnFailure implements ChannelFutureListener {
   private static final Logger logger = Logger.getLogger(LogOnFailure.class.getName());
   public static final LogOnFailure INSTANCE = new LogOnFailure();

   public void operationComplete(ChannelFuture var1) throws Exception {
      Throwable var2 = var1.cause();
      if (var2 != null) {
         if (var2 instanceof CancellationException) {
            logger.fine("operation cancelled");
         } else {
            logger.warning("operation failed " + var2);
         }
      }

   }

   private LogOnFailure() {
   }
}
