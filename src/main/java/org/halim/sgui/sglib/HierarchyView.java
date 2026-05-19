package org.halim.sgui.sglib;

import org.halim.pd.OntoDirectoryService;
import org.halim.pd.OntologyReadingService;


/** Views that display or edit the overall structure (Tree, Venn, Graph) */
public abstract class HierarchyView extends WorkSpaceViewPanel {


public OntologyReadingService hierarchy;


protected abstract void clearHierarchy();


@Override
public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {
	clearHierarchy();
	this.hierarchy = dataLakeService.getOntologyReadingService();
	Utilities.repaintJFrame(this);
}

}