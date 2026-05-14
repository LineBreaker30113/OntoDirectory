package org.halim.sgui.vpanels;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.sgui.sglib.ContentView;
import org.halim.sgui.sglib.Utilities;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FilesView extends ContentView {

private final DefaultListModel<FileInterface> listModel;
private final JList<FileInterface> fileList;

// Pagination & State
private List<FileInterface> rawDomainFiles = new ArrayList<>(); // Master cache
private List<FileInterface> currentDomainFiles = new ArrayList<>(); // Filtered view
private int currentPage = 0;
private final int ITEMS_PER_PAGE = 100;

private final JLabel pageLabel;
private final JTextField searchBar;

public FilesView() {
	setLayout(new BorderLayout());
	setBackground(Utilities.WP_BG);
	
	// --- TOP: Search & Filter Bar ---
	JPanel topBar = new JPanel(new BorderLayout(10, 0));
	topBar.setBackground(Utilities.LIST_HEADER_BG);
	topBar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
	
	searchBar = new JTextField();
	searchBar.setBackground(Utilities.ELEMENT_BG);
	searchBar.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	searchBar.setCaretColor(Color.WHITE);
	searchBar.setBorder(BorderFactory.createCompoundBorder(
		  BorderFactory.createLineBorder(Color.DARK_GRAY),
		  BorderFactory.createEmptyBorder(4, 8, 4, 8)
	));
	// Allow ENTER key to trigger search natively
	searchBar.addActionListener(e -> performSearch());
	
	JButton searchBtn = new JButton("Search Domain");
	Utilities.initButton(searchBtn);
	searchBtn.setBackground(Utilities.sideButtonBG);
	searchBtn.setForeground(Color.WHITE);
	searchBtn.addActionListener(e -> performSearch());
	
	topBar.add(searchBar, BorderLayout.CENTER);
	topBar.add(searchBtn, BorderLayout.EAST);
	
	// --- CENTER: The Multi-Select File List ---
	listModel = new DefaultListModel<>();
	fileList = new JList<>(listModel);
	fileList.setBackground(Utilities.LIST_BG);
	fileList.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	fileList.setFixedCellHeight(30);
	
	fileList.setCellRenderer(new DefaultListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof FileInterface fi) {
				// Utilizing the new O(1) Fixed Memory String!
				label.setText("  " + fi.actualName + "  [ID: " + fi.identity + "]");
			}
			if (isSelected) {
				label.setBackground(Utilities.SLPEB_Idle);
				label.setForeground(Color.BLACK);
			} else {
				label.setBackground(Utilities.LIST_BG);
				label.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
			}
			return label;
		}
	});
	
	// Attach Context Menu
	fileList.addMouseListener(new java.awt.event.MouseAdapter() {
		@Override public void mousePressed(java.awt.event.MouseEvent e) { showContextMenu(e); }
		@Override public void mouseReleased(java.awt.event.MouseEvent e) { showContextMenu(e); }
	});
	
	JScrollPane scrollPane = new JScrollPane(fileList);
	scrollPane.setBorder(null);
	
	// --- BOTTOM: Pagination Controls ---
	JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
	bottomBar.setBackground(Utilities.LIST_HEADER_BG);
	
	JButton prevBtn = new JButton("<< Prev");
	Utilities.initButton(prevBtn);
	prevBtn.setForeground(Color.WHITE);
	prevBtn.addActionListener(e -> switchPage(-1));
	
	pageLabel = new JLabel("Page 1 of 1");
	pageLabel.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
	
	JButton nextBtn = new JButton("Next >>");
	Utilities.initButton(nextBtn);
	nextBtn.setForeground(Color.WHITE);
	nextBtn.addActionListener(e -> switchPage(1));
	
	bottomBar.add(prevBtn);
	bottomBar.add(Box.createHorizontalStrut(15));
	bottomBar.add(pageLabel);
	bottomBar.add(Box.createHorizontalStrut(15));
	bottomBar.add(nextBtn);
	
	add(topBar, BorderLayout.NORTH);
	add(scrollPane, BorderLayout.CENTER);
	add(bottomBar, BorderLayout.SOUTH);
}

private void showContextMenu(java.awt.event.MouseEvent e) {
	if (e.isPopupTrigger()) {
		int row = fileList.locationToIndex(e.getPoint());
		if (row != -1 && !fileList.isSelectedIndex(row)) {
			fileList.setSelectedIndex(row);
		}
		
		List<FileInterface> selectedFiles = fileList.getSelectedValuesList();
		if (selectedFiles.isEmpty()) return;
		
		JPopupMenu contextMenu = new JPopupMenu();
		
		JMenuItem exportCopy = new JMenuItem("Export (Copy)");
		JMenuItem exportCrop = new JMenuItem("Export (Move/Crop)");
		JMenuItem rename = new JMenuItem("Rename Actual Name");
		JMenuItem addTag = new JMenuItem("Add Tag to Selection...");
		JMenuItem removeTag = new JMenuItem("Remove Tag from Selection...");
		
		exportCopy.addActionListener(a -> System.out.println("Copying " + selectedFiles.size() + " files."));
		exportCrop.addActionListener(a -> System.out.println("Moving " + selectedFiles.size() + " files."));
		rename.addActionListener(a -> System.out.println("Renaming file ID: " + selectedFiles.get(0).identity));
		
		contextMenu.add(exportCopy);
		contextMenu.add(exportCrop);
		contextMenu.addSeparator();
		contextMenu.add(rename);
		contextMenu.addSeparator();
		contextMenu.add(addTag);
		contextMenu.add(removeTag);
		
		contextMenu.show(fileList, e.getX(), e.getY());
	}
}

@Override
public void onTagChange(OntologyClass ntag, OntologyHierarchyReader reader) {
	Set<FileInterface> rawFiles = reader.getAllFilesForClass(ntag);
	rawDomainFiles = new ArrayList<>(rawFiles);
	currentDomainFiles = new ArrayList<>(rawDomainFiles);
	currentPage = 0;
	updateListModel();
}

private void switchPage(int direction) {
	int maxPages = (int) Math.ceil((double) currentDomainFiles.size() / ITEMS_PER_PAGE);
	int newPage = currentPage + direction;
	if (newPage >= 0 && newPage < maxPages) {
		currentPage = newPage;
		updateListModel();
	}
}

private void performSearch() {
	String query = searchBar.getText().toLowerCase().trim();
	if (query.isEmpty()) {
		currentDomainFiles = new ArrayList<>(rawDomainFiles);
	} else {
		// High-speed RAM filtering algorithm
		currentDomainFiles = rawDomainFiles.stream()
			  .filter(f -> f.actualName != null && f.actualName.toLowerCase().contains(query))
			  .toList();
	}
	currentPage = 0;
	updateListModel();
}

private void updateListModel() {
	listModel.clear();
	int start = currentPage * ITEMS_PER_PAGE;
	int end = Math.min(start + ITEMS_PER_PAGE, currentDomainFiles.size());
	
	for (int i = start; i < end; i++) {
		listModel.addElement(currentDomainFiles.get(i));
	}
	
	int maxPages = Math.max(1, (int) Math.ceil((double) currentDomainFiles.size() / ITEMS_PER_PAGE));
	pageLabel.setText("Page " + (currentPage + 1) + " of " + maxPages);
}
}