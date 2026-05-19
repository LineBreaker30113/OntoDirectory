package org.halim.sgui;

import org.halim.dlake.OntologyClass;
import org.halim.pd.OntoDirectoryService;
import org.halim.sgui.sglib.WorkspaceListener;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceController {

public final ApplicationController owner;

// Observers that need domain updates (the views managed by WorkspacePanel)
private final List<WorkspaceListener> workspaceListeners = new ArrayList<>();

public WorkspaceController(ApplicationController owner) {
	this.owner = owner;
}

/** Registers a view to receive domain state updates */
public void registerListener(WorkspaceListener listener) {
	if (!workspaceListeners.contains(listener)) {
		workspaceListeners.add(listener);
	}
}

public void triggerLakeRefresh(OntoDirectoryService.DataLakeService lakeService) {
	for (WorkspaceListener listener : workspaceListeners) {
		listener.onSelectedLakeChange(lakeService);
	}
}

public void broadcastTagSelection(int tagIdentity) {
	OntoDirectoryService.DataLakeService activeLake = owner.servicePort.getActiveDataLake();
	if (activeLake != null) {
		// Retrieve the OntologyClass aggregate from the Domain via Port
		OntologyClass targetClass = activeLake.getOntologyReadingService().getClassFromIdentity(tagIdentity);
		
		// Broadcast the full object to listeners handling the UI
		for (WorkspaceListener listener : workspaceListeners) {
			listener.onSelectedClassChange(targetClass);
		}
	}
}
}