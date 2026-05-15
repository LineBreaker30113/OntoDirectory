package org.halim.sgui;

import org.halim.hport.OntoDirectoryService;
import org.halim.sgui.sglib.ContentView;
import org.halim.sgui.sglib.HierarchyView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceController {

public static final String filesVN = "filesV", notesVN = "notesV",
	  treeVN = "treeV", graphVN = "graphV", vennVN = "vennV";
public static final String[] vnames = new String[]{filesVN, notesVN, treeVN, graphVN, vennVN};

public final ApplicationController owner;

private HierarchyView treeView, graphView, vennView;
private ContentView filesView, notesView;

// Use wildcard/generics or custom interfaces here instead of domain listeners
private final List<HierarchyView> hierarchyViews = new ArrayList<>();
private final List<ContentView> contentViews = new ArrayList<>();

public WorkspaceController(ApplicationController owner) {
	this.owner = owner;
	SwingUtilities.invokeLater(this::initViews);
}

void initViews() {
	treeView = new HierarchyTreeView(owner); // Inject owner so it can access the servicePort
	graphView = new HierarchyGraphView();    // Stubs
	vennView = new HierarchyVennView();      // Stubs
	
	hierarchyViews.add(treeView);
	hierarchyViews.add(graphView);
	hierarchyViews.add(vennView);
	
	filesView = new FilesContentView();      // Stubs
	notesView = new NotesContentView();      // Stubs
	
	contentViews.add(filesView);
	contentViews.add(notesView);
}

public JPanel getView(@NotNull String name) {
	return switch (name) {
		case filesVN -> filesView;
		case notesVN -> notesView;
		case treeVN -> treeView;
		case graphVN -> graphView;
		case vennVN -> vennView;
		default -> null;
	};
}

public void registerWSViews() {
	SwingUtilities.invokeLater(() -> {
		for (String name : vnames) {
			if (name.equals(treeVN) || name.equals(filesVN)) {
				owner.view.workspacePanel.addPanel(name, getView(name));
			}
		}
	});
}

public void triggerLakeRefresh(OntoDirectoryService.DataLakeService lakeService) {
	// Distribute the Reading/Managing services to the views without exposing the Domain model directly
	for (HierarchyView view : hierarchyViews) {
		view.refreshFromService(lakeService.getOntologyReadingService());
	}
}
}