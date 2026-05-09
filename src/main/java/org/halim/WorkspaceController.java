package org.halim;

import org.halim.Listeners.LakeChangeListener;
import org.halim.Listeners.TagChangeListener;
import org.halim.dlake.DataLakeManager;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyHierarchy;
import org.halim.gui.library.ContentView;
import org.halim.gui.library.HierarchyView;
import org.halim.gui.WorkspacePanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * The reason it is called WorkspaceController because the view elements
 * are created here which enables the access to the model.
 * */
public class WorkspaceController {// The currently active Data Lake data

public static final String filesVN = "filesV", notesVN = "notesV",
	  treeVN = "treeV", graphVN = "graphV", vennVN = "vennV",
	  vnames[] = new String[] { filesVN, notesVN, treeVN, graphVN, vennVN }
;

private HierarchyView treeView, graphView, vennView;
private ContentView filesView, notesView;

public final ApplicationController owner;

public JPanel getView(@NotNull String name) {
	if(name.equals(filesVN)) { return filesView; }
	if(name.equals(notesVN)) { return notesView; }
	if(name.equals(treeVN)) { return treeView; }
	if(name.equals(graphVN)) { return graphView; }
	if(name.equals(vennVN)) { return vennView; }
	System.out.println("Not proper name in WSC.getView");
	return null;
}

public void broadcastTagSelection(OntologyClass targetClass) {
	if (activeDataLake == null || targetClass == null) return;
	for (TagChangeListener listener : tagChangeListeners) {
		listener.onTagChange(targetClass, activeDataLake.ontologyHierarchy.reader);
	}
}

private WorkspacePanel targetWorkspace;

void initViews() {
	treeView = new HierarchyView() {
		private javax.swing.JTree tree;
		private javax.swing.tree.DefaultTreeModel treeModel;
		
		// Initialization block to configure the Swing layout and add the JTree
		{
			setLayout(new java.awt.BorderLayout());
			tree = new javax.swing.JTree((javax.swing.tree.TreeNode) null); // Start empty
			tree.setRootVisible(true);
			tree.setShowsRootHandles(true);
			
			tree.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mousePressed(java.awt.event.MouseEvent e) { handleMouseClick(e); }
				@Override
				public void mouseReleased(java.awt.event.MouseEvent e) { handleMouseClick(e); }
				
				private void handleMouseClick(java.awt.event.@NotNull MouseEvent e) {
					// 1. Resolve pixel coordinates to a tree node
					int selRow = tree.getRowForLocation(e.getX(), e.getY());
					javax.swing.tree.TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
					
					// If we didn't click on empty space...
					if (selRow != -1 && selPath != null) {
						
						// 2. Handle LEFT CLICK (e.g., load files into ContentView)
						if (javax.swing.SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
							// Extract the node name
							javax.swing.tree.DefaultMutableTreeNode node =
								  (javax.swing.tree.DefaultMutableTreeNode) selPath.getLastPathComponent();
							
							System.out.println("Left clicked on tag: " + node.getUserObject());
							// Future logic: Broadcast to ContentView to load files for this tag
						}
						
						// 3. Handle RIGHT CLICK (Context Menu)
						else if (e.isPopupTrigger() || javax.swing.SwingUtilities.isRightMouseButton(e)) {
							tree.setSelectionPath(selPath);
							javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) selPath.getLastPathComponent();
							String clickedTagName = node.getUserObject().toString();
							
							javax.swing.JPopupMenu contextMenu = new javax.swing.JPopupMenu();
							
							javax.swing.JMenuItem createTag = new javax.swing.JMenuItem("Create New Tag Here");
							javax.swing.JMenuItem removeTag = new javax.swing.JMenuItem("Remove Tag");
							javax.swing.JMenuItem addParent = new javax.swing.JMenuItem("Add Parent...");
							javax.swing.JMenuItem addChild = new javax.swing.JMenuItem("Add Child...");
							javax.swing.JMenuItem makeParent = new javax.swing.JMenuItem("Make Parent of...");
							javax.swing.JMenuItem makeChild = new javax.swing.JMenuItem("Make Child of...");
							
							// Action examples hooking into your Backend Bridge
							createTag.addActionListener(a -> {
								String newName = JOptionPane.showInputDialog(treeView, "Name for new sub-tag under " + clickedTagName + ":");
								if (newName != null && !newName.trim().isEmpty()) {
									activeDataLake.ontologyHierarchy.manager.createNewSubClass(clickedTagName, newName.trim());
									// Trigger a reload of the tree UI
									owner.dispatchLakeLoadRequest(activeDataLake.managerPath);
								}
							});
							
							removeTag.addActionListener(a -> System.out.println("Trigger remove logic for: " + clickedTagName));
							addParent.addActionListener(a -> System.out.println("Trigger Add Parent logic"));
							
							contextMenu.add(createTag);
							contextMenu.add(removeTag);
							contextMenu.addSeparator();
							contextMenu.add(addParent);
							contextMenu.add(addChild);
							contextMenu.addSeparator();
							contextMenu.add(makeParent);
							contextMenu.add(makeChild);
							
							contextMenu.show(tree, e.getX(), e.getY());
						}
					}
				}
			});
			
			add(new javax.swing.JScrollPane(tree), java.awt.BorderLayout.CENTER);
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
			if (newHierarchy == null) return;
			
			
			// 1. Setup the Root Node (Pulling from the actual Data Lake!)
			OntologyClass rootClass = newHierarchy.manager.getClassFromName("File");
			
			javax.swing.tree.DefaultMutableTreeNode rootNode = new javax.swing.tree.DefaultMutableTreeNode(rootClass.name);
			treeModel = new javax.swing.tree.DefaultTreeModel(rootNode);
			
			// 2. Setup the BFS Queue
			// We use an Object array to pair the DAG class with its corresponding visual tree node
			Queue<Object[]> queue = new LinkedList<>();
			queue.add(new Object[]{rootClass, rootNode});
			
			// 3. Execute Breadth-First Search
			while (!queue.isEmpty()) {
				Object[] current = queue.poll();
				OntologyClass currentClass = (OntologyClass) current[0];
				javax.swing.tree.DefaultMutableTreeNode currentNode = (javax.swing.tree.DefaultMutableTreeNode) current[1];
				
				for (OntologyClass childClass : currentClass.children) {
					// Create the visual node for the child
					javax.swing.tree.DefaultMutableTreeNode childNode = new javax.swing.tree.DefaultMutableTreeNode(childClass.name);
					
					// Append to the visual tree
					currentNode.add(childNode);
					
					// Push to the queue to process this child's descendants
					queue.add(new Object[]{childClass, childNode});
				}
			}
			
			// 4. Apply the newly built model to the JTree
			tree.setModel(treeModel);
		}
	};
	graphView = new HierarchyView() {
		@Override
		protected void clearHierarchy() {
		
		}
		
		@Override
		public void onLakeSwitched(OntologyHierarchy newHierarchy) {
		
		}
	};
	vennView = new HierarchyView() {
		@Override
		protected void clearHierarchy() {
		
		}
		
		@Override
		public void onLakeSwitched(OntologyHierarchy newHierarchy) {
		
		}
	};
	lakeChangeListeners.add(treeView);
	lakeChangeListeners.add(graphView);
	lakeChangeListeners.add(vennView);
	filesView = new ContentView() {
		@Override
		public void onTagChange(OntologyClass ntag, OntologyHierarchy.OntologyHierarchyReader reader) {
		
		}
	};
	notesView = new ContentView() {
		@Override
		public void onTagChange(OntologyClass ntag, OntologyHierarchy.OntologyHierarchyReader reader) {
		
		}
	};
	tagChangeListeners.add(filesView);
	tagChangeListeners.add(notesView);
	new Thread(owner::registerWSViews).start();
}

public WorkspaceController(ApplicationController owner) {
	this.owner = owner;
	SwingUtilities.invokeLater(this::initViews);
	
}

public DataLakeManager activeDataLake;

private List<LakeChangeListener> lakeChangeListeners = new ArrayList<>();
private List<TagChangeListener> tagChangeListeners = new ArrayList<>();

public void setActiveLake(DataLakeManager newDataLake) {
	activeDataLake = newDataLake;
	// Broadcast the change to all UI components!
	for(LakeChangeListener view : lakeChangeListeners) {
		view.onLakeSwitched(newDataLake.ontologyHierarchy);
	}
	// Broadcast the change to all UI components!
	for(TagChangeListener view : tagChangeListeners) {
		view.onTagChange(activeDataLake.ontologyHierarchy.manager.getClassFromName("File"), activeDataLake.ontologyHierarchy.reader);
	}
}

}
