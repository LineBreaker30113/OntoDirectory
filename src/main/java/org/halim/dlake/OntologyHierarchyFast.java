package org.halim.dlake;

import org.halim.OntoDirectoryException;
import org.halim.hport.OntologyMemento;
import org.halim.hport.OntologyReadingService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;


/**
 * For speed, it uses only the identity numbers, good for organizing the data lake.
 * But it is forbidden to interact with the files during {@link OntologyHierarchyFast#vacuumDatabase(Path)}.
 */
public class OntologyHierarchyFast {

public ArrayList<OntologyClass> ontologyClasses;
public ArrayList<ArrayList<FileInterface>> filesPerOntology;
public ArrayList<FileInterface> fileInterfaces;

public OntologyHierarchyFast() {
	ontologyClasses = new ArrayList<>();
	filesPerOntology = new ArrayList<>();
	fileInterfaces = new ArrayList<>();
	ontologyClasses.add(OntologyClass.makeROOT_ONTOLOGY_CLASS("File"));
	filesPerOntology.add(new ArrayList<>());
}

// /////////////////// Storing managements:

//public void loadAllFrom(@NotNull OntologyStorageService storageService, Path filesRoot) throws IOException {
//	storageService.loadOntology(filesRoot, this);
//}

/** All the actions to take after loading the data lake. For now just creating the fast arrays. */
public void onLoad() {
	ontologyClasses.forEach(__ -> filesPerOntology.add(new ArrayList<>()));
	fileInterfaces.forEach(fi -> { fi.tagsByIdentity.forEach(fii -> filesPerOntology.get(fii).add(fi)); });
}


/**
 * Defragments the Data Lake by removing tombstoned (null) entries and recalculating
 * all foreign key references (Integer Identities).
 * WARNING: This alters persistent Integer IDs. Do not run implicitly on standard saves.
 */
public void vacuumDatabase(Path dataLakePath) {
	// 1. Fast O(N) removal of nulls from file interfaces
	fileInterfaces.removeIf(Objects::isNull);
	
	// Re-sync File identities and disk names
	for (int i = 0; i < fileInterfaces.size(); i++) {
		FileInterface fi = fileInterfaces.get(i);
		fi.renameDisk(FileInterface.getDiskNameFor(i), dataLakePath);
		fi.identity = i;
	}
	
	ArrayList<Integer> toRemove = new ArrayList<>();
	
	// 2. Identify dead ontology classes
	for (int i = 0; i < ontologyClasses.size(); i++) {
		if (ontologyClasses.get(i) == null) {
			toRemove.add(i);
		}
	}
	
	if (toRemove.isEmpty()) return;
	
	// 3. Recalculate Foreign Keys (Tags attached to files) safely
	for (FileInterface fi : fileInterfaces) {
		if (fi.tagsByIdentity == null) continue;
		
		for (int ti = fi.tagsByIdentity.size() - 1; ti >= 0; ti--) {
			int oldTagId = fi.tagsByIdentity.get(ti);
			
			if (toRemove.contains(oldTagId)) {
				fi.tagsByIdentity.remove(ti);
				continue;
			}
			
			int shiftAmount = 0;
			for (int deadIndex : toRemove) {
				if (deadIndex < oldTagId) shiftAmount++;
				else break;
			}
			fi.tagsByIdentity.set(ti, oldTagId - shiftAmount);
		}
	}
	
	// 4. Compact the actual arrays (Backwards to prevent index shifting bugs)
	for (int i = toRemove.size() - 1; i >= 0; i--) {
		int deadIndex = toRemove.get(i);
		ontologyClasses.remove(deadIndex);
		filesPerOntology.remove(deadIndex);
	}
	
	System.out.println("Database Vacuum Complete. Reclaimed " + toRemove.size() + " class identities.");
}

//public void saveTo(@NotNull OntologyStorageService storageService, Path filesRoot) throws IOException {
//	storageService.saveOntologyHierarchy(filesRoot, this);
//}

// ////////////////// Ontology Managements:

public OntologyClass getClassFromIdentity(int anInt) { return ontologyClasses.get(anInt); }

public int getIdentityFromClass(@NotNull OntologyClass oc) { return ontologyClasses.indexOf(oc); }

// ////////////////// Ontology Service:

public class OntologyHierarchyReader implements OntologyReadingService {
	public final OntologyClass ROOT_TEMPORAL_CLASS;
	public final OntologyHierarchyFast hierarchy;
	public OntologyClass domainEntry; // FIX_LATER TODO when sub tags has parent outside of domain.
	
	public OntologyHierarchyReader() {
		ROOT_TEMPORAL_CLASS = OntologyClass.makeROOT_ONTOLOGY_CLASS("View Since:" + new Date().getTime());
		hierarchy = OntologyHierarchyFast.this;
	}
	
	@Override
	public OntologyClass getRootOntologyClass() { return ontologyClasses.getFirst(); }
	
	@Override
	public OntologyClass getClassFromName(String name) {
		for(OntologyClass oc : ontologyClasses) {
			if(oc != null && oc.name.equals(name)) return oc;
		}
		return null;
	}
	
	@Override
	public String getDomain() { return domainEntry != null ? domainEntry.name : null; }
	@Override
	public void setDomain(OntologyClass domain) { this.domainEntry = domain; }
	
	@Override
	public ArrayList<FileInterface> getOntologyElements(String className) {
		OntologyClass oc = getClassFromName(className);
		if (oc == null) return new ArrayList<>();
		return filesPerOntology.get(getIdentityFromClass(oc));
	}
	
	@Override
	public ArrayList<FileInterface> getAllOntologyElements(String className) {
		OntologyClass oc = getClassFromName(className);
		if (oc == null) return new ArrayList<>();
		
		HashSet<FileInterface> interfaces = new HashSet<>(filesPerOntology.get(getIdentityFromClass(oc)));
		for(OntologyClass sub : oc.children) {
			interfaces.addAll(getAllOntologyElementsSet(sub));
		}
		return new ArrayList<>(interfaces);
	}
	
	public HashSet<FileInterface> getAllOntologyElementsSet(OntologyClass ontologyClass) {
		HashSet<FileInterface> interfaces = new HashSet<>(filesPerOntology.get(getIdentityFromClass(ontologyClass)));
		for (OntologyClass sub : ontologyClass.children) {
			interfaces.addAll(getAllOntologyElementsSet(sub));
		}
		return interfaces;
	}
	
	@Override
	public boolean isElementForFilter(OntologyClass ontologyClass, FileInterface file) {
		if (ontologyClass == null) return false;
		return filesPerOntology.get(getIdentityFromClass(ontologyClass)).contains(file);
	}
	
	@Override
	public boolean isDescendentForFilter(OntologyClass ontologyClass, @NotNull FileInterface file) {
		if (file.tagsByIdentity == null) return false;
		for(Integer i : file.tagsByIdentity) {
			OntologyClass tag = getClassFromIdentity(i);
			if(tag != null && tag.isAncestry(ontologyClass)) return true;
		}
		return false;
	}
	
	@Override
	public List<FileInterface> getOntologyElements(@NotNull OntologyFilter filter) {
		return fileInterfaces.stream().filter(filter::filter).toList();
	}
	
	@Override
	public void addOntologyServiceListener(OntologyServiceListener ontologyServiceListener) {
		// Hook into Event Bus when implemented
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
		filesPerOntology.get(targetIndex).forEach(fi -> {
			if(fi.tagsByIdentity != null) {
				fi.tagsByIdentity.remove(Integer.valueOf(targetIndex));
			}
		});
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
			
			if(file.tagsByIdentity == null) {
				file.tagsByIdentity = new ArrayList<>();
			}
			file.tagsByIdentity.add(getIdentityFromClass(targetClass));
			
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
		
		int classId = getIdentityFromClass(targetClass);
		ArrayList<FileInterface> fileList = filesPerOntology.get(classId);
		if (fileList.remove(file)) {
			if(file.tagsByIdentity != null) {
				file.tagsByIdentity.remove(Integer.valueOf(classId));
			}
			
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
			memento.undo(this);
			undoStack.push(memento);
		} finally {
			isReplaying = false;
		}
	}
}
}