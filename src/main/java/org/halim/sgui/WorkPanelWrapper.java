package org.halim.sgui;

import org.halim.sgui.library.Utilities;

import javax.swing.*;
import java.awt.*;

public class WorkPanelWrapper extends JPanel {

public final String title;
public final WorkspacePanel parentWorkspace;
public final JPanel content;
public final JPanel titlePanel;

public WorkPanelWrapper(String title, JPanel content, WorkspacePanel parentWorkspace) {
	this.title = title;
	this.content = content;
	this.parentWorkspace = parentWorkspace;
	this.titlePanel = new JPanel() {
	
	};
	
	setLayout(new BorderLayout());
	setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY)); // Right separator
	
	// Build the Header
	JPanel header = new JPanel(new BorderLayout());
	header.setBackground(new Color(45, 45, 45)); // Slightly lighter than background
	header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
	
	JLabel titleLabel = new JLabel(title);
	header.add(titleLabel, BorderLayout.WEST);
	
	// Build Controls
	JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
	controls.setOpaque(false);
	
	JButton leftBtn = new JButton("<"); // Replace with 16x16 icon later
	JButton rightBtn = new JButton(">"); // Replace with 16x16 icon later
	
	Utilities.initButton(leftBtn);
	Utilities.initButton(rightBtn);
	
	leftBtn.addActionListener(e -> parentWorkspace.shiftPanel(this, -1));
	rightBtn.addActionListener(e -> parentWorkspace.shiftPanel(this, 1));
	
	controls.add(leftBtn);
	controls.add(rightBtn);
	header.add(controls, BorderLayout.EAST);
	
	add(header, BorderLayout.NORTH);
	add(content, BorderLayout.CENTER);
}
}