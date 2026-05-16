package org.halim.hport;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyFilter;

import java.util.ArrayList;
import java.util.List;

public interface OntologyReadingService {

OntologyClass getRootOntologyClass();

@Deprecated
OntologyClass getClassFromName(String name);

OntologyClass getClassFromIdentity(int identity);

/** Later I realized this is redundant, but too busy to remove. */
@Deprecated
String getDomain();

int getDomainIdentity();

@Deprecated
default void setDomain(String domainName) { setDomain(getClassFromName(domainName)); }

default void setDomain(int domainIdentity) { setDomain(getClassFromIdentity(domainIdentity)); }

void setDomain(OntologyClass domain);

/** Returns the elements that are directly tagged with this. */
@Deprecated
ArrayList<FileInterface> getOntologyElements(String className);

ArrayList<FileInterface> getOntologyElements(int classIdentity);

/** Returns the elements that are in the domain of the Ontology Class. */
@Deprecated
ArrayList<FileInterface> getAllOntologyElements(String className);

ArrayList<FileInterface> getAllOntologyElements(int classIdentity);

/** Checks if the element has the tag. */
@Deprecated
default boolean isElementOf(String className, FileInterface file) {
	return isElementForFilter(getClassFromName(className),  file);
}

default boolean isElementOf(int classIdentity, FileInterface file) {
	return isElementForFilter(getClassFromIdentity(classIdentity), file);
}

/** Checks if the element has a tag descendent of the class. */
@Deprecated
default boolean isDescendentOf(String className, FileInterface file) {
	return isDescendentForFilter(getClassFromName(className),  file);
}

default boolean isDescendentOf(int classIdentity, FileInterface file) {
	return isDescendentForFilter(getClassFromIdentity(classIdentity), file);
}

/** For The Filter Only! Checks if the element has the tag. */
boolean isElementForFilter(OntologyClass ontologyClass, FileInterface file);

/** For The Filter Only! Checks if the element has a tag descendent of the class. */
boolean isDescendentForFilter(OntologyClass ontologyClass, FileInterface file);

/** Returns all the ontology Elements that satisfy the filter for the domain. */
List<FileInterface> getOntologyElements(OntologyFilter filter);


void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener);

interface OntologyServiceListener {
	void setOntologyDomain(OntologyClass ontologyClass);
	void printRequestProcess(Object message);
}

interface OntologyManagingService extends OntologyReadingService {
	
	int filterToClass(OntologyFilter filter, String newClassName);
	void copyContentsTo(int sourceIdentity, int targetIdentity, boolean copyParents, boolean copyChildren, boolean copyFiles);
	
	@Deprecated
	void createOntologyClass(String name, ArrayList<String> parents, ArrayList<String> children);
	/** Return the identity of the newly created class. */
	int createOntologyClass(String name, List<Integer> parentIds, List<Integer> childrenIds);
	
	@Deprecated
	void removeOntologyClass(String name);
	void removeOntologyClass(int classIdentity);
	
	@Deprecated
	void addParent(String className, String parentName);
	void addParent(int childId, int parentId);
	
	@Deprecated
	void removeParent(String className, String parentName);
	void removeParent(int childId, int parentId);
	
	@Deprecated
	void addElement(String className, FileInterface file);
	void addElement(int classIdentity, FileInterface file);
	
	@Deprecated
	void removeElement(String className, FileInterface file);
	void removeElement(int classIdentity, FileInterface file);
	
	@Deprecated
	void renameOntologyClass(String className, String newName);
	void renameOntologyClass(int classIdentity, String newName);
	
	void undo(); void redo();
	
}
}