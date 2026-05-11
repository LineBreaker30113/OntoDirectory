package org.halim.sgui;

import org.halim.hport.OntoDirectoryService;

import javax.swing.*;
import java.awt.*;

public class GUI_RootPanel extends JPanel {

public final LeftSidebar leftSidebar;
public final CenterPanel centerPanel;
public final WorkspacePanel workspacePanel;
public final OntoDirectoryService ontoDirectoryService;

public GUI_RootPanel(OntoDirectoryService service) {
	ontoDirectoryService = service;
	setLayout(new BorderLayout());
	leftSidebar = new LeftSidebar(this);
	centerPanel = new CenterPanel(this);
	workspacePanel = new WorkspacePanel(this);
	
	// Pin them to the layout
	add(leftSidebar, BorderLayout.WEST);
	add(centerPanel, BorderLayout.CENTER);
	
	
	centerPanel.add(workspacePanel, "WORKSPACE");
}

}