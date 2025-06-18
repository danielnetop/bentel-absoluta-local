package protocol.dsc.errors;

public class WrongSequenceNumberException extends DscProtocolException {
   public WrongSequenceNumberException(int receivedCommand, String message) {
      super(receivedCommand, message);
   }

   @Override
   public int getErrorCode() {
      return 3;
   }
}