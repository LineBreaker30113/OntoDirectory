package org.halim.dlake;

import org.halim.hport.OntoDirectoryException;
import org.halim.hport.OntologyReadingService;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class OntologyHierarchyNew {

public ArrayList<OntologyClass> ontologyClasses;
public ArrayList<ArrayList<FileInterface>> filesPerOntology;
public ArrayList<FileInterface> fileInterfaces;

public OntologyHierarchyNew() {
	ontologyClasses = new ArrayList<>();
	filesPerOntology = new ArrayList<>();
	fileInterfaces = new ArrayList<>();
	ontologyClasses.add(OntologyClass.makeROOT_ONTOLOGY_CLASS("File"));
}

// /////////////////// Storing managements:

public void loadAllFrom(@NotNull OntologyStorageService storageService, Path filesRoot) throws IOException {
	storageService.loadOntology(filesRoot, this);
	
}
public void saveTo(@NotNull OntologyStorageService storageService, Path filesRoot) throws IOException {
	storageService.saveOntologyHierarchy(filesRoot, this);
	
}

// ////////////////// Ontology Managements:

public OntologyClass getClassFromIdentity(int anInt) { return ontologyClasses.get(anInt); }

public int getIdentityFromClass(@NotNull OntologyClass oc) { return ontologyClasses.indexOf(oc); }


// ////////////////// Ontology Service:


public class OntologyHierarchyReader implements OntologyReadingService {
	public final OntologyClass ROOT_TEMPORAL_CLASS;
	public final OntologyHierarchyNew hierarchy;
	public OntologyClass domainEntry; // FIX_LATER TODO when sub tags has parent outside of domain.
	
	public OntologyHierarchyReader() {
		ROOT_TEMPORAL_CLASS = OntologyClass.makeROOT_ONTOLOGY_CLASS("View Since:" + new Date().getTime());
		hierarchy = OntologyHierarchyNew.this;
	}
	
	@Override public OntologyClass getRootOntologyClass() { return ontologyClasses.getFirst(); }
	@Override
	public OntologyClass getClassFromName(String name) {
		for(OntologyClass oc : ontologyClasses) { if(oc.name.equals(name)) { return oc; } }
		return null;
	}
	
	@Override public String getDomain() { return domainEntry.name; }
	@Override public void setDomain(OntologyClass domain) { this.domainEntry = domain; }
	@Override
	public ArrayList<FileInterface> getOntologyElements(String className) {
		return filesPerOntology.get(getIdentityFromClass(getClassFromName(className)));
	}
	@Override
	public ArrayList<FileInterface> getAllOntologyElements(String className) {
		OntologyClass oc = getClassFromName(className);
		HashSet<FileInterface> interfaces = new HashSet<>();
		interfaces.addAll(filesPerOntology.get(getIdentityFromClass(oc)));
		for(OntologyClass sub : oc.children) { interfaces.addAll(getAllOntologyElementsSet(sub)); };
		ArrayList<FileInterface> result = new ArrayList<>();
		result.addAll(interfaces);
		return result;
	}
	public HashSet<FileInterface> getAllOntologyElementsSet(OntologyClass ontologyClass) {
		HashSet<FileInterface> interfaces = new HashSet<>();
		interfaces.addAll(filesPerOntology.get(getIdentityFromClass(ontologyClass)));
		for(OntologyClass sub : ontologyClass.children) { interfaces.addAll(getAllOntologyElementsSet(sub)); };
		return interfaces;
	}
	
	@Override public boolean isElementOf(String className, FileInterface file) { return getOntologyElements(className).contains(file); }
	@Override
	public boolean isElementForFilter(OntologyClass ontologyClass, FileInterface file) {
		return filesPerOntology.get(getIdentityFromClass(ontologyClass)).contains(file);
	}
	@Override
	public boolean isDescendentForFilter(OntologyClass ontologyClass, FileInterface file) {
		return false;
	}
	
	@Override
	public ArrayList<FileInterface> getOntologyElements(OntologyFilter filter) {
		ArrayList<FileInterface> result = new ArrayList<>();
		for (FileInterface file : hierarchy.files) {
			if (filter.filter(file)) {
				result.add(file);
			}
		}
		return result;
	}
	
	@Override
	public void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener) {
	
	}
	
	@Override
	public boolean isElementOfFilter(OntologyClass ontologyClass, FileInterface file) {
		return false;
	}
}

// Inherits reader capabilities to satisfy the port contract
public class OntologyHierarchyManager extends OntologyHierarchy.OntologyHierarchyReader implements OntologyReadingService.OntologyManagingService {
	
	@Override
	public void createOntologyClass(String name, ArrayList<String> parentNames, ArrayList<String> childrenNames) {
		OntologyClass noc = new OntologyClass(name, getRootOntologyClass());
		if (parentNames != null) {
			for (String pName : parentNames) {
				OntologyClass parent = getClassFromName(pName);
				if (parent != null) {
					noc.addParent(parent);
				} else {
					throw new OntoDirectoryException.NullGivenAsOntologyClassException(
						  "One element in the \"parents\" array of \"" + name + "\" was null");
				}
			}
		}
		if (childrenNames != null) {
			for (String cName : childrenNames) {
				OntologyClass child = getClassFromName(cName);
				if (child != null) {
					child.addParent(noc);
				} else {
					throw new OntoDirectoryException.NullGivenAsOntologyClassException(
						  "One element in the \"children\" array of \"" + name + "\" was null");
				}
			}
		}
		
		ontologyClasses.add(noc);
		ontologyContainers.add(new OntologyHierarchy.OntologyElements(noc));
	}
	public void createOntologyClass(String name) { createOntologyClass(name, null, null); }
	public void createNewSubClass(String parentC, String name) {
		createOntologyClass(name, (ArrayList<String>) List.of(parentC), null);
	}
	
	@Override
	public void removeOntologyClass(String name) {
		OntologyClass oc = getClassFromName(name);
		if (oc == null || oc == getRootOntologyClass()) return;
		
		// Safely detach from DAG
		for (OntologyClass parent : oc.parents) { parent.children.remove(oc); }
		for (OntologyClass child : oc.children) { child.parents.remove(oc); }
		
		ontologyClasses.remove(oc);
		ontologyContainers.remove(getElementsFromClass(oc));
	}
	
	@Override
	public void addParent(String className, String parentName) {
		getClassFromName(className).addParent(getClassFromName(parentName));
	}
	@Override
	public void removeParent(String className, String parentName) {
		getClassFromName(className).removeParent(getClassFromName(parentName));
	}
	
	@Override
	public void addElement(String className, FileInterface file) {
		addElementForManager(getClassFromName(className), file);
	}
	public void addElementForManager(OntologyClass ontologyClass, FileInterface file) {
		getElementsFromClass(ontologyClass).files.add(file);
	}
	
	@Override
	public void removeElement(String className, FileInterface file) {
	
	}
	
	@Override
	public void renameOntologyClass(String className, String newName) {
	
	}
	
	@Override
	public void undo() {
		// MVP: History tracking deferred.
		System.err.println("Undo action invoked, but history tracking is deferred for MVP.");
	}
	
	@Override
	public void redo() {
		// MVP: History tracking deferred.
		System.err.println("Redo action invoked, but history tracking is deferred for MVP.");
	}
	
	// Legacy compat
	public void addFileToClass(FileInterface fi, OntologyClass oc) { addElementForManager(oc, fi); }
	
	@Override
	public ArrayList<FileInterface> getOntologyElements(String className) {
		return null;
	}
	
	@Override
	public boolean isElementOf(String className, FileInterface file) {
		return false;
	}
	
	@Override
	public void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener) {
	
	}
	
	@Override
	public boolean isElementOfFilter(OntologyClass ontologyClass, FileInterface file) {
		return false;
	}
}

}
