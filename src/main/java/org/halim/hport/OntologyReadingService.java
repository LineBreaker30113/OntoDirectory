package org.halim.hport;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyFilter;

import java.util.ArrayList;

public interface OntologyReadingService {

OntologyClass getRootOntologyClass();
OntologyClass getClassFromName(String name);
ArrayList<FileInterface> getOntologyElements(String className);
boolean isElementOf(String className, FileInterface file);
ArrayList<FileInterface> getOntologyElements(OntologyFilter filter);


void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener);

interface OntologyServiceListener {
	void onChange();
}

interface OntologyManagingService extends OntologyReadingService {
	
	void createOntologyClass(String name, ArrayList<String> parents, ArrayList<String> children);
	void removeOntologyClass(String name);
	void addParent(String className, String parentName);
	void removeParent(String className, String parentName);
	void addElement(String className, FileInterface file);
	void removeElement(String className, FileInterface file);
	void renameOntologyClass(String className, String newName);
	
	void undo(); void redo();
	
}

boolean isElementOfFilter(OntologyClass ontologyClass, FileInterface file);

}
