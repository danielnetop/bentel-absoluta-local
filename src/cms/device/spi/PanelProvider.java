package cms.device.spi;

import java.util.List;
import java.util.Map;

import plugin.absoluta.AbsolutaPanelProvider.providerConnStatus;
import plugin.absoluta.connection.PanelStatus.globalArming;
import plugin.absoluta.connection.PanelStatus.partitionArming;
import plugin.absoluta.connection.PanelStatus.partitionStatus;
import plugin.absoluta.connection.PanelStatus.inputStatus;
import plugin.absoluta.connection.PanelStatus.outputAction;
import plugin.absoluta.connection.PanelStatus.outputStatus;

public interface PanelProvider {
   void initialize(PanelProvider.PanelCallback callback);

   providerConnStatus connect();

   void disconnect();

   void arming(globalArming armingMode);

   void partitionArming(String partitionId, partitionArming armingMode);

   void setBypassed(String zoneID, boolean setBypassed);

   boolean getBypassed(String zoneID);

   void doOutputAction(String outputId, outputAction action);

   boolean armingSupport(char presetMode);

   void armingSet(char mode);

   Map<String, String> getSettings();

   public interface PanelCallback extends AlertCallback {
      void connectionLost();

      void setArming(globalArming armingMode);

      void changePartitions(List<String> partitionIds);

      void setPartitionRemoteName(String partitionId, String remoteName);

      void setPartitionArming(String partitionId, partitionArming armingMode);

      void setPartitionStatus(String partitionId, partitionStatus status);

      void changeInputs(List<String> inputIds);

      void setInputRemoteName(String inputId, String remoteName);

      void setInputStatus(String inputId, inputStatus status);

      void tagInputIntoPartition(String inputId, List<String> partitionIds);

      void changeOutputs(List<String> outputIds);

      void setOutputRemoteName(String outputId, String remoteName);

      void setOutputStatus(String outputId, outputStatus status);

      void setLabelArming(char presetMode, String label);
   }
}