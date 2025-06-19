package cms.device.api;

import java.util.Objects;
import java.util.function.Consumer;
import org.openide.util.ChangeSupport;

import plugin.absoluta.connection.PanelStatus;

public class Output {
   private String remoteName;
   private boolean enabled = true;
   private PanelStatus.outputStatus status;
   private final ChangeSupport changeSupport;

   Output(String var2, Consumer<PanelStatus.outputAction> var3) {
      this.status = PanelStatus.outputStatus.UNKNOWN;
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

   public boolean isEnabled() {
      return this.enabled;
   }

   void setEnabled(boolean var1) {
      if (this.enabled != var1) {
         this.enabled = var1;
         this.changeSupport.fireChange();
      }
   }

   public PanelStatus.outputStatus getStatus() {
      return this.status;
   }

   public void setStatus(PanelStatus.outputStatus var1) {
      if (this.status != Objects.requireNonNull(var1)) {
         this.status = var1;
         this.changeSupport.fireChange();
      }

   }

   void fireChange() {
      this.changeSupport.fireChange();
   }
}
