package org.halim.sgui;

import org.halim.SettingLogic;
import org.halim.sgui.sglib.ScrollableListPanel;
import org.halim.sgui.sglib.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

public class CenterPanel extends JPanel {

public final GUI_RootPanel owner;
public final CardLayout layout;

public final JPanel welcomePage;
public WorkspacePanel getWorkspacePanel() { return owner.workspacePanel; }

public CenterPanel(GUI_RootPanel owner) {
	this.owner = owner;
	this.layout = new CardLayout();
	this.setLayout(layout);
	
	this.welcomePage = getWelcomePage();
	this.add(welcomePage, "WELCOME");
	
	// --- ADD THE NEW INSTRUCTIONS PAGE ---
	InstructionsPage instructionsPage = new InstructionsPage();
	this.add(instructionsPage, "INSTRUCTIONS");
}

private void launchLakeChooser() {
	JFileChooser fileChooser = new JFileChooser();
	fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	fileChooser.setDialogTitle("Select a Directory for your Data Lake");
	
	int result = fileChooser.showOpenDialog(this);
	if (result == JFileChooser.APPROVE_OPTION) {
		File selectedDir = fileChooser.getSelectedFile();
		Path lakePath = selectedDir.toPath();
		owner.ontoDirectoryService.createDataLake(lakePath);
	}
}

private @NotNull JPanel getWelcomePage() {
	JPanel wpage = new JPanel();
	wpage.setLayout(new BoxLayout(wpage, BoxLayout.X_AXIS));
	wpage.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
	wpage.setBackground(Utilities.WP_BG); // Applied Semantic Color
	
	// ==========================================
	// LEFT PANEL: Actions, Instructions & Anchor
	// ==========================================
	JPanel leftPanel = new JPanel();
	leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
	leftPanel.setAlignmentY(Component.TOP_ALIGNMENT);
	leftPanel.setOpaque(false);
	
	JLabel welcomeLabel = new JLabel("Welcome to Onto Directory", SwingConstants.LEFT);
	welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(28f));
	welcomeLabel.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	
	JButton createLakeBtn = new JButton("+ Create / Load Data Lake");
	Utilities.initButton(createLakeBtn);
	createLakeBtn.setBackground(Utilities.sideButtonBG);
	createLakeBtn.setForeground(Color.WHITE);
	createLakeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
	createLakeBtn.addActionListener(e -> launchLakeChooser());
	
	JLabel instructionsLabel = new JLabel("<html><br><b>Getting Started:</b><br><br>" +
		  "1. Load an existing Data Lake or initialize a new one.<br>" +
		  "2. Navigate your ontology hierarchy via the Sidebar.<br>" +
		  "3. Assign specific tags to files to organize your workspace.</html>");
	instructionsLabel.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
	instructionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	
	// UI UX Anchor: Fills the dead space at the bottom left
	JPanel anchorBox = new JPanel(new BorderLayout());
	anchorBox.setBackground(Utilities.ELEMENT_BG);
	anchorBox.setBorder(BorderFactory.createCompoundBorder(
		  BorderFactory.createLineBorder(Utilities.LIST_HEADER_BG, 1),
		  BorderFactory.createEmptyBorder(15, 15, 15, 15)
	));
	anchorBox.setMaximumSize(new Dimension(400, 100));
	anchorBox.setAlignmentX(Component.LEFT_ALIGNMENT);
	JLabel tipLabel = new JLabel("<html><b>Tip:</b> Use the Venn View to easily find files that exist at the intersection of multiple conceptual tags.</html>");
	tipLabel.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
	anchorBox.add(tipLabel, BorderLayout.CENTER);
	
	leftPanel.add(welcomeLabel);
	leftPanel.add(Box.createVerticalStrut(30));
	leftPanel.add(createLakeBtn);
	leftPanel.add(Box.createVerticalStrut(20));
	leftPanel.add(instructionsLabel);
	leftPanel.add(Box.createVerticalGlue()); // Pushes the anchor to the bottom
	leftPanel.add(anchorBox);
	
	// ==========================================
	// RIGHT PANEL: Scrollable Lists
	// ==========================================
	JPanel rightPanel = new JPanel();
	rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
	rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
	rightPanel.setOpaque(false);
	
	// --- 1. Data Lakes Scrollable List ---
	JPanel lakesHeader = new JPanel(new BorderLayout());
	lakesHeader.setBackground(Utilities.LIST_HEADER_BG);
	lakesHeader.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
	JLabel lakesHeaderLabel = new JLabel("Recent Data Lakes");
	lakesHeaderLabel.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	lakesHeader.add(lakesHeaderLabel, BorderLayout.WEST);
	
	// Let the scroll pane dictate its own width based on parent constraints
	ScrollableListPanel<JPanel> lakesList = new ScrollableListPanel<>(
		  lakesHeader, new Dimension(400, 250), new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
	lakesList.setInnerColor(Utilities.LIST_BG);
	lakesList.setSidePanelsColor(Utilities.LIST_HEADER_BG);
	
	for (Path lakePath : SettingLogic.dataLakes) {
		lakesList.addElement(createLakeCard(lakePath));
	}
	
	// --- 2. Settings Scrollable List ---
	JPanel settingsHeader = new JPanel(new BorderLayout());
	settingsHeader.setBackground(Utilities.LIST_HEADER_BG);
	settingsHeader.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
	JLabel settingsHeaderLabel = new JLabel("Global Settings");
	settingsHeaderLabel.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	settingsHeader.add(settingsHeaderLabel, BorderLayout.WEST);
	
	ScrollableListPanel<JPanel> settingsList = new ScrollableListPanel<>(
		  settingsHeader, new Dimension(400, 150), new Dimension(Integer.MAX_VALUE, 250));
	settingsList.setInnerColor(Utilities.LIST_BG);
	settingsList.setSidePanelsColor(Utilities.LIST_HEADER_BG);
	
	settingsList.addElement(createSettingToggle("Dark Theme", SettingLogic.isDarkTheme,
		  e -> SettingLogic.setSystemTheme(((JCheckBox)e.getSource()).isSelected())));
	settingsList.addElement(createSettingToggle("Native System File Chooser", SettingLogic.isSystemFileChooser,
		  e -> SettingLogic.setSystemFileChooser(((JCheckBox)e.getSource()).isSelected())));
	
	rightPanel.add(lakesList);
	rightPanel.add(Box.createVerticalStrut(20));
	rightPanel.add(settingsList);
	
	// Assemble the page
	wpage.add(leftPanel);
	wpage.add(Box.createHorizontalStrut(40));
	wpage.add(rightPanel);
	
	return wpage;
}

/** UI UX Component: Creates a beautiful, text-wrapping card for a Data Lake */
private @NotNull JPanel createLakeCard(@NotNull Path lakePath) {
	JPanel card = new JPanel(new BorderLayout(15, 0));
	card.setBackground(Utilities.ELEMENT_BG);
	card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
	JPanel textPanel = new JPanel();
	textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
	textPanel.setOpaque(false);
	
	// Primary Information: The Folder Name
	String lakeName = lakePath.getFileName().toString();
	JLabel nameLabel = new JLabel(lakeName);
	nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
	nameLabel.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	
	// Secondary Information: The absolute path wrapped safely
	JTextArea pathArea = new JTextArea(lakePath.toAbsolutePath().toString());
	pathArea.setWrapStyleWord(true);
	pathArea.setLineWrap(true);
	pathArea.setOpaque(false);
	pathArea.setEditable(false);
	pathArea.setFocusable(false);
	pathArea.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
	pathArea.setFont(pathArea.getFont().deriveFont(11f));
	pathArea.setAlignmentX(Component.LEFT_ALIGNMENT);
	
	textPanel.add(nameLabel);
	textPanel.add(Box.createVerticalStrut(4));
	textPanel.add(pathArea);
	
	card.add(textPanel, BorderLayout.CENTER);
	
	// Action Button
	JButton openBtn = new JButton("Open");
	Utilities.initButton(openBtn);
	openBtn.setBackground(Utilities.SLPEB_Idle);
	openBtn.setForeground(Color.BLACK);
	openBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
	openBtn.addActionListener(e -> owner.ontoDirectoryService.dispatchLakeLoadRequest(lakePath));
	
	// Wrapper for the button to keep it centered vertically
	JPanel btnWrapper = new JPanel(new GridBagLayout());
	btnWrapper.setOpaque(false);
	btnWrapper.add(openBtn);
	
	card.add(btnWrapper, BorderLayout.EAST);
	
	return card;
}

/** UI UX Component: Standardized row for boolean settings */
private JPanel createSettingToggle(String label, boolean initialState, java.awt.event.ActionListener action) {
	JPanel row = new JPanel(new BorderLayout());
	row.setBackground(Utilities.ELEMENT_BG);
	row.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
	JCheckBox toggle = new JCheckBox(label, initialState);
	toggle.setOpaque(false);
	toggle.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	toggle.addActionListener(action);
	
	row.add(toggle, BorderLayout.CENTER);
	return row;
}

public void showPage(String pageName) {
	layout.show(this, pageName);
}
}