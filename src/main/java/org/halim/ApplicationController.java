package org.halim;

import javafx.util.Pair;
import org.halim.dlake.DataLakeManager;
import org.halim.hport.OntoDirectoryService;
import org.halim.sgui.GUI_RootPanel;
import org.halim.sgui.WorkspacePanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/** Concerned with how the application GUI is managed. Later may decouple From GUI. */
public class ApplicationController {

public GUI_RootPanel view;
public WorkspaceController wsModeller;

// Memory map to hold and instantly swap between instantiated Data Lakes
private final Map<Path, DataLakeManager> activeLakes = new HashMap<>();

public ApplicationController() {
	SwingUtilities.invokeLater(() -> this.view = new GUI_RootPanel(this));
	this.wsModeller = new WorkspaceController(this);
}

public void deployTo(@NotNull JFrame applicationWindow) {
	applicationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	applicationWindow.add(view);
	applicationWindow.pack();
	applicationWindow.setLocationRelativeTo(null);
	applicationWindow.setVisible(true);
}

public WorkspacePanel getWSP() { return view.workspacePanel; }
public WorkspaceController getWSC() { return wsModeller; }

public void registerWSViews() {
	SwingUtilities.invokeLater(() -> {
		for(String name : WorkspaceController.vnames) {
			// Initialize all, but only pin Tree and Files to the visible Workspace
			if (name.equals(WorkspaceController.treeVN) || name.equals(WorkspaceController.filesVN)) {
				getWSP().addPanel(name, getWSC().getView(name));
			}
		}
		new Thread(() -> { view.revalidate(); view.repaint(); }).start();
		new Thread(() -> {
			try { Thread.sleep(1000); }
			catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			view.revalidate(); view.repaint();
		}).start();
	});
}

public void createDataLake(Path lakeRootPath_Identity) {
	SettingLogic.addDataLake(lakeRootPath_Identity);
	view.leftSidebar.registerDataLake(lakeRootPath_Identity);
}

// THE FIX: Instantiate the lake, set it active, and swap the UI CardLayout
public void dispatchLakeChooseRequest(Path identity) {
	DataLakeManager lake = activeLakes.computeIfAbsent(identity, DataLakeManager::new);
	wsModeller.setActiveLake(lake);
	view.centerPanel.showPage("WORKSPACE");
}

// THE FIX: Unload the lake and clear the UI if it was the active one
public void dispatchLakeCloseRequest(Path identity) {
	activeLakes.remove(identity);
	view.leftSidebar.unsignDataLake(identity);
	if (wsModeller.activeDataLake != null && wsModeller.activeDataLake.managerPath.equals(identity)) {
		wsModeller.setActiveLake(null);
		view.centerPanel.showPage("WELCOME"); // Return to landing page safely
	}
}

public void dispatchLakeLoadRequest(@NotNull Path lakePath) {
	view.leftSidebar.registerDataLake(lakePath);
	dispatchLakeChooseRequest(lakePath);
	view.leftSidebar.revalidate();
	view.leftSidebar.repaint();
}

}