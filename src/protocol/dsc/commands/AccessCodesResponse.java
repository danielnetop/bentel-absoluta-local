package protocol.dsc.commands;

import com.google.common.collect.ImmutableList;

import protocol.dsc.base.DscArray;
import protocol.dsc.base.DscNumber;
import protocol.dsc.base.DscSerializable;
import protocol.dsc.base.DscString;

import java.util.ArrayList;
import java.util.List;

public class AccessCodesResponse extends DscCommandWithResponse.Response<AccessCodes> implements DscArray.ElementProvider<DscString> {
   private final DscNumber accessCodeLength = DscNumber.newUnsignedNum(1);
   private final DscArray<DscString> codes = new DscArray<>(this);

   public AccessCodesResponse() {
      super(new AccessCodes());
   }

   protected List<DscSerializable> getResponseFields() {
      return ImmutableList.of(this.accessCodeLength, this.codes);
   }

   public int getCommandNumber() {
      return 18230;
   }

   public int getUserNumberStart() {
      return ((AccessCodes)this.requestInstance).getUserNumberStart();
   }

   public int getNumberOfUsers() {
      return ((AccessCodes)this.requestInstance).getNumberOfUsers();
   }

   public List<String> getCodes() {
      List<String> result = new ArrayList<>(this.codes.size());
      for (int i = 0; i < this.codes.size(); i++) {
         DscString code = this.codes.get(i);
         result.add(code.toString());
      }
      return result;
   }

   public int numberOfElements() {
      return this.getNumberOfUsers();
   }

   public DscString newElement() {
      return DscString.newBCDString(this.accessCodeLength.toInt());
   }
}
