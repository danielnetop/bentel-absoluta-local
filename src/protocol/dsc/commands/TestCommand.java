package protocol.dsc.commands;

import com.google.common.collect.ImmutableList;

import protocol.dsc.base.DscSerializable;
import protocol.dsc.base.DscString;
import protocol.dsc.transport.CommandDecoder;
import protocol.dsc.util.DscUtils;

import java.util.List;
import java.util.logging.Logger;

public class TestCommand extends DscCommandWithAppSeq {
   private static final Logger logger = Logger.getLogger(CommandDecoder.class.getName());
   private final int commandNumber;
   private final DscString payload = DscString.newBCDString();

   public TestCommand(int var1) {
      super(false);
      this.commandNumber = DscUtils.validateUShort(var1);
      logger.warning("new test command 0x%04X; use it only for test " + var1);
   }

   protected List<DscSerializable> getFields() {
      return ImmutableList.of(this.payload);
   }

   public int getCommandNumber() {
      return this.commandNumber;
   }

   public String getPayload() {
      return this.payload.toString();
   }

   public void setPayload(String var1) {
      this.payload.setString(var1);
   }
}
