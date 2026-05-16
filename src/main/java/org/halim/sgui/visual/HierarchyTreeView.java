package org.halim.sgui.visual;

import org.halim.dlake.OntologyClass;
import org.halim.hport.OntoDirectoryService;
import org.halim.hport.OntologyReadingService;
import org.halim.sgui.ApplicationController;
import org.halim.sgui.sglib.HierarchyView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class HierarchyTreeView extends HierarchyView {

private final ApplicationController mac;
private final JTree tree;
private DefaultTreeModel treeModel;
private OntoDirectoryService.DataLakeService currentLakeService = null;

private record TagNodeDto(int identity, String name) {
	// Strictly for Swing's DefaultTreeCellRenderer
	@Override
	public String toString() {
		return name;
	}
	// Strictly for your terminal logging / debug tracing
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
		
		// Bridge identity surrogate back to Domain class lookup
		OntologyClass targetClass = currentLakeService.getOntologyReadingService()
			  .getClassFromIdentity(clickedTag.identity());
		
		// Broadcast via the new listener model structure
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
	
	JPopupMenu contextMenu = new JPopupMenu();
	JMenuItem createTag = new JMenuItem("Create Sub-Tag Here");
	JMenuItem removeTag = new JMenuItem("Delete Tag");
	
	createTag.addActionListener(a -> {
		String newName = JOptionPane.showInputDialog(this, "Name for new tag under " + clickedTag.name() + ":");
		if (newName != null && !newName.trim().isEmpty()) {
			manager.createOntologyClass(newName.trim(), List.of(clickedTag.identity()), null);
			currentLakeService.saveChanges();
			
			// BACKEND WORKAROUND: The domain layer fails to initialize `identityNumber` dynamically.
			// By cycling the active state via the driving port, we trigger hydration on reload,
			// mapping the surrogate key indexes natively without penetrating the hexagonal boundary.
			Path currentPath = currentLakeService.getRootPath();
			mac.servicePort.deleteLake(currentLakeService);
			mac.servicePort.loadDataLake(currentPath.toString());
		}
	});
	
	removeTag.addActionListener(a -> {
		int result = JOptionPane.showConfirmDialog(this, "Delete tag '" + clickedTag.name() + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			manager.removeOntologyClass(clickedTag.identity());
			currentLakeService.saveChanges();
			mac.wsModeller.triggerLakeRefresh(currentLakeService);
		}
	});
	
	contextMenu.add(createTag);
	contextMenu.add(removeTag);
	contextMenu.show(tree, e.getX(), e.getY());
}

@Override
protected void clearHierarchy() {
	if (treeModel != null) {
		treeModel.setRoot(null);
		treeModel.reload();
	}
}

// --- WORKSPACE LISTENER IMPLEMENTATION MIGRATION ---

@Override
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {// 1. Cache the current expansion state before destroying the model
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
	
	tree.setModel(treeModel);// 2. Restore the expansion state
	if (expandedPaths != null) {
		while (expandedPaths.hasMoreElements()) {
			TreePath path = expandedPaths.nextElement();
			// We must find the new nodes that match the old path strings
			restorePath(tree, rootNode, path, 0);
		}
	} else {
		tree.expandRow(0); // Fallback to root
	}
}
// Helper method to safely re-expand nodes by comparing their string values
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

@Override
public void onSelectedClassChange(OntologyClass ontologyClass) { }
@Override
public void onDomainChange(OntologyClass newDomain) { }

}