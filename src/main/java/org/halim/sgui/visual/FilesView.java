package org.halim.sgui.visual;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyFilter;
import org.halim.pd.OntoDirectoryService;
import org.halim.pd.OntologyReadingService;
import org.halim.sgui.sglib.ContentView;
import org.halim.sgui.sglib.Utilities;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class FilesView extends ContentView {

private final DefaultListModel<FileInterface> listModel;
private final JList<FileInterface> fileList;

private OntoDirectoryService.DataLakeService activeLakeService = null;
private List<FileInterface> rawDomainFiles = new ArrayList<>();
private List<FileInterface> currentDomainFiles = new ArrayList<>();

// Infinite Scrolling State
private int loadedCount = 0;
private final int ITEMS_PER_CHUNK = 100;

// AST Builder Container
private final JPanel astContainer;
private FilterNodeUI rootAstNode;

public FilesView() {
	setLayout(new BorderLayout());
	setBackground(Utilities.WP_BG);
	
	// --- TOP: AST Filter Track ---
	JPanel topBar = new JPanel(new BorderLayout(10, 0));
	topBar.setBackground(Utilities.LIST_HEADER_BG);
	topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
	// The AST Track: A sideways scrollable rectangle
	astContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
	astContainer.setBackground(Utilities.ELEMENT_BG);
	initEmptyAst();
	
	JScrollPane astScrollPane = new JScrollPane(astContainer);
	astScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	astScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
	astScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
	astScrollPane.setPreferredSize(new Dimension(0, 65)); // Fixed height track
	
	// Action Buttons
	JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
	actionPanel.setOpaque(false);
	
	JButton searchBtn = new JButton("Apply Filter");
	Utilities.initButton(searchBtn);
	searchBtn.setBackground(Utilities.sideButtonBG);
	searchBtn.setForeground(Color.WHITE);
	searchBtn.setPreferredSize(new Dimension(120, 35));
	searchBtn.addActionListener(e -> performSearch());
	
	JButton exportBtn = new JButton("Export Filtered");
	Utilities.initButton(exportBtn);
	exportBtn.setBackground(new Color(100, 150, 200));
	exportBtn.setForeground(Color.WHITE);
	exportBtn.setPreferredSize(new Dimension(130, 35));
	exportBtn.addActionListener(e -> {
		if (activeLakeService == null || currentDomainFiles.isEmpty()) return;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select Export Destination");
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			activeLakeService.exportSpecificFiles(currentDomainFiles, chooser.getSelectedFile().toPath());
		}
	});
	
	actionPanel.add(exportBtn);
	actionPanel.add(searchBtn);
	
	topBar.add(astScrollPane, BorderLayout.CENTER);
	topBar.add(actionPanel, BorderLayout.EAST);
	
	// --- CENTER: Infinite Scroll List Rendering ---
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
				label.setText("  " + fi.actualName + "  [ID: " + fi.identity + "]");
				Icon fileIcon = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(fi.actualFile.toFile());
				label.setIcon(fileIcon);
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
	
	fileList.addMouseListener(new java.awt.event.MouseAdapter() {
		@Override public void mousePressed(java.awt.event.MouseEvent e) { showContextMenu(e); }
		@Override public void mouseReleased(java.awt.event.MouseEvent e) { showContextMenu(e); }
	});
	
	JScrollPane scrollPane = new JScrollPane(fileList);
	scrollPane.setBorder(null);
	
	// The Infinite Scroll Trigger
	scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
		if (!e.getValueIsAdjusting()) {
			JScrollBar bar = (JScrollBar) e.getAdjustable();
			int extent = bar.getModel().getExtent();
			int maximum = bar.getModel().getMaximum();
			int value = bar.getValue();
			// Load more if we are within 100 pixels of the bottom
			if (value + extent >= maximum - 100) {
				loadMoreFiles();
			}
		}
	});
	
	add(topBar, BorderLayout.NORTH);
	add(scrollPane, BorderLayout.CENTER);
}

private void initEmptyAst() {
	astContainer.removeAll();
	rootAstNode = new EmptyNodeUI(newNode -> {
		rootAstNode = newNode;
		astContainer.removeAll();
		astContainer.add(rootAstNode);
		astContainer.revalidate();
		astContainer.repaint();
	});
	astContainer.add(rootAstNode);
	astContainer.revalidate();
	astContainer.repaint();
}

private void showContextMenu(java.awt.event.MouseEvent e) {
	if (e.isPopupTrigger()) {
		int row = fileList.locationToIndex(e.getPoint());
		if (row != -1 && !fileList.isSelectedIndex(row)) {
			fileList.setSelectedIndex(row);
		}
		
		List<FileInterface> selectedFiles = fileList.getSelectedValuesList();
		if (selectedFiles.isEmpty() || activeLakeService == null) return;
		
		JPopupMenu contextMenu = new JPopupMenu();
		JMenuItem openFile = new JMenuItem("Open File (System Default)");
		JMenuItem addTag = new JMenuItem("Add Tag to Selected...");
		JMenuItem removeTag = new JMenuItem("Remove Tag from Selected...");
		JMenuItem exportCopy = new JMenuItem("Export Selected Files...");
		JMenuItem rename = new JMenuItem("Rename Actual Name");
		
		openFile.addActionListener(a -> {
			try {
				Desktop.getDesktop().open(selectedFiles.get(0).actualFile.toFile());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, "Could not open file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		addTag.addActionListener(a -> {
			Integer tagId = pickTagDialog();
			if (tagId != null) {
				for (FileInterface fi : selectedFiles) {
					activeLakeService.getOntologyManagingService().addElement(tagId, fi);
				}
				fileList.repaint();
			}
		});
		
		removeTag.addActionListener(a -> {
			Integer tagId = pickTagDialog();
			if (tagId != null) {
				for (FileInterface fi : selectedFiles) {
					activeLakeService.getOntologyManagingService().removeElement(tagId, fi);
				}
				// Visual cleanup if currently viewing the tag we just removed
				performSearch();
			}
		});
		
		exportCopy.addActionListener(a -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Select Export Destination");
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				activeLakeService.exportSpecificFiles(selectedFiles, chooser.getSelectedFile().toPath());
			}
		});
		
		rename.addActionListener(a -> {
			FileInterface target = selectedFiles.get(0);
			String newName = (String) JOptionPane.showInputDialog(this, "New file name:", "Rename File", JOptionPane.PLAIN_MESSAGE, null, null, target.actualName);
			if (newName != null && !newName.trim().isEmpty()) {
				activeLakeService.getOntologyManagingService().renameElementActualName(target, newName);
				fileList.repaint();
			}
		});
		
		contextMenu.add(openFile);
		contextMenu.addSeparator();
		contextMenu.add(addTag);
		contextMenu.add(removeTag);
		contextMenu.addSeparator();
		contextMenu.add(exportCopy);
		contextMenu.add(rename);
		contextMenu.show(fileList, e.getX(), e.getY());
	}
}

@Override
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {
	this.activeLakeService = dataLakeService;
	
	// Ensure AST has the whole lake to filter if no tree node is clicked
	if (dataLakeService != null && dataLakeService.getOntologyReadingService() != null) {
		OntologyReadingService reader = dataLakeService.getOntologyReadingService();
		// ID 0 is the universal 'File' Root Tag. It holds everything.
		rawDomainFiles = new ArrayList<>(reader.getAllOntologyElements(0));
	} else {
		rawDomainFiles.clear();
	}
	
	currentDomainFiles = new ArrayList<>(rawDomainFiles);
	listModel.clear();
	loadedCount = 0;
	initEmptyAst();
	loadMoreFiles(); // Bootstrap initial view
}

@Override
public void onSelectedClassChange(OntologyClass ontologyClass) {
	if (ontologyClass == null || activeLakeService == null) return;
	OntologyReadingService reader = activeLakeService.getOntologyReadingService();
	if (reader == null) return;
	
	List<FileInterface> rawFiles = reader.getAllOntologyElements(ontologyClass.identityNumber);
	rawDomainFiles = new ArrayList<>(rawFiles);
	currentDomainFiles = new ArrayList<>(rawDomainFiles);
	
	// Reset AST Builder on specific node click to avoid double-filtering confusion
	initEmptyAst();
	refreshScrollList();
}

@Override public void onDomainChange(OntologyClass ontologyClass) { onSelectedClassChange(ontologyClass); }

private void performSearch() {
	if (rootAstNode == null || activeLakeService == null) return;
	OntologyFilter filter = rootAstNode.buildFilter();
	
	if (filter == null) {
		currentDomainFiles = new ArrayList<>(rawDomainFiles);
	} else {
		Set<FileInterface> filteredSet = filter.resolve(activeLakeService.getOntologyReadingService());
		// Retain original chronological sort while filtering
		currentDomainFiles = rawDomainFiles.stream().filter(filteredSet::contains).toList();
	}
	refreshScrollList();
}

private void refreshScrollList() {
	listModel.clear();
	loadedCount = 0;
	loadMoreFiles();
}

private void loadMoreFiles() {
	if (currentDomainFiles == null) return;
	int total = currentDomainFiles.size();
	if (loadedCount >= total) return;
	
	int end = Math.min(loadedCount + ITEMS_PER_CHUNK, total);
	for (int i = loadedCount; i < end; i++) {
		listModel.addElement(currentDomainFiles.get(i));
	}
	loadedCount = end;
}

// =========================================================================
// AST GUI BUILDER COMPONENTS (Blocky Track Style)
// =========================================================================

private Integer pickTagDialog() {
	if (activeLakeService == null) return null;
	OntologyReadingService reader = activeLakeService.getOntologyReadingService();
	OntologyClass root = reader.getRootOntologyClass();
	if (root == null) return null;
	
	List<String> displayNames = new ArrayList<>();
	List<Integer> ids = new ArrayList<>();
	
	Queue<OntologyClass> q = new LinkedList<>();
	Set<Integer> visited = new HashSet<>();
	q.add(root);
	visited.add(root.identityNumber);
	
	while (!q.isEmpty()) {
		OntologyClass curr = q.poll();
		displayNames.add(curr.name);
		ids.add(curr.identityNumber);
		for (OntologyClass child : curr.getSafeChildren()) {
			if (visited.add(child.identityNumber)) q.add(child);
		}
	}
	
	JComboBox<String> combo = new JComboBox<>(displayNames.toArray(new String[0]));
	int res = JOptionPane.showConfirmDialog(this, combo, "Select Conceptual Tag", JOptionPane.OK_CANCEL_OPTION);
	if (res == JOptionPane.OK_OPTION && combo.getSelectedIndex() != -1) {
		return ids.get(combo.getSelectedIndex());
	}
	return null;
}

private abstract class FilterNodeUI extends JPanel {
	public FilterNodeUI() {
		setOpaque(true); // Make the blocks solid
		setBackground(Utilities.LIST_HEADER_BG);
		setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createLineBorder(Color.BLACK, 1),
			  BorderFactory.createEmptyBorder(4, 6, 4, 6)
		));
		setLayout(new FlowLayout(FlowLayout.CENTER, 4, 0));
	}
	public abstract OntologyFilter buildFilter();
}

private class EmptyNodeUI extends FilterNodeUI {
	private final Consumer<FilterNodeUI> onReplace;
	public EmptyNodeUI(Consumer<FilterNodeUI> onReplace) {
		this.onReplace = onReplace;
		setBackground(Utilities.WP_BG);
		
		JButton btn = new JButton("[ + ]");
		Utilities.initButton(btn);
		btn.setForeground(Color.GRAY);
		btn.addActionListener(e -> showMenu(btn));
		add(btn);
	}
	
	private void showMenu(Component anchor) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(createItem("Direct Element Of", () -> replaceWithLeaf(true)));
		menu.add(createItem("Under Domain Of", () -> replaceWithLeaf(false)));
		menu.addSeparator();
		menu.add(createItem("AND Operator", () -> onReplace.accept(new BinaryNodeUI(onReplace, "AND"))));
		menu.add(createItem("OR Operator", () -> onReplace.accept(new BinaryNodeUI(onReplace, "OR"))));
		menu.add(createItem("SUBTRACT Operator", () -> onReplace.accept(new BinaryNodeUI(onReplace, "SUB"))));
		menu.addSeparator();
		menu.add(createItem("Name Contains", () -> onReplace.accept(new ContainsNodeUI(onReplace))));
		menu.add(createItem("Clear Block", () -> onReplace.accept(new EmptyNodeUI(onReplace))));
		menu.show(anchor, 0, anchor.getHeight());
	}
	
	private JMenuItem createItem(String label, Runnable action) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(e -> action.run());
		return item;
	}
	
	private void replaceWithLeaf(boolean direct) {
		Integer tagId = pickTagDialog();
		if (tagId != null) onReplace.accept(new LeafNodeUI(onReplace, tagId, direct));
	}
	
	@Override public OntologyFilter buildFilter() { return null; }
}

private class LeafNodeUI extends FilterNodeUI {
	private final int tagId;
	private final boolean direct;
	public LeafNodeUI(Consumer<FilterNodeUI> onReplace, int tagId, boolean direct) {
		this.tagId = tagId;
		this.direct = direct;
		String name = activeLakeService.getOntologyReadingService().getClassFromIdentity(tagId).name;
		
		JButton resetBtn = new JButton("↻");
		Utilities.initButton(resetBtn);
		resetBtn.setForeground(Color.RED.darker());
		resetBtn.addActionListener(e -> onReplace.accept(new EmptyNodeUI(onReplace)));
		
		JLabel lbl = new JLabel((direct ? "Direct[" : "Domain[") + name + "]");
		lbl.setForeground(Utilities.GOLDEN_COLOR);
		
		add(resetBtn); add(lbl);
	}
	@Override public OntologyFilter buildFilter() {
		return direct ? new OntologyFilter.DirectElementOf(tagId) : new OntologyFilter.UnderDomain(tagId);
	}
}

private class BinaryNodeUI extends FilterNodeUI {
	private FilterNodeUI left, right;
	private final String operator;
	public BinaryNodeUI(Consumer<FilterNodeUI> onReplace, String operator) {
		this.operator = operator;
		setBackground(Utilities.sideBarBG.darker()); // Distinct color for operators
		
		JButton resetBtn = new JButton("↻");
		Utilities.initButton(resetBtn);
		resetBtn.setForeground(Color.RED.darker());
		resetBtn.addActionListener(e -> onReplace.accept(new EmptyNodeUI(onReplace)));
		
		left = new EmptyNodeUI(n -> { remove(left); left = n; add(left, 1); revalidate(); repaint(); });
		right = new EmptyNodeUI(n -> { remove(right); right = n; add(right); revalidate(); repaint(); });
		
		JLabel opLbl = new JLabel(" " + operator + " ");
		opLbl.setFont(opLbl.getFont().deriveFont(Font.BOLD));
		opLbl.setForeground(Color.WHITE);
		
		add(resetBtn); add(left); add(opLbl); add(right);
	}
	@Override public OntologyFilter buildFilter() {
		OntologyFilter l = left.buildFilter(), r = right.buildFilter();
		if (l == null || r == null) return null;
		if (operator.equals("AND")) return new OntologyFilter.AndFilter(l, r);
		if (operator.equals("OR")) return new OntologyFilter.OrFilter(l, r);
		return new OntologyFilter.SubtractFilter(l, r);
	}
}

private class ContainsNodeUI extends FilterNodeUI {
	private FilterNodeUI baseNode;
	private final JTextField textField;
	public ContainsNodeUI(Consumer<FilterNodeUI> onReplace) {
		setBackground(Utilities.sideBarBG.darker());
		
		JButton resetBtn = new JButton("↻");
		Utilities.initButton(resetBtn);
		resetBtn.setForeground(Color.RED.darker());
		resetBtn.addActionListener(e -> onReplace.accept(new EmptyNodeUI(onReplace)));
		
		baseNode = new EmptyNodeUI(n -> { remove(baseNode); baseNode = n; add(baseNode, 1); revalidate(); repaint(); });
		
		JLabel opLbl = new JLabel(" Contains: ");
		opLbl.setFont(opLbl.getFont().deriveFont(Font.BOLD));
		opLbl.setForeground(Color.WHITE);
		
		textField = new JTextField(10);
		textField.setBackground(Utilities.ELEMENT_BG);
		textField.setForeground(Color.WHITE);
		textField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		
		add(resetBtn); add(baseNode); add(opLbl); add(textField);
	}
	@Override public OntologyFilter buildFilter() {
		OntologyFilter base = baseNode.buildFilter();
		if (base == null || textField.getText().trim().isEmpty()) return base;
		return new OntologyFilter.WithNameContaining(base, textField.getText().trim());
	}
}
}