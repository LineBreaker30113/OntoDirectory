package org.halim.gui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;

public class WorkspacePanel extends JPanel {

// Tracks the logical order of currently visible panels
private final ArrayList<WorkPanelWrapper> allPanels = new ArrayList<>();
private final ArrayList<WorkPanelWrapper> activePanels = new ArrayList<>();
public final FullGUI owner;

public WorkspacePanel(FullGUI owner) {
	this.owner = owner;
	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
}

/** Adds a panel if it isn't already open */
public void addPanel(String title, JPanel view) {
	for (WorkPanelWrapper wrapper : activePanels) {
		if (wrapper.content == view) return;
	}
	
	WorkPanelWrapper wrapper = new WorkPanelWrapper(title, view, this);
	activePanels.add(wrapper);
	allPanels.add(wrapper);
	renderPanels();
}

/** Removes a panel */
public void closePanel(WorkPanelWrapper wrapper) {
	activePanels.remove(wrapper);
	renderPanels();
}

/** Removes a panel */
public void reopenPanel(WorkPanelWrapper wrapper) {
	activePanels.add(wrapper);
	renderPanels();
}

// --- Replace this method inside WorkspacePanel.java ---

public void panelToggled(String pname) {
	// THE FIX: Use findFirst().orElse(null) to prevent crashes if clicked during startup
	WorkPanelWrapper wrapper = allPanels.stream()
		  .filter(w -> w.title.equals(pname))
		  .findFirst()
		  .orElse(null);
	
	if (wrapper == null) return; // Panel not instantiated yet
	
	if(activePanels.contains(wrapper)) {
		closePanel(wrapper);
	} else {
		reopenPanel(wrapper);
	}
}

/** Shifts a panel left (-1) or right (1) */
public void shiftPanel(WorkPanelWrapper wrapper, int direction) {
	int currentIndex = activePanels.indexOf(wrapper);
	if (currentIndex == -1) return;
	
	int newIndex = currentIndex + direction;
	
	// Boundary checks
	if (newIndex < 0 || newIndex >= activePanels.size()) return;
	
	// Swap in the logical list
	Collections.swap(activePanels, currentIndex, newIndex);
	
	// Re-render the physical UI
	renderPanels();
}

/** Wipes the container and re-adds everything in the correct logical order */
private void renderPanels() {
	removeAll(); // Strip the Swing container
	for (WorkPanelWrapper wrapper : activePanels) {
		add(wrapper);
	}
	revalidate(); repaint();
}

}