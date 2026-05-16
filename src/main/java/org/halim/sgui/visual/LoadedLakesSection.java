package org.halim.sgui.visual;

import org.halim.hport.OntoDirectoryService;
import org.halim.sgui.LeftSidebar;
import org.halim.sgui.sglib.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LoadedLakesSection extends JPanel {

private final LeftSidebar parent;
private final JPanel lakesContainer;
private final JLabel dataLakeHeaderLabel;
private final List<DataLakeHeaderTab> lakeItems = new ArrayList<>();

public LoadedLakesSection(LeftSidebar parent) {
	this.parent = parent;
	setBackground(parent.getBackground().darker());
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	
	JPanel headerPanel = new JPanel(new BorderLayout());
	headerPanel.setBackground(Color.BLACK);
	headerPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
	headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
	headerPanel.setPreferredSize(new Dimension(parent.expanded, 56));
	
	Icon lockersIcon = Utilities.loadSVGIcon("icons/lockers.svg", 40, 40, Utilities.GOLDEN_COLOR);
	
	dataLakeHeaderLabel = new JLabel("DATA LAKES", lockersIcon, SwingConstants.CENTER);
	dataLakeHeaderLabel.setForeground(Utilities.GOLDEN_COLOR);
	dataLakeHeaderLabel.setIconTextGap(8);
	dataLakeHeaderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
	dataLakeHeaderLabel.setHorizontalTextPosition(SwingConstants.LEFT);
	headerPanel.add(dataLakeHeaderLabel, BorderLayout.EAST);
	
	lakesContainer = new JPanel();
	lakesContainer.setOpaque(false);
	lakesContainer.setLayout(new BoxLayout(lakesContainer, BoxLayout.Y_AXIS));
	lakesContainer.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
	add(headerPanel);
	add(Box.createVerticalStrut(8));
	add(lakesContainer);
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

public void updateCollapseState(boolean isCollapsed) {
	dataLakeHeaderLabel.setText(isCollapsed ? "" : "DATA LAKES");
	
	for (DataLakeHeaderTab item : lakeItems) {
		JButton btn = item.mainButton;
		JPanel actionPanel = item.actionBar;
		
		if (isCollapsed) {
			btn.setText("");
			btn.setIcon((Icon) btn.getClientProperty("collapseIcon"));
			btn.setHorizontalAlignment(SwingConstants.CENTER);
			actionPanel.setVisible(false);
		} else {
			btn.setText(btn.getActionCommand());
			btn.setIcon(null);
			btn.setHorizontalAlignment(SwingConstants.LEFT);
			actionPanel.setVisible(true);
		}
	}
}

private static class DataLakeHeaderTab {
	public final LoadedLakesSection section;
	public final JPanel container;
	public final JButton mainButton;
	public final JPanel actionBar;
	public final Path identity;
	public final String lakeName;
	
	public DataLakeHeaderTab(LoadedLakesSection section, @NotNull Path lakeRootPath) {
		this.section = section;
		this.identity = lakeRootPath;
		this.lakeName = lakeRootPath.getFileName().toString();
		
		container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.setOpaque(false);
		container.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		// --- 1. Main Lake Selection Button ---
		mainButton = new JButton(lakeName);
		mainButton.addActionListener(__ -> section.parent.owner.appController.servicePort.dispatchLakeChooseRequest(identity));
		mainButton.setActionCommand(lakeName);
		mainButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		mainButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		mainButton.setPreferredSize(new Dimension(56, 40));
		mainButton.setHorizontalAlignment(SwingConstants.LEFT);
		mainButton.setMargin(new Insets(0, 10, 0, 0));
		
		Icon collapsedIcon = Utilities.loadSVGIcon("icons/folder.svg", 40, 40);
		mainButton.putClientProperty("collapseIcon", collapsedIcon);
		Utilities.initButton(mainButton);
		
		// --- 2. Action Bar (Close, Import, Export) ---
		actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		actionBar.setOpaque(false);
		actionBar.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JButton exportBtn = createActionBtn("Export", "export.svg", Utilities.SLPEB_Hover);
		JButton importBtn = createActionBtn("Import", "arrow-square-in.svg", Utilities.SLPEB_Hover);
		JButton closeBtn  = createActionBtn("Close", "x-square.svg", new Color(255, 100, 100));
		
		// THE FIX: Defer deletion to EDT tail to bypass synchronous UI rebuilds
		closeBtn.addActionListener(e -> {
			int res = JOptionPane.showConfirmDialog(section.parent.owner, "Save and close Data Lake '" + lakeName + "'?", "Confirm Close", JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				section.parent.owner.appController.servicePort.dispatchLakeChooseRequest(identity);
				
				SwingUtilities.invokeLater(() -> {
					OntoDirectoryService.DataLakeService lake = section.parent.owner.appController.servicePort.getActiveDataLake();
					if (lake != null && lake.getRootPath().equals(identity)) {
						lake.saveChanges();
						section.parent.owner.appController.servicePort.deleteLake(lake);
						section.parent.owner.appController.wsModeller.triggerLakeRefresh(null);
					}
					section.unsignDataLake(identity);
				});
			}
		});
		
		// Import Logic
		importBtn.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) { handle(e); }
			@Override public void mouseReleased(MouseEvent e) { handle(e); }
			private void handle(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem m1 = new JMenuItem("Import Single File...");
					JMenuItem m2 = new JMenuItem("Import Directory...");
					menu.add(m1); menu.add(m2);
					menu.show(e.getComponent(), e.getX(), e.getY());
				} else if (SwingUtilities.isLeftMouseButton(e) && e.getID() == MouseEvent.MOUSE_RELEASED) {
					section.parent.owner.appController.servicePort.dispatchLakeChooseRequest(identity);
					OntoDirectoryService.DataLakeService lake = section.parent.owner.appController.servicePort.getActiveDataLake();
					if (lake != null) lake.importFiles();
				}
			}
		});
		
		// Export Logic
		exportBtn.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) { handle(e); }
			@Override public void mouseReleased(MouseEvent e) { handle(e); }
			private void handle(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem m1 = new JMenuItem("Export Filtered...");
					JMenuItem m2 = new JMenuItem("Export Entire Lake...");
					menu.add(m1); menu.add(m2);
					menu.show(e.getComponent(), e.getX(), e.getY());
				} else if (SwingUtilities.isLeftMouseButton(e) && e.getID() == MouseEvent.MOUSE_RELEASED) {
					section.parent.owner.appController.servicePort.dispatchLakeChooseRequest(identity);
				}
			}
		});
		
		actionBar.add(exportBtn);
		actionBar.add(importBtn);
		actionBar.add(closeBtn);
		
		container.add(mainButton);
		container.add(actionBar);
		container.add(Box.createVerticalStrut(10));
	}
	
	private JButton createActionBtn(String text, String iconName, Color hoverColor) {
		JButton btn = new JButton(text);
		btn.setFont(btn.getFont().deriveFont(12f)); // Increased from 10f
		btn.setMargin(new Insets(4, 10, 4, 10));    // Increased padding
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setFocusPainted(false);
		btn.setOpaque(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		
		Icon defaultIcon = Utilities.loadSVGIcon("icons/" + iconName, 14, 14, Utilities.ELEMENT_TEXT_SECONDARY);
		Icon activeIcon = Utilities.loadSVGIcon("icons/" + iconName, 14, 14, hoverColor);
		
		if(defaultIcon != null) {
			btn.setIcon(defaultIcon);
			btn.setRolloverIcon(activeIcon);
		}
		
		btn.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
		
		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) { btn.setForeground(hoverColor); }
			@Override
			public void mouseExited(MouseEvent e) { btn.setForeground(Utilities.ELEMENT_TEXT_SECONDARY); }
		});
		
		if (text.equals("Close")) btn.setForeground(new Color(255, 100, 100));
		return btn;
	}
}
}