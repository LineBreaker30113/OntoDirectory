package org.halim.sgui.visual;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyFilter;
import org.halim.pd.OntoDirectoryService;
import org.halim.pd.OntologyReadingService;
import org.halim.sgui.sglib.ContentView;
import org.halim.sgui.sglib.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class FilesView extends ContentView {

private final DefaultListModel<FileInterface> listModel;
private final JList<FileInterface> fileList;

private OntoDirectoryService.DataLakeService activeLakeService = null;
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
	topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
	// 1. Override preferred size AND layout logic natively
	astContainer = new JPanel(null) {
		@Override
		public Dimension getPreferredSize() {
			if (rootAstNode != null) {
				return rootAstNode.getPreferredSize();
			}
			return super.getPreferredSize();
		}
		
		@Override
		public void doLayout() {
			super.doLayout();
			if (rootAstNode == null) return;
			
			int containerWidth = getWidth();
			Dimension childSize = rootAstNode.getPreferredSize();
			
			// Calculate centering coordinates
			int x = (containerWidth - childSize.width) / 2;
			int y = (65 - childSize.height) / 2;
			
			// If the AST exceeds view bounds, anchor it to 0,0 so scrolling works
			if (x < 0) x = 0;
			System.out.println(y);
//			if (y < 0) y = 0;
			
			// Force the child to take its required space at the centered coordinates
			rootAstNode.setBounds(x, y, childSize.width, childSize.height);
		}
	};
	
	astContainer.setBackground(Utilities.ELEMENT_BG);
	initEmptyAst();
	
	JScrollPane astScrollPane = new JScrollPane(astContainer);
	astScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	astScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
	astScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
	astScrollPane.setPreferredSize(new Dimension(0, 65));
	astScrollPane.getHorizontalScrollBar().setUnitIncrement(astScrollPane.getHorizontalScrollBar().getUnitIncrement() * 26);
	
	JPanel actionPanel = new JPanel(new GridLayout(3, 1, 0, 4));
	actionPanel.setPreferredSize(new Dimension(100, 80));
	actionPanel.setOpaque(false);
	
	actionPanel.add(createColBtn("Save", "plus-circle.svg", e -> {
		OntologyFilter f = rootAstNode.buildFilter();
		if (f != null && activeLakeService != null) {
			String name = JOptionPane.showInputDialog(this, "Tag Name:");
			if (name != null) activeLakeService.getOntologyManagingService().filterToClass(f, name);
		}
	}));
	actionPanel.add(createColBtn("Search", "magnifying-glass.svg", e -> performSearch()));
	actionPanel.add(createColBtn("Export", "share.svg", e -> {
		if (activeLakeService != null) {
			JFileChooser c = new JFileChooser(); c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) activeLakeService.exportSpecificFiles(currentDomainFiles, c.getSelectedFile().toPath());
		}
	}));
	
	topBar.add(astScrollPane, BorderLayout.CENTER);
	topBar.add(actionPanel, BorderLayout.EAST);
	
	// --- CENTER: Infinite Scroll List Rendering ---
	listModel = new DefaultListModel<>();
	
	fileList = new JList<>(listModel)/* {
		@Override
		public String getToolTipText(java.awt.event.MouseEvent event) {
			int index = locationToIndex(event.getPoint());
			if (index != -1) {
				FileInterface fi = getModel().getElementAt(index);
				if (fi.tagsByIdentity != null && activeLakeService != null) {
					OntologyReadingService reader = activeLakeService.getOntologyReadingService();
					StringBuilder tagBuilder = new StringBuilder("<html>");
					boolean hasTags = false;
					for (int tagId : fi.tagsByIdentity) {
						if (tagId == 0) continue; // Skip Root
						OntologyClass tagClass = reader.getClassFromIdentity(tagId);
						if (tagClass != null) {
							tagBuilder.append(tagClass.name).append("<br>");
							hasTags = true;
						}
					}
					if (hasTags) {
						tagBuilder.append("</html>");
						return tagBuilder.toString();
					}
				}
			}
			return null;
		}
	}*/;
	// MAGIC LINE: This is required to force JList to fire mouse events for tooltip generation
//	fileList.setToolTipText("");
	
	fileList.setBackground(Utilities.LIST_BG);
	fileList.setForeground(Utilities.ELEMENT_TEXT_PRIMARY);
	fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	fileList.setFixedCellHeight(30);
	
	fileList.setCellRenderer(new DefaultListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof FileInterface fi) {
				label.setText("  " + fi.actualName);
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
		@Override public void mousePressed(java.awt.event.MouseEvent e) {
			showContextMenu(e);
			
			// Trigger tag list purely on a single left click
			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1 && !e.isPopupTrigger()) {
				int index = fileList.locationToIndex(e.getPoint());
				// Ensure they clicked exactly on an item row, not empty space
				if (index != -1 && fileList.getCellBounds(index, index).contains(e.getPoint())) {
					FileInterface fi = listModel.getElementAt(index);
					showTagListPopup(fi, e.getComponent(), e.getX(), e.getY());
				}
			}
		}
		@Override public void mouseReleased(java.awt.event.MouseEvent e) { showContextMenu(e); }
	});
	
	// DRAG AND DROP OUT (Files -> TreeView)
	fileList.setDragEnabled(true);
	fileList.setTransferHandler(new TransferHandler() {
		@Override public int getSourceActions(JComponent c) { return COPY_OR_MOVE; }
		@Override protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
			return new Utilities.GenericTransferable(fileList.getSelectedValuesList(), Utilities.FILE_LIST_FLAVOR);
		}
	});
	
	JScrollPane scrollPane = new JScrollPane(fileList);
	scrollPane.setBorder(null);
	
	scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
		if (!e.getValueIsAdjusting()) {
			JScrollBar bar = (JScrollBar) e.getAdjustable();
			if (bar.getValue() + bar.getModel().getExtent() >= bar.getModel().getMaximum() - 100) {
				loadMoreFiles();
			}
		}
	});
	
	add(topBar, BorderLayout.NORTH);
	add(scrollPane, BorderLayout.CENTER);
}

private @NotNull JButton createColBtn(String text, String icon, java.awt.event.ActionListener al) {
	JButton b = new JButton(text, Utilities.loadSVGIcon("icons/"+icon, 16, 16, Color.WHITE));
	b.setBackground(Utilities.sideButtonBG);
	b.setForeground(Color.WHITE);
	b.setMargin(new Insets(0, 0, 0, 0));
	b.addActionListener(al);
	return b;
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

private void showTagListPopup(FileInterface fi, Component anchor, int x, int y) {
	if (fi.tagsByIdentity == null || activeLakeService == null) return;
	OntologyReadingService reader = activeLakeService.getOntologyReadingService();
	
	JPopupMenu tagMenu = new JPopupMenu();
	boolean hasTags = false;
	
	for (int tagId : fi.tagsByIdentity) {
		if (tagId == 0) continue; // Skip the Root "File" tag
		OntologyClass tagClass = reader.getClassFromIdentity(tagId);
		if (tagClass != null) {
			JMenuItem item = new JMenuItem(tagClass.name);
			item.setEnabled(false); // Make it read-only so it acts like a label
			tagMenu.add(item);
			hasTags = true;
		}
	}
	
	if (hasTags) {
		tagMenu.show(anchor, x, y);
	}
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
		JMenuItem openFile = new JMenuItem("Open File (Read-Only Copy)");
		JMenuItem addTag = new JMenuItem("Add Tag to Selected...");
		JMenuItem removeTag = new JMenuItem("Remove Tag from Selected...");
		JMenuItem exportCopy = new JMenuItem("Export Selected Files...");
		JMenuItem rename = new JMenuItem("Rename Actual Name");
		
		openFile.addActionListener(a -> {
			for (FileInterface fi : selectedFiles) {
				try {
					Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "ontoDirTemp");
					Files.createDirectories(tempDir);
					Path tempFile = tempDir.resolve(fi.actualName);
					Files.copy(fi.actualFile, tempFile, StandardCopyOption.REPLACE_EXISTING);
					tempFile.toFile().setReadOnly();
					Desktop.getDesktop().open(tempFile.toFile());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "Could not open file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
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
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService s) {
	this.activeLakeService = s;
	performSearch();
}

@Override
public void onSelectedClassChange(OntologyClass ontologyClass) {
	if (ontologyClass == null || activeLakeService == null) return;
	
	rootAstNode = new LeafNodeUI(newNode -> {
		rootAstNode = newNode;
		astContainer.removeAll();
		astContainer.add(rootAstNode);
		astContainer.revalidate();
		astContainer.repaint();
	}, ontologyClass.identityNumber, false);
	
	astContainer.removeAll();
	astContainer.add(rootAstNode);
	astContainer.revalidate();
	astContainer.repaint();
	
	performSearch();
}

@Override public void onDomainChange(OntologyClass ontologyClass) { onSelectedClassChange(ontologyClass); }

private void performSearch() {
	if (rootAstNode == null || activeLakeService == null) return;
	OntologyFilter filter = rootAstNode.buildFilter();
	OntologyReadingService reader = activeLakeService.getOntologyReadingService();
	
	if (filter == null) {
		currentDomainFiles = new ArrayList<>(reader.getAllOntologyElements(0));
	} else {
		currentDomainFiles = new ArrayList<>(filter.resolve(reader));
	}
	
	currentDomainFiles.sort(Comparator.comparingInt(f -> f.identity));
	
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
		setOpaque(true);
		setAlignmentY(Component.CENTER_ALIGNMENT); // Perfectly centers in BoxLayout.X_AXIS
		setBackground(Utilities.LIST_HEADER_BG);
		// Replaced FlowLayout gap with empty border margins so BoxLayout spaces them correctly
		setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createEmptyBorder(2, 4, 2, 4),
			  BorderFactory.createCompoundBorder(
				    BorderFactory.createLineBorder(Color.BLACK, 1),
				    BorderFactory.createEmptyBorder(4, 6, 4, 6)
			  )
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
		
		JButton btn = new JButton("[ + Drop Tag or Click ]");
		Utilities.initButton(btn);
		btn.setForeground(Color.GRAY);
		btn.addActionListener(e -> showMenu(btn));
		add(btn);
		
		setTransferHandler(new TransferHandler() {
			@Override public boolean canImport(TransferSupport support) {
				return support.isDataFlavorSupported(Utilities.TAG_LIST_FLAVOR);
			}
			@Override public boolean importData(TransferSupport support) {
				try {
					@SuppressWarnings("unchecked")
					List<Utilities.TagNodeDto> tags = (List<Utilities.TagNodeDto>) support.getTransferable().getTransferData(Utilities.TAG_LIST_FLAVOR);
					if (tags != null && !tags.isEmpty()) {
						int tagId = tags.get(0).identity();
						JPopupMenu dropMenu = new JPopupMenu();
						dropMenu.add(createItem("Filter as Domain of " + tags.get(0).name(), () -> onReplace.accept(new LeafNodeUI(onReplace, tagId, false))));
						dropMenu.add(createItem("Filter as Direct Element of " + tags.get(0).name(), () -> onReplace.accept(new LeafNodeUI(onReplace, tagId, true))));
						dropMenu.show(EmptyNodeUI.this, 0, getHeight());
						return true;
					}
				} catch (Exception ex) { ex.printStackTrace(); }
				return false;
			}
		});
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
		setBackground(Utilities.sideBarBG.darker());
		
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