package org.halim.sgui;

import javax.swing.*;
import java.awt.*;

public class GUI_RootPanel extends JPanel {

public final LeftSidebar leftSidebar;
public final CenterPanel centerPanel;
public final WorkspacePanel workspacePanel;
public final ApplicationController appController; // The Controller orchestrates, not the Port

public GUI_RootPanel(ApplicationController appController) {
	this.appController = appController;
	setLayout(new BorderLayout());
	
	// WorkspacePanel must be instantiated before CenterPanel adds it
	workspacePanel = new WorkspacePanel(this);
	leftSidebar = new LeftSidebar(this);
	centerPanel = new CenterPanel(this);
	
	// Pin them to the layout
	add(leftSidebar, BorderLayout.WEST);
	add(centerPanel, BorderLayout.CENTER);
	
	centerPanel.add(workspacePanel, "WORKSPACE");
}
}