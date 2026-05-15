package org.halim.dlake;

import org.halim.OntoDirectoryException;
import org.halim.hport.OntologyMemento;
import org.halim.hport.OntologyReadingService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

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


private void ifNotNull(OntologyClass instance, Consumer<OntologyClass> action) { if(instance != null) action.accept(instance); }
/** All the actions to take after loading the data lake. For now just creating the fast arrays. */
public void onLoad() {
	Iterator<OntologyClass> iterator = ontologyClasses.iterator(); int ocin = 0;
	while (iterator.hasNext()) {
		final int ci = ocin++; ifNotNull(iterator.next(), oc -> oc.identityNumber = ci);
		filesPerOntology.add(new ArrayList<>());
	}
	fileInterfaces.forEach(fi -> { fi.tagsByIdentity.forEach(fii -> filesPerOntology.get(fii).add(fi)); });
}

/**
 * Defragments the Data Lake by removing tombstoned (null) entries and recalculating
 * all foreign key references (Integer Identities).
 * WARNING: This alters persistent Integer IDs. Do not run implicitly on standard saves.
 */
public void vacuumDatabase(Path dataLakePath) {
	fileInterfaces.removeIf(Objects::isNull);
	
	for (int i = 0; i < fileInterfaces.size(); i++) {
		FileInterface fi = fileInterfaces.get(i);
		fi.renameDisk(FileInterface.getDiskNameFor(i), dataLakePath);
		fi.identity = i;
	}
	
	ArrayList<Integer> toRemove = new ArrayList<>();
	
	for (int i = 0; i < ontologyClasses.size(); i++) {
		if (ontologyClasses.get(i) == null) {
			toRemove.add(i);
		}
	}
	
	if (toRemove.isEmpty()) return;
	
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
	
	for (int i = toRemove.size() - 1; i >= 0; i--) {
		int deadIndex = toRemove.get(i);
		ontologyClasses.remove(deadIndex);
		filesPerOntology.remove(deadIndex);
	}
	
	System.out.println("Database Vacuum Complete. Reclaimed " + toRemove.size() + " class identities.");
}

public OntologyClass getClassFromIdentity(int anInt) {
	if (anInt < 0 || anInt >= ontologyClasses.size()) return null;
	return ontologyClasses.get(anInt);
}

public int getIdentityFromClass(@NotNull OntologyClass oc) { return ontologyClasses.indexOf(oc); }

// ////////////////// Ontology Service:

public class OntologyHierarchyReader implements OntologyReadingService {
	public final OntologyClass ROOT_TEMPORAL_CLASS;
	public final OntologyHierarchyFast hierarchy;
	public OntologyClass domainEntry;
	
	public OntologyHierarchyReader() {
		ROOT_TEMPORAL_CLASS = OntologyClass.makeROOT_ONTOLOGY_CLASS("View Since:" + new Date().getTime());
		hierarchy = OntologyHierarchyFast.this;
	}
	
	@Override
	public OntologyClass getRootOntologyClass() { return ontologyClasses.getFirst(); }
	
	@Override
	@Deprecated
	public OntologyClass getClassFromName(String name) {
		for(OntologyClass oc : ontologyClasses) {
			if(oc != null && oc.name.equals(name)) return oc;
		}
		return null;
	}
	
	@Override
	public OntologyClass getClassFromIdentity(int identity) {
		return OntologyHierarchyFast.this.getClassFromIdentity(identity);
	}
	
	@Override
	@Deprecated
	public String getDomain() { return domainEntry != null ? domainEntry.name : null; }
	
	@Override
	public int getDomainIdentity() { return domainEntry != null ? getIdentityFromClass(domainEntry) : -1; }
	
	@Override
	public void setDomain(OntologyClass domain) { this.domainEntry = domain; }
	
	@Override
	@Deprecated
	public ArrayList<FileInterface> getOntologyElements(String className) {
		OntologyClass oc = getClassFromName(className);
		if (oc == null) return new ArrayList<>();
		return filesPerOntology.get(getIdentityFromClass(oc));
	}
	
	@Override
	public ArrayList<FileInterface> getOntologyElements(int classIdentity) {
		if (classIdentity < 0 || classIdentity >= filesPerOntology.size()) return new ArrayList<>();
		return filesPerOntology.get(classIdentity);
	}
	
	@Override
	@Deprecated
	public ArrayList<FileInterface> getAllOntologyElements(String className) {
		OntologyClass oc = getClassFromName(className);
		if (oc == null) return new ArrayList<>();
		
		HashSet<FileInterface> interfaces = new HashSet<>(filesPerOntology.get(getIdentityFromClass(oc)));
		for(OntologyClass sub : oc.children) {
			interfaces.addAll(getAllOntologyElementsSet(sub));
		}
		return new ArrayList<>(interfaces);
	}
	
	@Override
	public ArrayList<FileInterface> getAllOntologyElements(int classIdentity) {
		OntologyClass oc = getClassFromIdentity(classIdentity);
		if (oc == null) return new ArrayList<>();
		
		HashSet<FileInterface> interfaces = new HashSet<>(filesPerOntology.get(classIdentity));
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
	
	private boolean isReplaying = false;
	
	private void recordAction(OntologyMemento memento) {
		if (isReplaying) return;
		undoStack.push(memento);
		redoStack.clear();
	}
	
	@Override
	@Deprecated
	public void createOntologyClass(String name, ArrayList<String> parentNames, ArrayList<String> childrenNames) {
		if (getClassFromName(name) != null) throw new OntoDirectoryException("Class '" + name + "' already exists.");
		
		OntologyClass noc = new OntologyClass(name, getRootOntologyClass());
		if (parentNames != null) {
			for (String pName : parentNames) {
				OntologyClass parent = getClassFromName(pName);
				if (parent != null) noc.addParent(parent);
				else throw new OntoDirectoryException("One element in the parents array of '" + name + "' was null");
			}
		}
		if (childrenNames != null) {
			for (String cName : childrenNames) {
				OntologyClass child = getClassFromName(cName);
				if (child != null) child.addParent(noc);
				else throw new OntoDirectoryException("One element in the children array of '" + name + "' was null");
			}
		}
		
		insertNewClass(noc);
		
		OntologyMemento memento = new OntologyMemento();
		memento.actionType = OntologyMemento.ActionType.Create;
		memento.primaryTarget = name;
		recordAction(memento);
	}
	
	@Override
	public void createOntologyClass(String name, List<Integer> parentIds, List<Integer> childrenIds) {
		if (getClassFromName(name) != null) throw new OntoDirectoryException("Class '" + name + "' already exists.");
		
		OntologyClass noc = new OntologyClass(name, getRootOntologyClass());
		if (parentIds != null) {
			for (Integer pId : parentIds) {
				OntologyClass parent = getClassFromIdentity(pId);
				if (parent != null) noc.addParent(parent);
				else throw new OntoDirectoryException("One element in the parents array of '" + name + "' was null");
			}
		}
		if (childrenIds != null) {
			for (Integer cId : childrenIds) {
				OntologyClass child = getClassFromIdentity(cId);
				if (child != null) child.addParent(noc);
				else throw new OntoDirectoryException("One element in the children array of '" + name + "' was null");
			}
		}
		
		insertNewClass(noc);
		
		OntologyMemento memento = new OntologyMemento();
		memento.actionType = OntologyMemento.ActionType.Create;
		memento.primaryTarget = name; // Resolved for Memento compatibility
		recordAction(memento);
	}
	
	private void insertNewClass(OntologyClass noc) {
		int insertIndex = ontologyClasses.indexOf(null);
		if (insertIndex == -1) {
			ontologyClasses.add(noc);
			filesPerOntology.add(new ArrayList<>());
		} else {
			ontologyClasses.set(insertIndex, noc);
			filesPerOntology.set(insertIndex, new ArrayList<>());
		}
	}
	
	public void createOntologyClass(String name) { createOntologyClass(name, (List<Integer>)null, null); }
	
	public void createNewSubClass(OntologyClass parentC, String name) {
		createOntologyClass(name, new ArrayList<>(List.of(getIdentityFromClass(parentC))), null);
	}
	
	@Override
	@Deprecated
	public void removeOntologyClass(String name) {
		OntologyClass oc = getClassFromName(name);
		executeRemove(oc);
	}
	
	@Override
	public void removeOntologyClass(int classIdentity) {
		OntologyClass oc = getClassFromIdentity(classIdentity);
		executeRemove(oc);
	}
	
	private void executeRemove(OntologyClass oc) {
		if (oc == null || oc == getRootOntologyClass()) return;
		
		for (OntologyClass parent : oc.parents) { parent.children.remove(oc); }
		for (OntologyClass child : oc.children) { child.parents.remove(oc); }
		
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
		memento.primaryTarget = oc.name;
		recordAction(memento);
	}
	
	@Override
	@Deprecated
	public void addParent(String className, String parentName) {
		OntologyClass child = getClassFromName(className);
		OntologyClass parent = getClassFromName(parentName);
		executeAddParent(child, parent);
	}
	
	@Override
	public void addParent(int childId, int parentId) {
		OntologyClass child = getClassFromIdentity(childId);
		OntologyClass parent = getClassFromIdentity(parentId);
		executeAddParent(child, parent);
	}
	
	private void executeAddParent(OntologyClass child, OntologyClass parent) {
		if (child != null && parent != null) {
			child.addParent(parent);
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.AddParent;
			memento.primaryTarget = child.name;
			memento.secondaryTarget = parent.name;
			recordAction(memento);
		}
	}
	
	@Override
	@Deprecated
	public void removeParent(String className, String parentName) {
		OntologyClass child = getClassFromName(className);
		OntologyClass parent = getClassFromName(parentName);
		executeRemoveParent(child, parent);
	}
	
	@Override
	public void removeParent(int childId, int parentId) {
		OntologyClass child = getClassFromIdentity(childId);
		OntologyClass parent = getClassFromIdentity(parentId);
		executeRemoveParent(child, parent);
	}
	
	private void executeRemoveParent(OntologyClass child, OntologyClass parent) {
		if (child != null && parent != null) {
			child.removeParent(parent);
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.RemoveParent;
			memento.primaryTarget = child.name;
			memento.secondaryTarget = parent.name;
			recordAction(memento);
		}
	}
	
	@Override
	@Deprecated
	public void addElement(String className, FileInterface file) {
		OntologyClass targetClass = getClassFromName(className);
		executeAddElement(targetClass, file);
	}
	
	@Override
	public void addElement(int classIdentity, FileInterface file) {
		OntologyClass targetClass = getClassFromIdentity(classIdentity);
		executeAddElement(targetClass, file);
	}
	
	private void executeAddElement(OntologyClass targetClass, FileInterface file) {
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
			memento.primaryTarget = targetClass.name;
			memento.targetFile = file;
			recordAction(memento);
		}
	}
	
	@Override
	@Deprecated
	public void removeElement(String className, FileInterface file) {
		OntologyClass targetClass = getClassFromName(className);
		executeRemoveElement(targetClass, file);
	}
	
	@Override
	public void removeElement(int classIdentity, FileInterface file) {
		OntologyClass targetClass = getClassFromIdentity(classIdentity);
		executeRemoveElement(targetClass, file);
	}
	
	private void executeRemoveElement(OntologyClass targetClass, FileInterface file) {
		if (targetClass == null) return;
		
		int classId = getIdentityFromClass(targetClass);
		ArrayList<FileInterface> fileList = filesPerOntology.get(classId);
		if (fileList.remove(file)) {
			if(file.tagsByIdentity != null) {
				file.tagsByIdentity.remove(Integer.valueOf(classId));
			}
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.RemoveElement;
			memento.primaryTarget = targetClass.name;
			memento.targetFile = file;
			recordAction(memento);
		}
	}
	
	@Override
	@Deprecated
	public void renameOntologyClass(String className, String newName) {
		OntologyClass targetClass = getClassFromName(className);
		executeRename(targetClass, newName);
	}
	
	@Override
	public void renameOntologyClass(int classIdentity, String newName) {
		OntologyClass targetClass = getClassFromIdentity(classIdentity);
		executeRename(targetClass, newName);
	}
	
	private void executeRename(OntologyClass targetClass, String newName) {
		if (targetClass != null && targetClass != getRootOntologyClass()) {
			String oldName = targetClass.name;
			targetClass.name = newName;
			
			OntologyMemento memento = new OntologyMemento();
			memento.actionType = OntologyMemento.ActionType.Rename;
			memento.primaryTarget = oldName;
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