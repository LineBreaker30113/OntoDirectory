package org.halim.dlake;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The central graph manager for a specific Data Lake.
 * <p>
 * This class encapsulates the entire Directed Acyclic Graph (DAG) of {@link OntologyClass}
 * instances and maps them to their respective {@link FileInterface} contents.
 */
public class OntologyHierarchy {

/** * Internal tuple mapping a specific ontology class to the files that possess that tag.
 */
private static class OntologyElements {
	OntologyClass ontologyClass;
	ArrayList<FileInterface> files = new ArrayList<>(); // Initialized to prevent NullPointer
	
	public OntologyElements(OntologyClass classElement) {
		this.ontologyClass = classElement;
	}
	private OntologyElements() { this.ontologyClass = null; }
	
	public static OntologyElements @NotNull [] getEmptyArray(int count) {
		OntologyElements[] result = new OntologyElements[count];
		for(int i = 0; i < count; i++) { result[i] = new OntologyElements(); }
		return result;
	}
}

private DataLakeManager owner;
private Path lakePath;

/** The linear registry of all ontology classes, ordered by their integer identity. */
private ArrayList<OntologyClass> ontologyClasses = new ArrayList<>();
private OntologyClass getROOT_ONTOLOGY_CLASS() { return ontologyClasses.getFirst(); }

/** The registry mapping classes to their assigned files. */
private ArrayList<OntologyElements> ontologyContainers = new ArrayList<>();

/** To edit the Ontolog Hierarchy. */
public OntologyHierarchyManager manager = new OntologyHierarchyManager();
/** To read from Ontolog Hierarchy for the GUI. */
public OntologyHierarchyReader reader = new OntologyHierarchyReader();

/** * Constructs a pristine, empty Ontology Hierarchy for a new Data Lake.
 * * @param owner The managing instance for this lake.
 */
public OntologyHierarchy(DataLakeManager owner) {
	this.owner = owner;
	ontologyClasses.add(OntologyClass.makeROOT_ONTOLOGY_CLASS("File"));
}

/** * Constructs and hydrates an Ontology Hierarchy from persistent storage.
 * * @param owner The managing instance for this lake.
 * @param lakePath The root directory containing the binary representation files.
 * @throws IOException If file access fails or the binary stream is corrupted.
 */
public OntologyHierarchy(DataLakeManager owner, @NotNull Path lakePath) throws IOException {
	this(owner);
	this.lakePath = lakePath; // Ensured lakePath is stored
	
	Path oesp = lakePath.resolve("ontologyClasses.bin");
	Path ohsp = lakePath.resolve("ontologyHierarchy.bin");
	
	if (Files.exists(oesp)) {
		System.out.println("OESP exists!");
		try (DataInputStream dis = new DataInputStream(Files.newInputStream(oesp))) {
			int elementCount = dis.readInt();
			System.out.println("Element count:" + dis.readInt());
			ontologyClasses.addAll(List.of(OntologyClass.getEmptyArray(elementCount - 1))); // Adjusted count
			
			for (int i = 1; i < ontologyClasses.size(); i++) {
				int payloadSize = dis.readInt();
				byte[] payload = new byte[payloadSize];
				dis.readFully(payload);
				ontologyClasses.get(i).unPack(this, ByteBuffer.wrap(payload));
			}
		}
	}
	
	if (Files.exists(ohsp)) {
		System.out.println("OHSP exists!");
		try (DataInputStream dis = new DataInputStream(Files.newInputStream(ohsp))) {
			ontologyContainers.addAll(List.of(OntologyElements.getEmptyArray(ontologyClasses.size())));
			for(OntologyElements oe : ontologyContainers) {
				oe.ontologyClass = getClassFromIdentity(dis.readInt());
				int instances = dis.readInt();
				oe.files.ensureCapacity(instances);
				for(int i = 0; i < instances; i++) {
					oe.files.add(owner.getFileFromIdentity(dis.readInt()));
				}
			}
		}
	}
}

@Contract(pure = true)
private @Nullable OntologyElements getElementsFromClass(OntologyClass ontologyClass) {
	for(OntologyElements oe : ontologyContainers) { if(oe.ontologyClass == ontologyClass) { return oe; } }
	return null;
}
//@Contract(pure = true)
//private @Nullable OntologyElements getAllDescendentElementsFromClass(OntologyClass ontologyClass) {
//	assert getElementsFromClass(ontologyClass) != null;
//	Collection<FileInterface> initial = getElementsFromClass(ontologyClass).files;
//	for(OntologyElements oe : ontologyContainers) { if(oe.ontologyClass == ontologyClass) { return oe; } }
//	return null;
//}

public class OntologyHierarchyManager {
//	/** Means the descendant can be added to the hierarchy, but maybe not. And maybe redundant. */
//	public final OntologyClass ROOT_PURGATORY_CLASS;
//	public OntologyHierarchyManager() {
//		ROOT_PURGATORY_CLASS = OntologyClass.makeROOT_ONTOLOGY_CLASS("View");
//	}
	
	public OntologyHierarchyManager createNewClass(String name) {
		OntologyClass noc = new OntologyClass(name, getROOT_ONTOLOGY_CLASS());
		OntologyHierarchy.this.ontologyClasses.add(noc);
		OntologyHierarchy.this.ontologyContainers.add(new OntologyElements(noc));
		return this;
	}
	public OntologyHierarchyManager createNewSubClass(String parentN, String childName) {
		createNewSubClass(getClassFromName(parentN), childName);
		return this;
	}
	public OntologyHierarchyManager createNewSubClass(OntologyClass parentC, String name) {
		OntologyClass noc = new OntologyClass(name, parentC);
		OntologyHierarchy.this.ontologyClasses.add(noc);
		OntologyHierarchy.this.ontologyContainers.add(new OntologyElements(noc));
		return this;
	}
	public OntologyHierarchyManager createNewIntersectionClass(String[] parentNames) {
		String name = Arrays.stream(parentNames)
			  .collect(Collectors.joining("\" AND \"", "{\"", "\"}"));
		
		List<OntologyClass> parents = Arrays.stream(parentNames)
			  .map(this::getClassFromName)
			  .toList();
		
		return createNewIntersectionClass(parents, name);
	}
	public OntologyHierarchyManager createNewIntersectionClass(@NotNull List<OntologyClass> parentClasses, String name) {
//		OntologyClass noc = new OntologyClass(name, ROOT_PURGATORY_CLASS);
		OntologyClass noc = new OntologyClass(name, getROOT_ONTOLOGY_CLASS());
		parentClasses.forEach(noc::addParent);
		// TODO: Make a proper recursive intersection collector algorithm, it must pick every common heritage
		OntologyHierarchy.this.ontologyClasses.add(noc);
		OntologyHierarchy.this.ontologyContainers.add(new OntologyElements(noc));
//		createNewSubClass(getClassFromName(parentN), childName);
		return this;
	}
	public OntologyHierarchyManager createNewUnionClass(String[] childNames) {
		String name = Arrays.stream(childNames)
			  .collect(Collectors.joining("\" OR \"", "{\"", "\"}"));
		
		List<OntologyClass> parents = Arrays.stream(childNames)
			  .map(this::getClassFromName)
			  .toList();
		
		return createNewUnionClass(parents, name);
	}
	public OntologyHierarchyManager createNewUnionClass(@NotNull List<OntologyClass> childClasses, String name) {
		OntologyClass noc = new OntologyClass(name, getROOT_ONTOLOGY_CLASS());
		// Simple logic, If a fact holds for any, it also holds for the Union.
		childClasses.forEach(cc -> { cc.addParent(noc); cc.parents.forEach(noc::addParent); });
		OntologyHierarchy.this.ontologyClasses.add(noc);
		OntologyHierarchy.this.ontologyContainers.add(new OntologyElements(noc));
		return this;
	}
	
	public OntologyHierarchyManager removeClass(int identity) {
		OntologyClass oc = getClassFromIdentity(identity);
		ontologyClasses.remove(oc);
		ontologyContainers.remove(OntologyHierarchy.this.getElementsFromClass(oc));
		return this;
	}
	/** Returns a different array from the actual elements. */
	public FileInterface[] getElementsFromClass(OntologyClass ontologyClass) {
		return Objects.requireNonNull(OntologyHierarchy.this.getElementsFromClass(ontologyClass)).files.toArray(new FileInterface[0]);
	}
	public OntologyClass getClassFromName(String name) {
		for(OntologyClass oc : ontologyClasses) { if(oc.name.equals(name)) { return oc; } }
		return null;
	}
	public OntologyHierarchyManager addParent(String name, String parentName) {
		getClassFromName(name).addParent(getClassFromName(parentName));
		return this;
	}
	public OntologyHierarchyManager removeParent(String name, String parentName) {
		getClassFromName(name).removeParent(getClassFromName(parentName));
		return this;
	}
	/** Modifies the actual elements. */
	public void addFileToClass(FileInterface fi, OntologyClass temporalClass) {
		assert OntologyHierarchy.this.getElementsFromClass(temporalClass) != null;
		OntologyHierarchy.this.getElementsFromClass(temporalClass).files.add(fi);
	}
	
}
public class OntologyHierarchyReader {
	
	public final OntologyClass ROOT_TEMPORAL_CLASS;
	public OntologyHierarchyReader() {
		ROOT_TEMPORAL_CLASS = OntologyClass.makeROOT_ONTOLOGY_CLASS("View Since:" + new Date().getTime());
	}
	
	public OntologyClass createNewIntersectionClass(String[] parentNames) {
		String name = Arrays.stream(parentNames)
			  .collect(Collectors.joining("\" AND \"", "{\"", "\"}"));
		OntologyClass result = new OntologyClass(name, ROOT_TEMPORAL_CLASS);
		Arrays.stream(parentNames)
			  .map(this::getClassFromName)
			  .map(result.parents::add);
		return result;
	}
	public OntologyClass createNewUnionClass(String[] childrenNames) {
		String name = Arrays.stream(childrenNames)
			  .collect(Collectors.joining("\" OR \"", "{\"", "\"}"));
		OntologyClass result = new OntologyClass(name, ROOT_TEMPORAL_CLASS);
		Arrays.stream(childrenNames)
			  .map(this::getClassFromName)
			  .map(result.children::add);
		return result;
	}
	
	public Set<FileInterface> getElementsFromTemporalClass(@NotNull OntologyClass temporalClass) {
		Set<FileInterface> resultSet = new HashSet<>();
		
		// Is it an OR query? (Temporal with Children)
		if (!temporalClass.children.isEmpty()) {
			for (OntologyClass child : temporalClass.children) {
				assert OntologyHierarchy.this.getElementsFromClass(child) != null;
				resultSet.addAll(OntologyHierarchy.this.getElementsFromClass(child).files);
			}
			return resultSet;
		}
		
		// Is it an AND query? (Temporal with Parents)
		if (temporalClass.parents.get(0) != ROOT_TEMPORAL_CLASS) {
			boolean first = true;
			for (OntologyClass parent : temporalClass.parents) {
				if (parent == ROOT_TEMPORAL_CLASS) continue;
				
				assert OntologyHierarchy.this.getElementsFromClass(parent) != null;
				Set<FileInterface> parentFiles = new HashSet<>(OntologyHierarchy.this.getElementsFromClass(parent).files);
				if (first) {
					resultSet.addAll(parentFiles);
					first = false;
				} else {
					resultSet.retainAll(parentFiles); // retainAll performs the mathematical AND intersection
				}
			}
			return resultSet;
		}
		return resultSet;
	}
	
	private OntologyClass getClassFromName(String name) {
		for(OntologyClass oc : ontologyClasses) { if(oc.name.equals(name)) { return oc; } }
		return null;
	}
}


/** Resolves an OntologyClass object to its integer identity (index). */
public int getIdentityFromClass(OntologyClass ontologyClass) {
	return ontologyClasses.indexOf(ontologyClass);
}
/** Resolves an integer identity back to its OntologyClass object instance. */
public OntologyClass getClassFromIdentity(int identity) {
	return ontologyClasses.get(identity);
}

public void printSelf() {
	printSubThree(ontologyClasses.getFirst(), 0);
}
public void printSubThree(OntologyClass oc, int tabs) {
	for(int i = 0; i < tabs; i++) { System.out.print("  "); }
	System.out.print(oc.name);
	if(!oc.children.isEmpty()) { System.out.print(":"); }
	System.out.println();
	oc.children.forEach(noc -> printSubThree(noc, tabs+1));
}

/** Flushes the entire RAM DAG state to the binary storage files. */
public void saveToDisk(Path lakePath) throws IOException {
	Path oesp = lakePath.resolve("ontologyClasses.bin");
	Path ohsp = lakePath.resolve("ontologyHierarchy.bin");
	
	// 1. Save Classes (Skipping Root Node at index 0 as per unpack logic)
	try (java.io.DataOutputStream dos = new java.io.DataOutputStream(Files.newOutputStream(oesp))) {
		dos.writeInt(ontologyClasses.size());
		for (int i = 1; i < ontologyClasses.size(); i++) {
			ByteBuffer buf = ontologyClasses.get(i).serialize(this);
			dos.writeInt(buf.capacity()); // Payload size
			dos.write(buf.array());       // Payload bytes
		}
	}
	
	// 2. Save Containers (File Mappings)
	try (java.io.DataOutputStream dos = new java.io.DataOutputStream(Files.newOutputStream(ohsp))) {
		for (OntologyElements oe : ontologyContainers) {
			dos.writeInt(getIdentityFromClass(oe.ontologyClass));
			dos.writeInt(oe.files.size());
			for (FileInterface fi : oe.files) {
				dos.writeInt(fi.identity);
			}
		}
	}
}

// --- Add this inside your public class OntologyHierarchyManager ---

public void addFileToClass(FileInterface fi, OntologyClass oc) {
	OntologyElements oe = OntologyHierarchy.this.getElementsFromClass(oc);
	if (oe != null && !oe.files.contains(fi)) {
		oe.files.add(fi);
	}
}

}

//package org.halim.dlake;
//
//import org.jetbrains.annotations.Contract;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.DataInputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.*;
//	  import java.util.stream.Collectors;
//
//public class OntologyHierarchy {
//
//private static class OntologyElements {
//	OntologyClass ontologyClass;
//	ArrayList<FileInterface> files = new ArrayList<>();
//
//	public OntologyElements(OntologyClass classElement) {
//		this.ontologyClass = classElement;
//	}
//	private OntologyElements() { this.ontologyClass = null; }
//
//	public static OntologyElements @NotNull [] getEmptyArray(int count) {
//		OntologyElements[] result = new OntologyElements[count];
//		for(int i = 0; i < count; i++) { result[i] = new OntologyElements(); }
//		return result;
//	}
//}
//
//private DataLakeManager owner;
//private Path lakePath;
//
//private ArrayList<OntologyClass> ontologyClasses = new ArrayList<>();
//private @Nullable OntologyClass getROOT_ONTOLOGY_CLASS() { return ontologyClasses.isEmpty() ? null : ontologyClasses.get(0); }
//
//private ArrayList<OntologyElements> ontologyContainers = new ArrayList<>();
//
//public OntologyHierarchyManager manager = new OntologyHierarchyManager();
//public OntologyHierarchyReader reader = new OntologyHierarchyReader();
//
//public OntologyHierarchy(DataLakeManager owner) {
//	this.owner = owner;
//	OntologyClass root = OntologyClass.makeROOT_ONTOLOGY_CLASS("File");
//	ontologyClasses.add(root);
//	ontologyContainers.add(new OntologyElements(root)); // FIX: Ensure Root has a container
//}
//
//public OntologyHierarchy(DataLakeManager owner, @NotNull Path lakePath) throws IOException {
//	this.owner = owner;
//	this.lakePath = lakePath;
//	// DO NOT call this(owner) here. We must rebuild the list precisely from disk.
//
//	Path oesp = lakePath.resolve("ontologyClasses.bin");
//	Path ohsp = lakePath.resolve("ontologyHierarchy.bin");
//
//	if (Files.exists(oesp)) {
//		try (DataInputStream dis = new DataInputStream(Files.newInputStream(oesp))) {
//			int elementCount = dis.readInt();
//			if (elementCount > 0) {
//				// Pre-allocate the root and the rest of the array
//				ontologyClasses.add(OntologyClass.makeROOT_ONTOLOGY_CLASS("File")); // Index 0
//				if (elementCount > 1) {
//					ontologyClasses.addAll(List.of(OntologyClass.getEmptyArray(elementCount - 1)));
//				}
//
//				for (int i = 0; i < elementCount; i++) {
//					int payloadSize = dis.readInt();
//					byte[] payload = new byte[payloadSize];
//					dis.readFully(payload);
//					ontologyClasses.get(i).unPack(this, ByteBuffer.wrap(payload));
//				}
//			}
//		}
//	} else {
//		ontologyClasses.add(OntologyClass.makeROOT_ONTOLOGY_CLASS("File"));
//	}
//
//	if (Files.exists(ohsp)) {
//		try (DataInputStream dis = new DataInputStream(Files.newInputStream(ohsp))) {
//			ontologyContainers.clear();
//			ontologyContainers.addAll(List.of(OntologyElements.getEmptyArray(ontologyClasses.size())));
//
//			for(OntologyElements oe : ontologyContainers) {
//				oe.ontologyClass = getClassFromIdentity(dis.readInt());
//				int instances = dis.readInt();
//				oe.files.ensureCapacity(instances);
//				for(int i = 0; i < instances; i++) {
//					FileInterface f = owner.getFileFromIdentity(dis.readInt());
//					if (f != null) oe.files.add(f);
//				}
//			}
//		}
//	} else {
//		ontologyContainers.add(new OntologyElements(ontologyClasses.get(0)));
//	}
//}
//
//@Contract(pure = true)
//private @Nullable OntologyElements getElementsFromClass(OntologyClass ontologyClass) {
//	for(OntologyElements oe : ontologyContainers) { if(oe.ontologyClass == ontologyClass) { return oe; } }
//	return null;
//}
//
//public class OntologyHierarchyManager {
//	public OntologyHierarchyManager createNewClass(String name) {
//		OntologyClass noc = new OntologyClass(name, getROOT_ONTOLOGY_CLASS());
//		OntologyHierarchy.this.ontologyClasses.add(noc);
//		OntologyHierarchy.this.ontologyContainers.add(new OntologyElements(noc));
//		return this;
//	}
//
//	public OntologyHierarchyManager createNewSubClass(String parentN, String childName) {
//		OntologyClass parent = getClassFromName(parentN);
//		if (parent != null) createNewSubClass(parent, childName);
//		return this;
//	}
//
//	public OntologyHierarchyManager createNewSubClass(OntologyClass parentC, String name) {
//		OntologyClass noc = new OntologyClass(name, parentC);
//		OntologyHierarchy.this.ontologyClasses.add(noc);
//		OntologyHierarchy.this.ontologyContainers.add(new OntologyElements(noc));
//		return this;
//	}
//
//	public OntologyHierarchyManager removeClass(int identity) {
//		OntologyClass oc = getClassFromIdentity(identity);
//		if (oc == null || oc == getROOT_ONTOLOGY_CLASS()) return this;
//
//		for (OntologyClass parent : oc.parents) { parent.children.remove(oc); }
//		for (OntologyClass child : oc.children) { child.parents.remove(oc); }
//
//		ontologyClasses.remove(oc);
//		ontologyContainers.remove(OntologyHierarchy.this.getElementsFromClass(oc));
//		return this;
//	}
//
//	public OntologyHierarchyManager addParent(String name, String parentName) {
//		OntologyClass child = getClassFromName(name);
//		OntologyClass parent = getClassFromName(parentName);
//		if (child != null && parent != null) { child.addParent(parent); }
//		return this;
//	}
//
//	public OntologyHierarchyManager removeParent(String name, String parentName) {
//		OntologyClass child = getClassFromName(name);
//		OntologyClass parent = getClassFromName(parentName);
//		if (child != null && parent != null) { child.removeParent(parent); }
//		return this;
//	}
//
//	public OntologyClass getClassFromName(String name) {
//		for(OntologyClass oc : ontologyClasses) { if(oc.name.equals(name)) { return oc; } }
//		return null;
//	}
//
//	public void addFileToClass(FileInterface fi, OntologyClass oc) {
//		OntologyElements oe = OntologyHierarchy.this.getElementsFromClass(oc);
//		if (oe != null && !oe.files.contains(fi)) {
//			oe.files.add(fi);
//		}
//	}
//}
//
//public class OntologyHierarchyReader {
//	public final OntologyClass ROOT_TEMPORAL_CLASS;
//
//	public OntologyHierarchyReader() {
//		ROOT_TEMPORAL_CLASS = OntologyClass.makeROOT_ONTOLOGY_CLASS("View Since:" + new Date().getTime());
//	}
//
//	public Set<FileInterface> getAllFilesForClass(OntologyClass targetClass) {
//		Set<FileInterface> resultSet = new HashSet<>();
//		if (targetClass == null) return resultSet;
//
//		Queue<OntologyClass> queue = new LinkedList<>();
//		Set<OntologyClass> visited = new HashSet<>();
//
//		queue.add(targetClass);
//		while (!queue.isEmpty()) {
//			OntologyClass current = queue.poll();
//			if (!visited.contains(current)) {
//				visited.add(current);
//				OntologyElements oe = OntologyHierarchy.this.getElementsFromClass(current);
//				if (oe != null) { resultSet.addAll(oe.files); }
//				queue.addAll(current.children);
//			}
//		}
//		return resultSet;
//	}
//}
//
//public int getIdentityFromClass(OntologyClass ontologyClass) {
//	return ontologyClasses.indexOf(ontologyClass);
//}
//
//public OntologyClass getClassFromIdentity(int identity) {
//	if (identity >= 0 && identity < ontologyClasses.size()) return ontologyClasses.get(identity);
//	return null;
//}
//
//
//
//public void saveToDisk(Path lakePath) throws IOException {
//	Path oesp = lakePath.resolve("ontologyClasses.bin");
//	Path ohsp = lakePath.resolve("ontologyHierarchy.bin");
//
//	try (java.io.DataOutputStream dos = new java.io.DataOutputStream(Files.newOutputStream(oesp))) {
//		dos.writeInt(ontologyClasses.size());
//		// THE FIX: Loop must start at 0 to save the root node's children!
//		for (int i = 0; i < ontologyClasses.size(); i++) {
//			ByteBuffer buf = ontologyClasses.get(i).serialize(this);
//			dos.writeInt(buf.capacity());
//			dos.write(buf.array());
//		}
//	}
//
//	try (java.io.DataOutputStream dos = new java.io.DataOutputStream(Files.newOutputStream(ohsp))) {
//		for (OntologyElements oe : ontologyContainers) {
//			dos.writeInt(getIdentityFromClass(oe.ontologyClass));
//			dos.writeInt(oe.files.size());
//			for (FileInterface fi : oe.files) {
//				dos.writeInt(fi.identity);
//			}
//		}
//	}
//}
//
//}