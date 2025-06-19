package cms.device.api;

import java.util.List;

import cms.device.spi.PanelProvider;
import plugin.absoluta.connection.PanelStatus;

final class Callbacks {
   static enum PanelCb implements PanelProvider.PanelCallback {
      DUMMY;

      @Override
      public void changePartitions(List<String> partitionIds) { }

      @Override
      public void setPartitionArming(String partitionId, PanelStatus.partitionArming armingMode) { }

      @Override
      public void changeInputs(List<String> inputIds) { }

      @Override
      public void tagInputIntoPartition(String inputId, List<String> partitionIds) { }

      @Override
      public void setArming(Panel.Arming armingMode) { }

      @Override
      public void setStatus(Panel.Status status) { }

      @Override
      public void setInputStatus(String inputId, PanelStatus.inputStatus status) { }

      @Override
      public void setPartitionStatus(String partitionId, PanelStatus.partitionStatus status) { }

      @Override
      public void connectionLost() { }

      @Override
      public void setLabelArming(char presetMode, String label) { }

      @Override
      public void alert(String message) { }

      @Override
      public void setPartitionRemoteName(String partitionId, String remoteName) { }

      @Override
      public void setInputRemoteName(String inputId, String remoteName) { }

      @Override
      public void changeOutputs(List<String> outputIds) { }

      @Override
      public void setOutputRemoteName(String outputId, String remoteName) { }

      @Override
      public void setOutputStatus(String outputId, PanelStatus.outputStatus status) { }
   }
}