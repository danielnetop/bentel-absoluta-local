package absoluta;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.List;
import java.util.Objects;

import absoluta.connection.CustomizedArmingModes;
import absoluta.connection.PanelStatus;
import absoluta.spi.PanelProvider.PanelCallback;

class CallbackListener implements PropertyChangeListener {
   private final PanelCallback callback;
   private final PanelStatus panelStatus;

   CallbackListener(PanelCallback var1, PanelStatus var2) {
      this.callback = (PanelCallback)Objects.requireNonNull(var1);
      this.panelStatus = (PanelStatus)Objects.requireNonNull(var2);
   }

   public void propertyChange(PropertyChangeEvent property) {
      assert this.panelStatus == property.getSource();

      if (property.getNewValue() != null) {
         if (property instanceof IndexedPropertyChangeEvent) {
            int indexProperty = ((IndexedPropertyChangeEvent)property).getIndex();
            String propertyName = property.getPropertyName();
            switch(propertyName) {
               case "PARTITION_ARMING":
                  this.callback.setPartitionArming(indexProperty, this.panelStatus.getPartitionArming(indexProperty));
                  break;
               case "PARTITION_STATUS":
                  this.callback.setPartitionStatus(indexProperty, this.panelStatus.getPartitionStatus(indexProperty));
                  break;
               case "PARTITION_LABEL":
                  this.callback.setPartitionRemoteName(indexProperty, this.panelStatus.getPartitionLabel(indexProperty));
                  break;
               case "ZONE_STATUS":
               case "ZONE_BYPASS":
                  this.callback.setInputStatus(indexProperty, this.panelStatus.getZoneStatus(indexProperty));
                  break;
               case "ZONE_LABEL":
                  this.callback.setInputRemoteName(indexProperty, this.panelStatus.getZoneLabel(indexProperty));
                  break;
               case "OUTPUT_STATUS":
                  this.callback.setOutputStatus(indexProperty, this.panelStatus.getOutputStatus(indexProperty));
                  break;
               case "OUTPUT_LABEL":
                  this.callback.setOutputRemoteName(indexProperty, this.panelStatus.getOutputLabel(indexProperty));
                  break;
               case "ARMING_MODE_LABEL":
                  Character charMode = (Character)CustomizedArmingModes.ARMING_MODE_LABELS.get(indexProperty);
                  if (charMode != null) {
                     this.callback.setLabelArming(charMode, this.panelStatus.getArmingModeLabel(indexProperty));
                  }
                  break;
            }
         }
         else {
            String propertyName = property.getPropertyName();
            switch(propertyName) {
               case "CONNECTION_STATUS":
                  if (PanelStatus.PanelConnStatus.CONNECTED == property.getOldValue() && PanelStatus.PanelConnStatus.DISCONNECTED == property.getNewValue()) {
                     this.callback.connectionLost();
                  }
                  break;
               case "GLOBAL_ARMING":
                  this.callback.setArming(this.panelStatus.getGlobalArming());
                  break;
               case "SYSTEM_LABEL":
                  break;
               case "PARTITIONS":
                  this.callback.changePartitions(this.panelStatus.getPartitions());
                  break;
               case "ZONES":
                  List<Integer> zones = this.panelStatus.getZones();
                  this.callback.changeInputs(zones);
                  List<Integer> partitions = this.panelStatus.getPartitions();
                  for (int partitionId : partitions) {
                     this.callback.tagInputIntoPartition(partitionId, zones);
                  }
                  return;
               case "OUTPUTS":
                  this.callback.changeOutputs(this.panelStatus.getOutputs());
                  break;
               case "TROUBLES":
                  break;
            }
         }
      }
   }
}