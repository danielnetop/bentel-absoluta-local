package cms.device.api;

import java.util.Objects;

import org.openide.util.ChangeSupport;

import plugin.absoluta.connection.PanelStatus;

public class Input {
   private String remoteName;
   private PanelStatus.inputStatus status;
   private final ChangeSupport changeSupport;

   public Input(Panel var1) {
      this.status = PanelStatus.inputStatus.OK;
      this.changeSupport = new ChangeSupport(this);
   }

   public String getRemoteName() {
      return this.remoteName;
   }

      void setRemoteName(String name) {
      String clean = name != null && !name.trim().isEmpty() ? name.trim() : null;
      if (!Objects.equals(this.remoteName, clean)) {
         this.remoteName = clean;
         this.changeSupport.fireChange();
      }
   }

   public PanelStatus.inputStatus getStatus() {
      return this.status;
   }

   public void setStatus(PanelStatus.inputStatus var1) {
      if (this.status != var1) {
         this.status = var1;
         this.changeSupport.fireChange();
      }
   }

   public void fireChange() {
      this.changeSupport.fireChange();
   }
}
