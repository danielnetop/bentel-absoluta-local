package cms.device.spi;

import java.util.List;
import java.util.Map;

import cms.device.api.Input;
import cms.device.api.Output;
import cms.device.api.Panel;
import cms.device.api.Partition;

public interface PanelProvider {
   void initialize(PanelProvider.PanelCallback callback);

   Panel.ConnStatus connect();

   void disconnect();

   void arming(Panel.Arming armingMode);

   void partitionArming(String partitionId, Partition.Arming armingMode);

   void setBypassed(String zoneID, boolean setBypassed);

   boolean getBypassed(String zoneID);

   void doOutputAction(String outputId, Output.Action action);

   boolean armingSupport(char presetMode);

   void armingSet(char mode);

   Map<String, String> getSettings();

   public interface PanelCallback extends AlertCallback {
      void connectionLost();

      void setArming(Panel.Arming armingMode);

      void setStatus(Panel.Status status);

      void changePartitions(List<String> partitionIds);

      void setPartitionRemoteName(String partitionId, String remoteName);

      void setPartitionArming(String partitionId, Partition.Arming armingMode);

      void setPartitionStatus(String partitionId, Partition.Status status);

      void changeInputs(List<String> inputIds);

      void setInputRemoteName(String inputId, String remoteName);

      void setInputStatus(String inputId, Input.Status status);

      void tagInputIntoPartition(String inputId, List<String> partitionIds);

      void changeOutputs(List<String> outputIds);

      void setOutputRemoteName(String outputId, String remoteName);

      void setOutputStatus(String outputId, Output.Status status);

      void setLabelArming(char presetMode, String label);
   }
}
