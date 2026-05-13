package org.halim.hport;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyFilter;

import java.util.ArrayList;

public interface OntologyReadingService {

OntologyClass getRootOntologyClass();
OntologyClass getClassFromName(String name);

/** Later I realized this is redundant, but too busy to remove. */
String getDomain();
default void setDomain(String domainName) { setDomain(getClassFromName(domainName)); }
void setDomain(OntologyClass domain);

/** Returns the elements that are directly tagged with this. */
ArrayList<FileInterface> getOntologyElements(String className);
/** Returns the elements that are in the domain of the Ontology Class. */
ArrayList<FileInterface> getAllOntologyElements(String className);

/** Checks if the element has the tag. */
default boolean isElementOf(String className, FileInterface file) {
	return isElementForFilter(getClassFromName(className),  file);
}
/** Checks if the element has a tag descendent of the class. */
default boolean isDescendentOf(String className, FileInterface file) {
	return isDescendentForFilter(getClassFromName(className),  file);
}

/** For The Filter Only! Checks if the element has the tag. */
boolean isElementForFilter(OntologyClass ontologyClass, FileInterface file);
/** For The Filter Only! Checks if the element has a tag descendent of the class. */
boolean isDescendentForFilter(OntologyClass ontologyClass, FileInterface file);

/** Returns all the ontology Elements that satisfy the filter for the domain. */
ArrayList<FileInterface> getOntologyElements(OntologyFilter filter);


void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener);

interface OntologyServiceListener {
	void setOntologyDomain(OntologyClass ontologyClass);
	void printRequestProcess(Object message);
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
