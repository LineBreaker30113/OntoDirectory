package org.halim.dlake;

import org.halim.hport.OntoDirectoryException;
import org.halim.hport.OntologyMemento;
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
		for (OntologyClass sub : ontologyClass.children) {
			interfaces.addAll(getAllOntologyElementsSet(sub));
		}
		;
		return interfaces;
	}
	
	@Override
	public boolean isElementForFilter(OntologyClass ontologyClass, FileInterface file) {
		return filesPerOntology.get(getIdentityFromClass(ontologyClass)).contains(file);
	}
	@Override
	public boolean isDescendentForFilter(OntologyClass ontologyClass, @NotNull FileInterface file) {
		for(Integer i : file.tagsByIdentity) if(getClassFromIdentity(i).isAncestry(ontologyClass)) return true;
		return false;
	}
	
	@Override
	public List<FileInterface> getOntologyElements(@NotNull OntologyFilter filter) {
		return fileInterfaces.stream().filter(filter::filter).toList();
	}
	
	@Override
	public void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener) {
	
	}
}

// Inherits reader capabilities to satisfy the port contract
public class OntologyHierarchyManager extends OntologyHierarchyReader implements OntologyReadingService.OntologyManagingService {
	
	private final Stack<OntologyMemento> undoStack = new Stack<>();
	private final Stack<OntologyMemento> redoStack = new Stack<>();
	
	// Guardrail to prevent recording historical actions while currently playing back history
	private boolean isReplaying = false;
	
	private void recordAction(OntologyMemento memento) {
		if (isReplaying) return;
		undoStack.push(memento);
		redoStack.clear(); // Any new action completely invalidates the redo future
	}
	
	@Override
	public void createOntologyClass(String name, ArrayList<String> parentNames, ArrayList<String> childrenNames) {
		if (getClassFromName(name) != null) {
			throw new OntoDirectoryException("Class '" + name + "' already exists.");
		}
		
		OntologyClass noc = new OntologyClass(name, getRootOntologyClass());
		if (parentNames != null) {
			for (String pName : parentNames) {
				OntologyClass parent = getClassFromName(pName);
				if (parent != null) {
					noc.addParent(parent);
				} else {
					throw new OntoDirectoryException("One element in the parents array of '" + name + "' was null");
				}
			}
		}
		if (childrenNames != null) {
			for (String cName : childrenNames) {
				OntologyClass child = getClassFromName(cName);
				if (child != null) {
					child.addParent(noc);
				} else {
					throw new OntoDirectoryException("One element in the children array of '" + name + "' was null");
				}
			}
		}
		
		// Locate a tombstoned index, or append to the end
		int insertIndex = ontologyClasses.indexOf(null);
		if (insertIndex == -1) {
			ontologyClasses.add(noc);
			filesPerOntology.add(new ArrayList<>());
		} else {
			ontologyClasses.set(insertIndex, noc);
			filesPerOntology.set(insertIndex, new ArrayList<>());
		}
		
		OntologyMemento memento = new OntologyMemento();
		memento.actionType = OntologyMemento.ActionType.Create;
		memento.primaryTarget = name;
		recordAction(memento);
	}
	
	public void createOntologyClass(String name) { createOntologyClass(name, null, null); }
	public void createNewSubClass(String parentC, String name) {
		createOntologyClass(name, new ArrayList<>(List.of(parentC)), null);
	}
	
	@Override
	public void removeOntologyClass(String name) {
		OntologyClass oc = getClassFromName(name);
		if (oc == null || oc == getRootOntologyClass()) return;
		
		// Safely detach from DAG
		for (OntologyClass parent : oc.parents) { parent.children.remove(oc); }
		for (OntologyClass child : oc.children) { child.parents.remove(oc); }
		
		// TOMBSTONING: Do not use .remove() as it shifts the contiguous memory array
		int targetIndex = getIdentityFromClass(oc);
		ontologyClasses.set(targetIndex, null);
		filesPerOntology.set(targetIndex, null);
		
		OntologyMemento memento = new OntologyMemento();
		memento.actionType = OntologyMemento.ActionType.Remove;
		memento.primaryTarget = name;
		recordAction(memento);
	}
	
	@Override
	public void addParent(String className, String parentName) {
		OntologyClass child = getClassFromName(className);
		OntologyClass parent = getClassFromName(parentName);
		if (child != null && parent != null) {
			child.addParent(parent);
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.AddParent;
			memento.primaryTarget = className;
			memento.secondaryTarget = parentName;
			recordAction(memento);
		}
	}
	
	@Override
	public void removeParent(String className, String parentName) {
		OntologyClass child = getClassFromName(className);
		OntologyClass parent = getClassFromName(parentName);
		if (child != null && parent != null) {
			child.removeParent(parent);
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.RemoveParent;
			memento.primaryTarget = className;
			memento.secondaryTarget = parentName;
			recordAction(memento);
		}
	}
	
	@Override
	public void addElement(String className, FileInterface file) {
		OntologyClass targetClass = getClassFromName(className);
		if (targetClass == null) return;
		
		ArrayList<FileInterface> fileList = filesPerOntology.get(getIdentityFromClass(targetClass));
		if (!fileList.contains(file)) {
			fileList.add(file);
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.AddElement;
			memento.primaryTarget = className;
			memento.targetFile = file;
			recordAction(memento);
		}
	}
	
	@Override
	public void removeElement(String className, FileInterface file) {
		OntologyClass targetClass = getClassFromName(className);
		if (targetClass == null) return;
		
		ArrayList<FileInterface> fileList = filesPerOntology.get(getIdentityFromClass(targetClass));
		if (fileList.remove(file)) {
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.RemoveElement;
			memento.primaryTarget = className;
			memento.targetFile = file;
			recordAction(memento);
		}
	}
	
	@Override
	public void renameOntologyClass(String className, String newName) {
		OntologyClass targetClass = getClassFromName(className);
		if (targetClass != null && targetClass != getRootOntologyClass()) {
			targetClass.name = newName;
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.Rename;
			memento.primaryTarget = className;
			memento.secondaryTarget = newName;
			recordAction(memento);
		}
	}
	
	@Override
	public void undo() {
		if (undoStack.isEmpty()) return;
		isReplaying = true;
		try {
			OntologyMemento memento = undoStack.pop();
			memento.undo(this);
			redoStack.push(memento);
		} finally {
			isReplaying = false;
		}
	}
	
	@Override
	public void redo() {
		if (redoStack.isEmpty()) return;
		isReplaying = true;
		try {
			OntologyMemento memento = redoStack.pop();
			memento.undo(this); // undoing an inverse operation executes the original action
			undoStack.push(memento);
		} finally {
			isReplaying = false;
		}
	}
}

}
