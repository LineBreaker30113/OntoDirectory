package org.halim.gui;

import org.halim.WorkspaceController;
import org.halim.gui.library.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LeftSidebar extends JPanel {

private final FullGUI owner;
private boolean isCollapsed = false;

// The three distinct zones
private final JPanel anchorSection;
private final JPanel loadedLakesSection;
private final JPanel workspaceSection;

// A container specifically to hold the lake rows so we can hide them all at once
private JPanel lakesContainer;

// Track the header label to manipulate its text/icon on collapse
private JLabel dataLakeHeaderLabel;
private Icon lockersIcon;
private Icon databaseIcon;

// Keep lists of components to toggle their text visibility
private final ArrayList<JButton> sidebarButtons = new ArrayList<>();
private final List<DataLakeHeaderTab> lakeItems = new ArrayList<>(); // Unified group

public int expanded = 260, contracted = 56;

public LeftSidebar(FullGUI owner) {
	this.owner = owner;
	setBackground(Utilities.sideBarBG);
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	setSidebarWidth(expanded);
	
	anchorSection = buildAnchorSection();
	anchorSection.setAlignmentX(Component.RIGHT_ALIGNMENT);
	loadedLakesSection = buildLoadedLakesSection();
	loadedLakesSection.setAlignmentX(Component.RIGHT_ALIGNMENT);
	workspaceSection = buildWorkspaceSection();
	workspaceSection.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
	// Add sections with flexible spacing (Glue) to push them apart
	add(anchorSection);
	add(Box.createVerticalStrut(48));
	add(loadedLakesSection);
	add(Box.createVerticalGlue());
	add(workspaceSection);
	add(Box.createVerticalStrut(40));
}

private void setSidebarWidth(int width) {
	Dimension dim = new Dimension(width, Short.MAX_VALUE);
	setPreferredSize(dim);
	setMinimumSize(dim);
	setMaximumSize(dim);
}

public void toggleCollapse() {
	isCollapsed = !isCollapsed;
	setSidebarWidth(isCollapsed ? contracted : expanded);
	
	// 1. Handle main buttons
	for (JButton btn : sidebarButtons) {
		if (isCollapsed) {
			btn.setText("");
			Icon cIcon = (Icon) btn.getClientProperty("collapsedIcon");
			if (cIcon != null) btn.setIcon(cIcon);
			btn.setHorizontalAlignment(SwingConstants.CENTER);
		} else {
			btn.setText(btn.getActionCommand());
			Icon eIcon = (Icon) btn.getClientProperty("expandedIcon");
			if (eIcon != null) btn.setIcon(eIcon);
			btn.setHorizontalAlignment(SwingConstants.TRAILING);
		}
	}
	
	// 2. Isolate Data Lakes Collapse Logic
	if (dataLakeHeaderLabel != null && lakesContainer != null) {
		if (isCollapsed) {
			dataLakeHeaderLabel.setText("");
		} else {
			dataLakeHeaderLabel.setText("DATA LAKES");
		}
	}
	
	for (DataLakeHeaderTab item : lakeItems) {
		JButton btn = item.mainButton;
		JButton cbtn = item.closeButton;
		
		if (isCollapsed) {
			btn.setText("");
			btn.setIcon((Icon) btn.getClientProperty("collapseIcon"));
			btn.setHorizontalAlignment(SwingConstants.CENTER);
			cbtn.setVisible(false);
		} else {
			btn.setText(btn.getActionCommand());
			btn.setIcon(null);
			btn.setHorizontalAlignment(SwingConstants.LEFT);
			cbtn.setVisible(true);
		}
	}
	
	revalidate();
	repaint();
}

// --- Section Builders ---

private @NotNull JPanel buildAnchorSection() {
	JPanel panel = new JPanel();
	panel.setBackground(getBackground().brighter());
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	
	JButton collapseBtn = registerButton("Toggle Sidebar", "arrow-line-left.svg", "caret-right.svg");
	collapseBtn.addActionListener(e -> toggleCollapse());
	
	JButton welcomeBtn = registerButton("Welcome Page", "house.svg");
	welcomeBtn.addActionListener(a -> owner.centerPanel.showPage("WELCOME"));
	JButton instructionsBtn = registerButton("Instruction Manual", "notebook.svg");
	instructionsBtn.addActionListener(a -> owner.centerPanel.showPage("INSTRUCTIONS"));
	
	panel.add(collapseBtn);
	panel.add(Box.createVerticalStrut(8));
	panel.add(welcomeBtn);
	panel.add(Box.createVerticalStrut(8));
	panel.add(instructionsBtn);
	return panel;
}

private @NotNull JPanel buildLoadedLakesSection() {
	JPanel panel = new JPanel();
	panel.setBackground(getBackground().darker());
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	panel.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
	// 1. The Header
	JPanel headerPanel = new JPanel(new BorderLayout());
	headerPanel.setBackground(Color.BLACK);
	headerPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
	Dimension headerSize = new Dimension(Integer.MAX_VALUE, 56);
	headerPanel.setMinimumSize(new Dimension(56, 56));
	headerPanel.setPreferredSize(new Dimension(expanded, 56));
	headerPanel.setMaximumSize(headerSize);
	lockersIcon = Utilities.loadSVGIcon("icons/lockers.svg", 40, 40, Utilities.GOLDEN_COLOR);
	databaseIcon = Utilities.loadSVGIcon("icons/database.svg", 40, 40, Utilities.GOLDEN_COLOR);
	
	dataLakeHeaderLabel = new JLabel("DATA LAKES", lockersIcon, SwingConstants.CENTER);
	dataLakeHeaderLabel.setForeground(Utilities.GOLDEN_COLOR);
	dataLakeHeaderLabel.setIconTextGap(8);
	dataLakeHeaderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
	dataLakeHeaderLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
	dataLakeHeaderLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
	dataLakeHeaderLabel.setMinimumSize(new Dimension(56, 56));
	dataLakeHeaderLabel.setHorizontalTextPosition(SwingConstants.LEFT);
	
	headerPanel.setLayout(new BorderLayout());
	headerPanel.add(dataLakeHeaderLabel, BorderLayout.EAST);
	
	// 2. The Container for Lakes
	lakesContainer = new JPanel();
	lakesContainer.setOpaque(false);
	lakesContainer.setLayout(new BoxLayout(lakesContainer, BoxLayout.Y_AXIS));
	lakesContainer.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
//	LakeItemGroup ktuProject = new LakeItemGroup("KTÜ Projects");
//	lakeItems.add(ktuProject);
//	lakesContainer.add(ktuProject.container);
//
//	lakesContainer.add(Box.createVerticalStrut(8));
//
//	LakeItemGroup personalFiles = new LakeItemGroup("Personal Files");
//	lakeItems.add(personalFiles);
//	lakesContainer.add(personalFiles.container);
	
	panel.add(headerPanel);
	panel.add(Box.createVerticalStrut(8));
	panel.add(lakesContainer);
	
	return panel;
}

public void registerDataLake(Path lakeRootPath) {
	for (DataLakeHeaderTab tab : lakeItems) {
		if (tab.identity.equals(lakeRootPath)) return;
	}
	
	DataLakeHeaderTab nlake = new DataLakeHeaderTab(this, lakeRootPath);
	lakeItems.add(nlake);
	lakesContainer.add(nlake.container);
	revalidate();
	repaint();
}

private @NotNull JPanel buildWorkspaceSection() {
	JPanel panel = new JPanel();
	panel.setBackground(getBackground().brighter());
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	
	JButton treeBtn = registerButton("Tree View", "tree-view.svg");
	treeBtn.addActionListener(e -> owner.mac.getWSP().panelToggled(WorkspaceController.treeVN));
	
	JButton filesBtn = registerButton("Files View", "files.svg");
	filesBtn.addActionListener(e -> owner.mac.getWSP().panelToggled(WorkspaceController.filesVN));
	
	JButton notesBtn = registerButton("Notes View", "note.svg");
	notesBtn.addActionListener(e -> owner.mac.getWSP().panelToggled(WorkspaceController.notesVN));
	
	JButton graphBtn = registerButton("Graph View", "graph.svg");
	graphBtn.addActionListener(e -> owner.mac.getWSP().panelToggled(WorkspaceController.graphVN));
	
	JButton vennBtn = registerButton("Venn View", "intersect-three.svg");
	vennBtn.addActionListener(e -> owner.mac.getWSP().panelToggled(WorkspaceController.vennVN));
	
	panel.add(treeBtn);
	panel.add(Box.createVerticalStrut(8));
	panel.add(filesBtn);
	panel.add(Box.createVerticalStrut(8));
	panel.add(notesBtn);
	panel.add(Box.createVerticalStrut(8));
	panel.add(graphBtn);
	panel.add(Box.createVerticalStrut(8));
	panel.add(vennBtn);
	
	return panel;
}

// THE FIX: Actually locate and destroy the UI component for the closed lake
public void unsignDataLake(Path lakeRootPath) {
	DataLakeHeaderTab targetTab = null;
	for (DataLakeHeaderTab tab : lakeItems) {
		if (tab.identity.equals(lakeRootPath)) {
			targetTab = tab;
			break;
		}
	}
	
	if (targetTab != null) {
		lakeItems.remove(targetTab);
		lakesContainer.remove(targetTab.container);
		revalidate();
		repaint();
	}
}

private @NotNull JButton registerButton(String name, String iconName) {
	return registerButton(name, iconName, iconName);
}
private @NotNull JButton registerButton(String name, String expandedIconName, String collapsedIconName) {
	JButton btn = new JButton(name);
	btn.setActionCommand(name);
	btn.setAlignmentX(Component.RIGHT_ALIGNMENT);
	btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
	btn.setMinimumSize(new Dimension(56, 56));
	btn.setPreferredSize(new Dimension(56, 56));
	
	btn.setHorizontalAlignment(SwingConstants.RIGHT);
	btn.setHorizontalTextPosition(SwingConstants.LEFT);
	btn.setIconTextGap(8);
	
	Icon expandedIcon = Utilities.loadSVGIcon("icons/" + expandedIconName, 40, 40);
	Icon collapsedIcon = Utilities.loadSVGIcon("icons/" + collapsedIconName, 40, 40);
	
	if (expandedIcon != null) {
		btn.setIcon(expandedIcon);
	}
	
	btn.putClientProperty("expandedIcon", expandedIcon);
	btn.putClientProperty("collapsedIcon", collapsedIcon);
	
	Utilities.initButton(btn);
	
	sidebarButtons.add(btn);
	return btn;
}

// --- Static Subclass for Data Lake Items ---

public static class DataLakeHeaderTab {
	public final LeftSidebar lsb;
	public final JPanel container;
	public final JButton mainButton;
	public final JButton closeButton;
	public final Component spacer;
	public final Path identity;
	public final String lakeName;
	
	public DataLakeHeaderTab(LeftSidebar leftSidebar, @NotNull Path lakeRootPath) {
		this.lsb = leftSidebar;
		identity = lakeRootPath;
		lakeName = lakeRootPath.getFileName().toString();
		spacer = Box.createVerticalStrut(8);
		
		container = new JPanel(new BorderLayout());
		container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		container.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		// Init Main Button
		mainButton = new JButton(lakeName);
		mainButton.addActionListener(__ -> lsb.owner.mac.dispatchLakeChooseRequest(identity));
		mainButton.setActionCommand(lakeName);
		mainButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		mainButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		mainButton.setMinimumSize(new Dimension(56, 56));
		mainButton.setPreferredSize(new Dimension(56, 56));
		mainButton.setLayout(new BorderLayout());
		mainButton.setHorizontalAlignment(SwingConstants.LEFT);
		mainButton.setMargin(new Insets(0, 0, 0, 40));
		
		Icon collapsedIcon = Utilities.loadSVGIcon("icons/folder.svg", 40, 40);
		mainButton.putClientProperty("collapseIcon", collapsedIcon);
		Utilities.initButton(mainButton);
		
		// Init Close Button
		closeButton = new JButton();
		closeButton.addActionListener(__ -> lsb.owner.mac.dispatchLakeCloseRequest(identity));
		Icon xIconGray = Utilities.loadSVGIcon("icons/x-square.svg", 24, 24);
		Icon xIconRed = Utilities.loadSVGIcon("icons/x-square.svg", 24, 24);
		
		if (xIconRed instanceof com.formdev.flatlaf.extras.FlatSVGIcon) {
			((com.formdev.flatlaf.extras.FlatSVGIcon) xIconRed).setColorFilter(
				  new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(color -> Color.RED)
			);
		}
		
		if (xIconGray != null) {
			closeButton.setIcon(xIconGray);
		} else {
			closeButton.setText("X");
		}
		
		Utilities.initButton(closeButton);
		closeButton.setContentAreaFilled(false);
		closeButton.setOpaque(false);
		
		closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				if (xIconRed != null) closeButton.setIcon(xIconRed);
			}
			@Override
			public void mouseExited(java.awt.event.MouseEvent evt) {
				if (xIconGray != null) closeButton.setIcon(xIconGray);
			}
		});
		
		closeButton.addActionListener(e -> {
			System.out.println("Closing lake: " + lakeName);
		});
		
		// Assemble
		mainButton.add(closeButton, BorderLayout.EAST);
		container.add(mainButton, BorderLayout.CENTER);
	}
}
}