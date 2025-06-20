package absoluta.spi;

import java.util.List;

import absoluta.AbsolutaPanelProvider.providerConnStatus;
import absoluta.connection.PanelStatus.globalArming;
import absoluta.connection.PanelStatus.inputStatus;
import absoluta.connection.PanelStatus.outputAction;
import absoluta.connection.PanelStatus.outputStatus;
import absoluta.connection.PanelStatus.partitionArming;
import absoluta.connection.PanelStatus.partitionStatus;

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