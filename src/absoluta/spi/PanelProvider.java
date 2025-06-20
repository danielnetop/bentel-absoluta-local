package absoluta.spi;

import java.util.List;

import absoluta.AbsolutaPanelProvider.providerConnStatus;
import absoluta.connection.PanelStatus.GlobalArming;
import absoluta.connection.PanelStatus.InputStatus;
import absoluta.connection.PanelStatus.OutputAction;
import absoluta.connection.PanelStatus.OutputStatus;
import absoluta.connection.PanelStatus.PartitionArming;
import absoluta.connection.PanelStatus.PartitionStatus;

public interface PanelProvider {
   void initialize(PanelProvider.PanelCallback callback);

   providerConnStatus connect();

   void disconnect();

   void arming(GlobalArming armingMode);

   void PartitionArming(int partitionId, PartitionArming armingMode);

   void setBypassed(int zoneID, boolean setBypassed);

   boolean getBypassed(int zoneID);

   void doOutputAction(int outputId, OutputAction action);

   boolean armingSupport(char presetMode);

   void armingSet(char mode);

   public interface PanelCallback extends AlertCallback {
      void connectionLost();

      void setArming(GlobalArming armingMode);

      void changePartitions(List<Integer> partitionIds);

      void setPartitionRemoteName(int partitionId, String remoteName);

      void setPartitionArming(int partitionId, PartitionArming armingMode);

      void setPartitionStatus(int partitionId, PartitionStatus status);

      void changeInputs(List<Integer> inputIds);

      void setInputRemoteName(int inputId, String remoteName);

      void setInputStatus(int inputId, InputStatus status);

      void tagInputIntoPartition(int inputId, List<Integer> partitionIds);

      void changeOutputs(List<Integer> outputIds);

      void setOutputRemoteName(int outputId, String remoteName);

      void setOutputStatus(int outputId, OutputStatus status);

      void setLabelArming(char presetMode, String label);
   }
}