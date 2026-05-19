package org.halim.sgui.sglib;

import org.halim.dlake.OntologyClass;
import org.halim.pd.OntoDirectoryService;

public interface WorkspaceListener {
	
	void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService);
	
	void onSelectedClassChange(OntologyClass ontologyClass);
	
	void onDomainChange(OntologyClass newDomain);
	
}
