package protocol.dsc.commands;

import com.google.common.collect.ImmutableList;

import protocol.dsc.base.DscArray;
import protocol.dsc.base.DscBitMask;
import protocol.dsc.base.DscNumber;
import protocol.dsc.base.DscSerializable;
import protocol.dsc.base.DscVariableBytes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserPartitionAssignmentConfiguration extends DscAbstractCommand implements DscArray.ElementProvider<DscBitMask> {
   private final DscVariableBytes userNumberStart = new DscVariableBytes();
   private final DscVariableBytes numberOfUsers = new DscVariableBytes();
   private final DscNumber numberOfBytes = DscNumber.newUnsignedNum(1);
   private final DscArray<DscBitMask> partitionAssignments = new DscArray<DscBitMask>(this);

   protected List<DscSerializable> getFields() {
      return ImmutableList.of(this.userNumberStart, this.numberOfUsers, this.numberOfBytes, this.partitionAssignments);
   }

   public int getCommandNumber() {
      return 1909;
   }

   public Map<Integer, List<Integer>> getPartitionAssignments() {
      Map<Integer, List<Integer>> result = new HashMap<>();
      int userNum = this.userNumberStart.toPositiveInt();
      for (int i = 0; i < this.partitionAssignments.size(); i++) {
         DscBitMask bitMask = this.partitionAssignments.get(i);
         result.put(userNum++, bitMask.getTrueIndexes());
      }
      return result;
   }

   public int numberOfElements() {
      return this.numberOfUsers.toPositiveInt();
   }

   public DscBitMask newElement() {
      return new DscBitMask(this.numberOfBytes.toInt(), 1);
   }
}
