package absoluta.spi;

import com.google.common.collect.ImmutableList;
import javax.swing.event.ChangeListener;

public interface TroubleManagerProvider {
   void addChangeListener(ChangeListener listener);

   void removeChangeListener(ChangeListener listener);

   ImmutableList<String> getTroubles();

   void clean();
}
