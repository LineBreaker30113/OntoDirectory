package org.halim.sgui;

import org.halim.dlake.DataLakeManager;
import org.halim.hport.OntoDirectoryService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles the deployment and primary orchestration of the Swing UI Adapter.
 * It strictly communicates through the OntoDirectoryService Port and manages
 * the background Debouncer thread for atomic state persistence.
 */
public class ApplicationController {

public final OntoDirectoryService servicePort;
public GUI_RootPanel view;
public WorkspaceController wsModeller;

// --- Concurrency: Debouncer Engine ---
private ScheduledExecutorService backgroundSaver;
private OntoDirectoryService.DataLakeService activeLake;

public ApplicationController(OntoDirectoryService servicePort) {
	this.servicePort = servicePort;
	this.wsModeller = new WorkspaceController(this);
	
	// Initialize the UI strictly on the Event Dispatch Thread
	SwingUtilities.invokeLater(() -> {
		this.view = new GUI_RootPanel(this);
		this.servicePort.addOntoDirectoryServiceListener(new ServiceListenerImpl());
	});
}

public void deployTo(@NotNull JFrame applicationWindow) {
	applicationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	applicationWindow.add(view);
	applicationWindow.pack();
	applicationWindow.setLocationRelativeTo(null);
	applicationWindow.setVisible(true);
}

/**
 * Executes the lifecycle teardown of the active Lake.
 * Invoked by UI actions (like the Close button on the lake tab).
 */
public void dispatchLakeCloseRequest(OntoDirectoryService.DataLakeService lake) {
	if (this.activeLake == lake) {
		if (backgroundSaver != null && !backgroundSaver.isShutdown()) {
			backgroundSaver.shutdown(); // Immediately prevent further background ticks
		}
		// Flush synchronously before closing to guarantee zero data loss
		if (lake instanceof DataLakeManager dlm && dlm.isDirty()) {
			dlm.saveChanges();
		}
		this.activeLake = null;
	}
	servicePort.deleteLake(lake);
	wsModeller.triggerLakeRefresh(null);
}

// Inner class to handle callbacks from the Server Adapter Boundaries
private class ServiceListenerImpl implements OntoDirectoryService.OntoDirectoryServiceListener {
	@Override
	public void onGUI_Change() {
		SwingUtilities.invokeLater(() -> {
			if (view.centerPanel.welcomePage != null) {
				view.centerPanel.welcomePage.refreshLakes(); // CRITICAL: Sync the UI
			}
			view.leftSidebar.revalidate();
			view.revalidate();
			view.repaint();
		});
	}
	
	@Override
	public void onDataLakeLoad(OntoDirectoryService.DataLakeService dataLakeService) {
		// 1. Safely tear down existing debouncer if switching lakes
		if (backgroundSaver != null && !backgroundSaver.isShutdown()) {
			backgroundSaver.shutdown();
		}
		
		activeLake = dataLakeService;
		
		// 2. Initialize the Debouncer Worker Thread
		backgroundSaver = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "OntoDir-Debouncer-Thread");
			t.setDaemon(true); // Don't block JVM shutdown
			return t;
		});
		
		// 3. The Flush Contract: Check dirty flag every 3 seconds
		backgroundSaver.scheduleAtFixedRate(() -> {
			if (activeLake instanceof DataLakeManager dlm) {
				if (dlm.isDirty()) {
					dlm.saveChanges();
				}
			}
		}, 3, 3, TimeUnit.SECONDS);
		
		// 4. Update the Swing UI
		SwingUtilities.invokeLater(() -> {
			view.leftSidebar.registerDataLake(dataLakeService.getRootPath());
			view.centerPanel.showPage("WORKSPACE");
			wsModeller.triggerLakeRefresh(dataLakeService);
		});
	}
	
	@Override
	public void showBugReport(java.nio.file.Path reportFile) {
		// Implementation for bug report dialogs
	}
}
}