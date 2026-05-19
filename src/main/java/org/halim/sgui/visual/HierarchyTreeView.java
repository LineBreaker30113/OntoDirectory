package org.halim.sgui.visual;

import org.halim.dlake.OntologyClass;
import org.halim.pd.OntoDirectoryService;
import org.halim.pd.OntologyReadingService;
import org.halim.sgui.ApplicationController;
import org.halim.sgui.sglib.HierarchyView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class HierarchyTreeView extends HierarchyView {

private final ApplicationController mac;
private final JTree tree;
private DefaultTreeModel treeModel;
private OntoDirectoryService.DataLakeService currentLakeService = null;

private record TagNodeDto(int identity, String name) {
	@Override
	public String toString() {
		return name;
	}
	public String toDebugString() {
		return "[" + name + " :|: " + identity + "]";
	}
}

public HierarchyTreeView(ApplicationController mac) {
	this.mac = mac;
	setLayout(new BorderLayout());
	
	tree = new JTree((javax.swing.tree.TreeNode) null);
	tree.setRootVisible(true);
	tree.setShowsRootHandles(true);
	tree.setBackground(new Color(40, 40, 40));
	tree.setForeground(Color.WHITE);
	
	tree.addTreeSelectionListener(e -> {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		if (node == null || currentLakeService == null) return;
		
		TagNodeDto clickedTag = (TagNodeDto) node.getUserObject();
		
		OntologyClass targetClass = currentLakeService.getOntologyReadingService()
			  .getClassFromIdentity(clickedTag.identity());
		
		mac.wsModeller.broadcastTagSelection(targetClass.identityNumber);
	});
	
	tree.addMouseListener(new MouseAdapter() {
		@Override public void mousePressed(MouseEvent e) { handleMouseClick(e); }
		@Override public void mouseReleased(MouseEvent e) { handleMouseClick(e); }
	});
	
	JScrollPane scrollPane = new JScrollPane(tree);
	scrollPane.setBorder(null);
	add(scrollPane, BorderLayout.CENTER);
}

private void handleMouseClick(@NotNull MouseEvent e) {
	if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
		tree.requestFocusInWindow();
		int selRow = tree.getRowForLocation(e.getX(), e.getY());
		TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
		
		if (selRow != -1 && selPath != null) {
			tree.setSelectionPath(selPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
			TagNodeDto clickedTag = (TagNodeDto) node.getUserObject();
			showContextMenu(e, clickedTag);
		}
	}
}

private void showContextMenu(MouseEvent e, TagNodeDto clickedTag) {
	if (currentLakeService == null) return;
	OntologyReadingService.OntologyManagingService manager = currentLakeService.getOntologyManagingService();
	OntologyClass targetClass = manager.getClassFromIdentity(clickedTag.identity());
	
	JPopupMenu contextMenu = new JPopupMenu();
	
	// --- Creation & Structural Links ---
	JMenuItem createTag = new JMenuItem("Create Sub-Tag Here");
	JMenuItem addParent = new JMenuItem("Add Parent...");
	JMenuItem addChild = new JMenuItem("Add Existing Tag as Child...");
	
	// --- Removals ---
	JMenuItem removeParent = new JMenuItem("Remove a Parent...");
	JMenuItem removeChild = new JMenuItem("Remove a Child...");
	JMenuItem deleteTag = new JMenuItem("Delete Tag Completely");
	
	// --- Utilities & Edits ---
	JMenuItem renameTag = new JMenuItem("Rename Tag");
	JMenuItem copyContents = new JMenuItem("Copy Contents To...");
	
	// --- Undo / Redo ---
	JMenuItem undoAct = new JMenuItem("Undo Last Action");
	JMenuItem redoAct = new JMenuItem("Redo Last Action");
	
	// --- ACTION LISTENERS ---
	
	createTag.addActionListener(a -> {
		String newName = JOptionPane.showInputDialog(this, "Name for new tag under " + clickedTag.name() + ":");
		if (newName != null && !newName.trim().isEmpty()) {
			manager.createOntologyClass(newName.trim(), List.of(clickedTag.identity()), null);
			refreshTreeUI();
		}
	});
	
	addParent.addActionListener(a -> {
		Integer parentId = showTagSelector("Select New Parent", getAllTags());
		if (parentId != null) {
			manager.addParent(clickedTag.identity(), parentId);
			refreshTreeUI();
		}
	});
	
	addChild.addActionListener(a -> {
		Integer childId = showTagSelector("Select Existing Tag to make a Child", getAllTags());
		if (childId != null) {
			manager.addParent(childId, clickedTag.identity());
			refreshTreeUI();
		}
	});
	
	removeParent.addActionListener(a -> {
		List<TagNodeDto> parents = targetClass.parents.stream()
			  .map(p -> new TagNodeDto(p.identityNumber, p.name)).toList();
		Integer parentId = showTagSelector("Remove which Parent?", parents);
		if (parentId != null) {
			manager.removeParent(clickedTag.identity(), parentId);
			refreshTreeUI();
		}
	});
	
	removeChild.addActionListener(a -> {
		List<TagNodeDto> children = targetClass.children.stream()
			  .map(c -> new TagNodeDto(c.identityNumber, c.name)).toList();
		Integer childId = showTagSelector("Remove which Child?", children);
		if (childId != null) {
			manager.removeParent(childId, clickedTag.identity());
			refreshTreeUI();
		}
	});
	
	deleteTag.addActionListener(a -> {
		int result = JOptionPane.showConfirmDialog(this, "Delete tag '" + clickedTag.name() + "'? This will orphan its children.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			manager.removeOntologyClass(clickedTag.identity());
			refreshTreeUI();
		}
	});
	
	renameTag.addActionListener(a -> {
		String newName = (String) JOptionPane.showInputDialog(this, "New name for tag:", "Rename", JOptionPane.PLAIN_MESSAGE, null, null, clickedTag.name());
		if (newName != null && !newName.trim().isEmpty() && !newName.equals(clickedTag.name())) {
			manager.renameOntologyClass(clickedTag.identity(), newName.trim());
			refreshTreeUI();
		}
	});
	
	copyContents.addActionListener(a -> showCopyContentsDialog(clickedTag.identity()));
	
	undoAct.addActionListener(a -> { manager.undo(); refreshTreeUI(); });
	redoAct.addActionListener(a -> { manager.redo(); refreshTreeUI(); });
	
	// --- POPULATE MENU ---
	contextMenu.add(createTag);
	contextMenu.add(addParent);
	contextMenu.add(addChild);
	contextMenu.addSeparator();
	contextMenu.add(removeParent);
	contextMenu.add(removeChild);
	contextMenu.add(deleteTag);
	contextMenu.addSeparator();
	contextMenu.add(renameTag);
	contextMenu.add(copyContents);
	contextMenu.addSeparator();
	contextMenu.add(undoAct);
	contextMenu.add(redoAct);
	
	contextMenu.show(tree, e.getX(), e.getY());
}

// --- UX HELPER DIALOGS ---

private Integer showTagSelector(String title, List<TagNodeDto> tags) {
	if (tags.isEmpty()) {
		JOptionPane.showMessageDialog(this, "No valid tags available for this operation.", "Empty List", JOptionPane.INFORMATION_MESSAGE);
		return null;
	}
	JList<TagNodeDto> list = new JList<>(tags.toArray(new TagNodeDto[0]));
	list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	JScrollPane scrollPane = new JScrollPane(list);
	scrollPane.setPreferredSize(new Dimension(250, 300));
	
	int result = JOptionPane.showConfirmDialog(this, scrollPane, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
	if (result == JOptionPane.OK_OPTION && list.getSelectedValue() != null) {
		return list.getSelectedValue().identity();
	}
	return null;
}

private void showCopyContentsDialog(int sourceId) {
	List<TagNodeDto> allTags = getAllTags();
	JComboBox<TagNodeDto> targetDropdown = new JComboBox<>(allTags.toArray(new TagNodeDto[0]));
	JCheckBox copyParents = new JCheckBox("Copy Parents (Link upwards)", true);
	JCheckBox copyChildren = new JCheckBox("Copy Children (Link downwards)", true);
	JCheckBox copyFiles = new JCheckBox("Copy Associated Files", true);
	
	JPanel panel = new JPanel(new GridLayout(5, 1, 0, 5));
	panel.add(new JLabel("Select Target Tag:"));
	panel.add(targetDropdown);
	panel.add(copyParents);
	panel.add(copyChildren);
	panel.add(copyFiles);
	
	int result = JOptionPane.showConfirmDialog(this, panel, "Copy Graph Contents", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
	if (result == JOptionPane.OK_OPTION && targetDropdown.getSelectedItem() != null) {
		TagNodeDto target = (TagNodeDto) targetDropdown.getSelectedItem();
		currentLakeService.getOntologyManagingService().copyContentsTo(
			  sourceId, target.identity(), copyParents.isSelected(), copyChildren.isSelected(), copyFiles.isSelected()
		);
		refreshTreeUI();
	}
}

// --- DATA LAKE QUERY HELPERS ---

private List<TagNodeDto> getAllTags() {
	List<TagNodeDto> tags = new ArrayList<>();
	OntologyReadingService reader = currentLakeService.getOntologyReadingService();
	OntologyClass root = reader.getRootOntologyClass();
	if (root == null) return tags;
	
	Queue<OntologyClass> q = new LinkedList<>();
	Set<Integer> visited = new HashSet<>();
	q.add(root);
	visited.add(root.identityNumber);
	
	while (!q.isEmpty()) {
		OntologyClass curr = q.poll();
		tags.add(new TagNodeDto(curr.identityNumber, curr.name));
		for (OntologyClass child : curr.children) {
			if (visited.add(child.identityNumber)) {
				q.add(child);
			}
		}
	}
	tags.sort(Comparator.comparing(TagNodeDto::name));
	return tags;
}

private void refreshTreeUI() {
	// As designed in Front 2: The background Debouncer flushes 'isDirty' to disk.
	// We only need to tell the UI controller to rebuild the visual JTree from RAM.
	mac.wsModeller.triggerLakeRefresh(currentLakeService);
}

@Override
protected void clearHierarchy() {
	if (treeModel != null) {
		treeModel.setRoot(null);
		treeModel.reload();
	}
}

// --- WORKSPACE LISTENER IMPLEMENTATION ---

@Override
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {
	java.util.Enumeration<TreePath> expandedPaths = null;
	if (tree.getModel() != null && tree.getModel().getRoot() != null) {
		expandedPaths = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
	}
	clearHierarchy();
	this.currentLakeService = dataLakeService;
	
	if (dataLakeService == null) return;
	OntologyReadingService readerService = dataLakeService.getOntologyReadingService();
	if (readerService == null) return;
	
	OntologyClass rootClass = readerService.getRootOntologyClass();
	if (rootClass == null) return;
	
	TagNodeDto rootDto = new TagNodeDto(rootClass.identityNumber, rootClass.name);
	DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootDto);
	treeModel = new DefaultTreeModel(rootNode);
	
	Queue<Object[]> queue = new LinkedList<>();
	queue.add(new Object[]{rootClass, rootNode});
	
	while (!queue.isEmpty()) {
		Object[] current = queue.poll();
		OntologyClass currentClass = (OntologyClass) current[0];
		DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) current[1];
		
		for (OntologyClass childClass : currentClass.children) {
			TagNodeDto childDto = new TagNodeDto(childClass.identityNumber, childClass.name);
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childDto);
			currentNode.add(childNode);
			queue.add(new Object[]{childClass, childNode});
		}
	}
	
	tree.setModel(treeModel);
	
	if (expandedPaths != null) {
		while (expandedPaths.hasMoreElements()) {
			TreePath path = expandedPaths.nextElement();
			restorePath(tree, rootNode, path, 0);
		}
	} else {
		tree.expandRow(0);
	}
}

private void restorePath(JTree tree, DefaultMutableTreeNode currentNode, TreePath oldPath, int depth) {
	if (depth >= oldPath.getPathCount()) return;
	
	String targetName = oldPath.getPathComponent(depth).toString();
	if (currentNode.getUserObject().toString().equals(targetName)) {
		tree.expandPath(new TreePath(currentNode.getPath()));
		
		for (int i = 0; i < currentNode.getChildCount(); i++) {
			restorePath(tree, (DefaultMutableTreeNode) currentNode.getChildAt(i), oldPath, depth + 1);
		}
	}
}

@Override public void onSelectedClassChange(OntologyClass ontologyClass) { }
@Override public void onDomainChange(OntologyClass newDomain) { }
}