package protocol.dsc.base;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.List;

public abstract class DscStruct implements DscSerializable {
   protected abstract List<DscSerializable> getFields();

   @Override
   public void readFrom(ByteBuf buffer) throws DecoderException, IndexOutOfBoundsException {
      for (DscSerializable field : this.getFields()) {
         field.readFrom(buffer);
      }
   }

   @Override
   public void writeTo(ByteBuf buffer) {
      for (DscSerializable field : this.getFields()) {
         field.writeTo(buffer);
      }
   }

   @Override
   public boolean isEquivalent(DscSerializable other) {
      if (this.getClass() == other.getClass()) {
         List<DscSerializable> fieldsThis = this.getFields();
         List<DscSerializable> fieldsOther = ((DscStruct) other).getFields();
         int size = fieldsThis.size();
         if (size != fieldsOther.size()) {
               return false;
         }
         for (int i = 0; i < size; ++i) {
               if (!fieldsThis.get(i).isEquivalent(fieldsOther.get(i))) {
                  return false;
               }
         }
         return true;
      }
      return false;
   }
}