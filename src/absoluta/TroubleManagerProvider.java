package absoluta;

import com.google.common.collect.ImmutableList;
import absoluta.spi.TroubleManagerProvider;

import absoluta.connection.PanelStatus;
import absoluta.connection.Trouble;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

class AbsolutaTroubleManagerProvider implements TroubleManagerProvider, PropertyChangeListener {
    private final AbsolutaPanelProvider panelProvider;
    private final ChangeSupport changeSupport = new ChangeSupport(this);
    private ImmutableList<String> troubles = ImmutableList.of();
    private boolean troublesInMemory; // True se almeno un guasto è solo in memoria
    private boolean connected;        // True se il pannello è connesso

    AbsolutaTroubleManagerProvider(AbsolutaPanelProvider panelProvider) {
        this.panelProvider = Objects.requireNonNull(panelProvider);
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        this.changeSupport.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        this.changeSupport.removeChangeListener(listener);
    }

    @Override
    public synchronized ImmutableList<String> getTroubles() {
        return this.troubles;
    }

    @Override
    public void clean() {
        if (this.troublesInMemory && this.connected){
            this.panelProvider.cleanTroubles();
        }
    }

    @Override
    public synchronized void propertyChange(PropertyChangeEvent evt) {
        PanelStatus panelStatus = (PanelStatus) evt.getSource();
        ImmutableList<String> oldTroubles = this.troubles;
        String propertyName = evt.getPropertyName();

        switch (propertyName) {
            case "TROUBLES":
            SortedSet<String> troubleSet = new TreeSet<>();
            this.troublesInMemory = false;

            // Aggiorna la lista dei guasti e verifica se almeno uno è solo in memoria
            for (Object obj : (Iterable<?>) evt.getNewValue()) {
                Trouble trouble = (Trouble) obj;
                troubleSet.add(trouble.toLocalizedString(panelStatus));
                this.troublesInMemory |= trouble.isInMemory();
            }
            this.troubles = ImmutableList.copyOf(troubleSet);
            break;

            case "CONNECTION_STATUS":
            PanelStatus.PanelConnStatus connStatus = (PanelStatus.PanelConnStatus) evt.getNewValue();
            this.connected = connStatus == PanelStatus.PanelConnStatus.CONNECTED;
            break;
        }

        // Notifica i listener solo se c'è stato un cambiamento effettivo
        if (!oldTroubles.equals(this.troubles)) {
            this.changeSupport.fireChange();
        }
    }
}