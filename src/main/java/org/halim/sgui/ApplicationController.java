package org.halim.sgui;

import org.halim.hport.OntoDirectoryService;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;

/**
 * Handles the deployment and primary orchestration of the Swing UI Adapter.
 * It strictly communicates through the OntoDirectoryService Port.
 */
public class ApplicationController {

public final OntoDirectoryService servicePort;
public GUI_RootPanel view;
public WorkspaceController wsModeller;

public ApplicationController(OntoDirectoryService servicePort) {
	this.servicePort = servicePort;
	this.wsModeller = new WorkspaceController(this);
	
	// Initialize the UI on the Event Dispatch Thread
	SwingUtilities.invokeLater(() -> {
		this.view = new GUI_RootPanel(servicePort);
		// Register this controller as a listener to backend changes
		this.servicePort.addOntoDirectoryServiceListener(new ServiceListenerImpl());
	});
}

public void deployTo(@NotNull JFrame applicationWindow) {
	applicationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	applicationWindow.add(view);
	applicationWindow.pack();
	applicationWindow.setLocationRelativeTo(null);
	applicationWindow.setVisible(true);
	wsModeller.registerWSViews(); // Pin initial views
}

// Inner class to handle callbacks from the Server
private class ServiceListenerImpl implements OntoDirectoryService.OntoDirectoryServiceListener {
	@Override
	public void onGUI_Change() {
		SwingUtilities.invokeLater(() -> {
			view.revalidate();
			view.repaint();
		});
	}
	
	@Override
	public void onDataLakeLoad(OntoDirectoryService.DataLakeService dataLakeService) {
		SwingUtilities.invokeLater(() -> {
			view.leftSidebar.registerDataLake(dataLakeService.getRootPath());
			view.centerPanel.showPage("WORKSPACE");
			wsModeller.triggerLakeRefresh(dataLakeService);
		});
	}
}
}