package protocol.dsc.util;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.concurrent.CancellationException;
import java.util.logging.Logger;

public class LogOnFailure implements ChannelFutureListener {
   private static final Logger logger = Logger.getLogger(LogOnFailure.class.getName());
   public static final LogOnFailure INSTANCE = new LogOnFailure();

   @Override
   public void operationComplete(ChannelFuture future) throws Exception {
      Throwable cause = future.cause();
      if (cause != null) {
         if (cause instanceof CancellationException) {
            logger.finer("Operation cancelled");
         } else {
            logger.finer("Operation failed");
         }
      }
   }

   private LogOnFailure() { }
}