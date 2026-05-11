package org.halim.sgui;

import org.halim.ApplicationController;

import javax.swing.*;
import java.awt.*;

public class FullGUI extends JPanel {

public final LeftSidebar leftSidebar;
public final CenterPanel centerPanel;
public final WorkspacePanel workspacePanel;
public final ApplicationController mac;

public FullGUI(ApplicationController owner) {
	mac = owner;
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