package cms.device.spi;

import java.util.List;
import java.util.Map;

import cms.device.api.Panel;
import plugin.absoluta.connection.PanelStatus;

public interface PanelProvider {
   void initialize(PanelProvider.PanelCallback callback);

   Panel.ConnStatus connect();

   void disconnect();

   void arming(PanelStatus.globalArming armingMode);

   void partitionArming(String partitionId, PanelStatus.partitionArming armingMode);

   void setBypassed(String zoneID, boolean setBypassed);

   boolean getBypassed(String zoneID);

   void doOutputAction(String outputId, PanelStatus.outputAction action);

   boolean armingSupport(char presetMode);

   void armingSet(char mode);

   Map<String, String> getSettings();

   public interface PanelCallback extends AlertCallback {
      void connectionLost();

      void setArming(PanelStatus.globalArming armingMode);

      void setStatus(Panel.Status status);

      void changePartitions(List<String> partitionIds);

      void setPartitionRemoteName(String partitionId, String remoteName);

      void setPartitionArming(String partitionId, PanelStatus.partitionArming armingMode);

      void setPartitionStatus(String partitionId, PanelStatus.partitionStatus status);

      void changeInputs(List<String> inputIds);

      void setInputRemoteName(String inputId, String remoteName);

      void setInputStatus(String inputId, PanelStatus.inputStatus status);

      void tagInputIntoPartition(String inputId, List<String> partitionIds);

      void changeOutputs(List<String> outputIds);

      void setOutputRemoteName(String outputId, String remoteName);

      void setOutputStatus(String outputId, PanelStatus.outputStatus status);

      void setLabelArming(char presetMode, String label);
   }
}
