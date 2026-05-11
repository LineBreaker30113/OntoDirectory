package org.halim.hport;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyFilter;

import java.util.ArrayList;

public interface OntologyReadingService {

OntologyClass getRootOntologyClass();
OntologyClass getClassFromName(String name);
ArrayList<FileInterface> getOntologyElements(OntologyClass classElement);

boolean isElementOf(OntologyClass classElement, FileInterface file);
ArrayList<FileInterface> getOntologyElements(OntologyFilter filter);

interface OntologyManagingService extends OntologyReadingService {
	
	void createOntologyClass(String name, ArrayList<String> parents, ArrayList<String> children);
	void removeOntologyClass(String name);
	void addParent(OntologyClass ontologyClass, String parentName);
	void addChildren(OntologyClass ontologyClass, String childrenName);
	void addElement(OntologyClass classElement, FileInterface file);
	void removeElement(OntologyClass classElement, FileInterface file);
	void renameOntologyClass(OntologyClass classElement, String newName);
	
	void undo(); void redo();
	
}

}
