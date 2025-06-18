package protocol.dsc.errors;

public class WrongCommandLengthException extends DscProtocolException {
   public WrongCommandLengthException(int receivedCommand, String message) {
      super(receivedCommand, message);
   }

   @Override
   public int getErrorCode() {
      return 1;
   }
}