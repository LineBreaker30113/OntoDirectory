package org.halim.gui.vpanels;

import org.halim.ApplicationController;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyHierarchy;
import org.halim.gui.library.HierarchyView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.Queue;

public class HierarchyTreeView extends HierarchyView {

private final ApplicationController mac;
private final JTree tree;
private DefaultTreeModel treeModel;

public HierarchyTreeView(ApplicationController mac) {
	this.mac = mac;
	setLayout(new BorderLayout());
	
	tree = new JTree((javax.swing.tree.TreeNode) null);
	tree.setRootVisible(true);
	tree.setShowsRootHandles(true);
	tree.setBackground(new Color(40, 40, 40));
	tree.setForeground(Color.WHITE);
	
	// NATIVE SELECTION: Reliable left-click handling
	tree.addTreeSelectionListener(e -> {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		if (node == null || hierarchy == null) return;
		
		String clickedTagName = node.getUserObject().toString();
		OntologyClass targetClass = hierarchy.manager.getClassFromName(clickedTagName);
		if (targetClass != null) {
			mac.getWSC().broadcastTagSelection(targetClass);
		}
	});
	
	// CONTEXT MENU: Isolated to popup triggers
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
		int selRow = tree.getRowForLocation(e.getX(), e.getY());
		TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
		
		if (selRow != -1 && selPath != null) {
			tree.setSelectionPath(selPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
			String clickedTagName = node.getUserObject().toString();
			showContextMenu(e, clickedTagName);
		}
	}
}

private void showContextMenu(MouseEvent e, String clickedTagName) {
	JPopupMenu contextMenu = new JPopupMenu();
	
	JMenuItem createTag = new JMenuItem("Create Sub-Tag Here");
	JMenuItem removeTag = new JMenuItem("Delete Tag");
	JMenuItem addParent = new JMenuItem("Add Existing Tag as Parent...");
	JMenuItem removeParentLink = new JMenuItem("Remove Parent Link...");
	
	createTag.addActionListener(a -> {
		String newName = JOptionPane.showInputDialog(this, "Name for new tag under " + clickedTagName + ":");
		if (newName != null && !newName.trim().isEmpty()) {
			OntologyClass parent = hierarchy.manager.getClassFromName(clickedTagName);
			if (parent != null) {
				hierarchy.manager.createNewSubClass(parent, newName.trim());
				mac.getWSC().activeDataLake.saveStateToDisk();
				onLakeSwitched(hierarchy); // Refresh GUI Tree
			}
		}
	});
	
	removeTag.addActionListener(a -> {
		int result = JOptionPane.showConfirmDialog(this, "Delete tag '" + clickedTagName + "'? Files inside will NOT be deleted.", "Confirm", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			OntologyClass target = hierarchy.manager.getClassFromName(clickedTagName);
			if (target != null && !target.name.equals("File")) {
				hierarchy.manager.removeClass(hierarchy.getIdentityFromClass(target));
				mac.getWSC().activeDataLake.saveStateToDisk();
				onLakeSwitched(hierarchy);
			}
		}
	});
	
	addParent.addActionListener(a -> {
		String parentName = JOptionPane.showInputDialog(this, "Enter name of existing tag to become parent of " + clickedTagName + ":");
		if (parentName != null && !parentName.trim().isEmpty()) {
			hierarchy.manager.addParent(clickedTagName, parentName.trim());
			mac.getWSC().activeDataLake.saveStateToDisk();
			onLakeSwitched(hierarchy);
		}
	});
	
	removeParentLink.addActionListener(a -> {
		String parentName = JOptionPane.showInputDialog(this, "Enter parent name to sever connection from " + clickedTagName + ":");
		if (parentName != null && !parentName.trim().isEmpty()) {
			hierarchy.manager.removeParent(clickedTagName, parentName.trim());
			mac.getWSC().activeDataLake.saveStateToDisk();
			onLakeSwitched(hierarchy);
		}
	});
	
	contextMenu.add(createTag);
	contextMenu.add(removeTag);
	contextMenu.addSeparator();
	contextMenu.add(addParent);
	contextMenu.addSeparator();
	contextMenu.add(removeParentLink);
	
	contextMenu.show(tree, e.getX(), e.getY());
}

@Override
protected void clearHierarchy() {
	if (treeModel != null) {
		treeModel.setRoot(null);
		treeModel.reload();
	}
}

@Override
public void onLakeSwitched(OntologyHierarchy newHierarchy) {
	clearHierarchy();
	this.hierarchy = newHierarchy;
	if (newHierarchy == null) return;
	
	OntologyClass rootClass = newHierarchy.manager.getClassFromName("File");
	if(rootClass == null) return;
	
	DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootClass.name);
	treeModel = new DefaultTreeModel(rootNode);
	
	Queue<Object[]> queue = new LinkedList<>();
	queue.add(new Object[]{rootClass, rootNode});
	
	while (!queue.isEmpty()) {
		Object[] current = queue.poll();
		OntologyClass currentClass = (OntologyClass) current[0];
		DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) current[1];
		
		for (OntologyClass childClass : currentClass.children) {
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childClass.name);
			currentNode.add(childNode);
			queue.add(new Object[]{childClass, childNode});
		}
	}
	
	tree.setModel(treeModel);
	tree.expandRow(0);
}
}