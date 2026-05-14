package org.halim.sgui.sglib;

import org.halim.Listeners.LakeChangeListener;

import javax.swing.*;

/** Views that display or edit the overall structure (Tree, Venn, Graph) */
public abstract class HierarchyView extends JPanel implements LakeChangeListener {
//	public abstract void toggleCollapse();
	public OntologyHierarchy hierarchy;
	protected abstract void clearHierarchy();
	@Override
	public void onLakeSwitched(OntologyHierarchy hierarchy) {
		clearHierarchy();
		this.hierarchy = hierarchy;
		Utilities.repaintJFrame(this);
	}
	public static class ComingSoonPanel extends HierarchyView {
//		@Override public void toggleCollapse() { }
		
		@Override
		protected void clearHierarchy() {
		
		}
	}
}



