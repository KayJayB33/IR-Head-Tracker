package pl.edu.pk.mech.tracking;

import javafx.application.Platform;
import pl.edu.pk.mech.gui.controller.MainWindowController;
import pl.edu.pk.mech.model.Model;

public abstract class TrackingThread extends Thread {

    protected final Model model = Model.getInstance();
    protected final MainWindowController controller;
    protected final ITracker tracker;
    protected boolean isPlaying = false;

    public TrackingThread(final MainWindowController controller, final ITracker tracker) {
        this.controller = controller;
        this.tracker = tracker;
    }

    public void stopCapturing() {
        Platform.runLater(controller::updateInterface);
        isPlaying = false;
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
