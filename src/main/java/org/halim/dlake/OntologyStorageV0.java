package org.halim.dlake;

import org.halim.hport.OntoDirectoryException;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


/** To isolate the storing process from the rest. TODO */
public class OntologyStorageV0 {

/************************************************
 * First few bytes on each file we will store since they may be broken or not.
 * Rule is the first byte is the "lock", it is telling if the file is on use.
 * Next 8 bytes are magic numbers for corruption check (rand-3.0.1.1.3-rand-rand).
 * After that a byte for the version, right now we accept it Zero for MVP.
 ***/
public byte[] firstFewMagicBytes = new byte[] { 0 /*lock*/, 0x08 /*DEL*/, 3, 0, 1, 1, 3, 0x7f /*BS*/, 0x16 /*SYN*/, 0 };
static public ByteBuffer unlockBB = ByteBuffer.allocate(1);

static {
	unlockBB.put((byte)0).flip();
}

public ByteBuffer readFile(Path file, int maxSize) throws IOException {
	if (!Files.exists(file)) throw new FileNotFoundException(file.toAbsolutePath().toString() + " Does not exist, and cannot be read!");
	if(Files.size(file) < maxSize) throw new OntoDirectoryException.StorageFileCorruptedError(file.toAbsolutePath().toString() + " is Too Huge!");
	byte[] allBytes = Files.readAllBytes(file); // Utility that handles the heavy lifting
	if (allBytes.length < 10) throw new OntoDirectoryException.StorageFileCorruptedError("File too small on: " + file.toAbsolutePath().toString());
	if (allBytes[0] != 0) throw new OntoDirectoryException.StorageFileLockedError("File is locked on: " + file.toAbsolutePath().toString());
	for (int i = 0; i < 8; i++) {
		if (allBytes[i + 1] != firstFewMagicBytes[i + 1]) {
			throw new OntoDirectoryException.StorageFileCorruptedError("Magic number mismatch on: " + file.toAbsolutePath().toString());
		}
	}
	return ByteBuffer.wrap(allBytes, 10, allBytes.length - 10);
}

public DataOutputStream writeFile(Path file) throws IOException {
	if (Files.exists(file) && Files.newInputStream(file).read() != 0) throw new OntoDirectoryException.
		  StorageFileLockedError(file.toAbsolutePath().toString());
	DataOutputStream dos = new DataOutputStream(Files.newOutputStream(file));
	dos.write(0x3f); // exact value doesn't matter
	dos.write(firstFewMagicBytes, 1, firstFewMagicBytes.length-1);
	return dos;
}
public void closeFile(Path file) throws IOException {
	FileChannel fc = FileChannel.open(file, StandardOpenOption.READ,  StandardOpenOption.WRITE);
	fc.position(0);
	fc.write(unlockBB);
	fc.close();
}


public OntologyHierarchyNew loadOntologyHierarchyFromFile(Path source) throws IOException {
	ByteBuffer ontoData = readFile(source, 82 << 16 /*less than 64KB (64K node with 10c name, five parents and children) expected*/);
	if (ontoData == null || ontoData.remaining() < 4) throw new IOException("File is empty or missing header");
	OntologyHierarchyNew hierarchy = new OntologyHierarchyNew();
	int elementCount = ontoData.getInt(); // Sanity check the element count
	if (elementCount < 0 || elementCount > 1_000_000) { // reasonable ceiling
		throw new OntoDirectoryException.StorageFileCorruptedError("Invalid element count: " + elementCount);
	}
	ArrayList<OntologyClass> classes = hierarchy.ontologyClasses;
	OntologyClass.getEmptyArrayList(classes,  elementCount-1);
	for (int i = 0; i < classes.size(); i++) {
		if (ontoData.remaining() < 4) throw new OntoDirectoryException.
			  StorageFileCorruptedError("Unexpected EOF");
		int payloadSize = ontoData.getInt();
		if (payloadSize > ontoData.remaining()) throw new OntoDirectoryException.
			  StorageFileCorruptedError("Payload size exceeds remaining data");
		int currentPos = ontoData.position();
		ByteBuffer payloadSlice = ontoData.slice().limit(payloadSize);
		unPackOC(hierarchy, payloadSlice, classes.get(i));
		ontoData.position(currentPos + payloadSize);
	}
	
	return hierarchy;
}
public ArrayList<> loadOntologyElementsFromFile(Path source) throws IOException {
	ByteBuffer ontoData = readFile(source, 82 << 16 /*less than 64KB (64K node with 10c name, five parents and children) expected*/);
	if (ontoData == null || ontoData.remaining() < 4) throw new IOException("File is empty or missing header");
	OntologyHierarchyNew hierarchy = new OntologyHierarchyNew();
	int elementCount = ontoData.getInt(); // Sanity check the element count
	if (elementCount < 0 || elementCount > 1_000_000) { // reasonable ceiling
		throw new OntoDirectoryException.StorageFileCorruptedError("Invalid element count: " + elementCount);
	}
	ArrayList<OntologyClass> classes = hierarchy.ontologyClasses;
	
	return hierarchy;
}

public void saveOntologyHierarchy(Path target, @NotNull OntologyHierarchyNew hierarchy) throws IOException {
	DataOutputStream dos = writeFile(target);
	ArrayList<OntologyClass> ontologyClasses = hierarchy.ontologyClasses;
	dos.writeInt(ontologyClasses.size());
	for (OntologyClass oc : ontologyClasses) {
		ByteBuffer buf = serializeOC(hierarchy, oc);
		dos.writeInt(buf.capacity());
		dos.write(buf.array());
	}
	dos.flush(); dos.close();
	closeFile(target);
}

public void saveOntologyElements(Path target, @NotNull OntologyHierarchyNew hierarchy) throws IOException {
	DataOutputStream dos = writeFile(target);
	ArrayList<OntologyClass> ontologyClasses = hierarchy.ontologyClasses;
//	for (OntologyHierarchy.OntologyElements oe : ontologyContainers) {
//		dos.writeInt(getIdentityFromClass(oe.ontologyClass));
//		dos.writeInt(oe.files.size());
//		for (FileInterface fi : oe.files) {
//			dos.writeInt(fi.identity);
//		}
//	}
	dos.flush(); dos.close();
	closeFile(target);
}


/** * Serializes this class's state into a binary format.
 * <p>
 * <b>Precondition:</b> The caller must ensure that this class exists within the provided
 * {@link OntologyHierarchy}. The self identity is assumed to be its index resolved by the {@code owner}.
 * * @param owner The map resolving object instances to integer identities.
 * @return A tightly packed {@link ByteBuffer} containing the serialized class data.
 */
public static @NotNull ByteBuffer serializeOC(@NotNull OntologyHierarchyNew hierarchy, @NotNull OntologyClass oc) {
	int size = 4 /*name length*/ + oc.name.length() * 2;
	size += 4 /*parent count*/ + oc.parents.size() * 4;
	size += 4 /*child count*/ + oc.children.size() * 4;
	
	ByteBuffer buffer = ByteBuffer.allocate(size);
	buffer.putInt(oc.name.length());
	buffer.asCharBuffer().put(oc.name.toCharArray());
	buffer.putInt(oc.parents.size());
	for(OntologyClass p : oc.parents) { buffer.putInt(hierarchy.getIdentityFromClass(p)); }
	buffer.putInt(oc.children.size());
	for(OntologyClass p : oc.children) { buffer.putInt(hierarchy.getIdentityFromClass(p)); }
	return buffer;
}

/**
 * Hydrates this object's state from a binary payload.
 * <p>
 * <b>Usage Protocol:</b>
 * <ol>
 * <li>Read the total number of tags from the file header.</li>
 * <li>Allocate the required instances using {@link org.halim.dlake.OntologyClass#getEmptyArray(int)}.</li>
 * <li>Invoke this method on each instance, passing its specific data slice.</li>
 * </ol>
 *
 * @param owner The map resolving integer identities back to {@code OntologyClass} instances.
 * @param data  The binary payload containing this class's serialized state.
 */
public static void unPackOC(@NotNull OntologyHierarchyNew owner, @NotNull ByteBuffer data, @NotNull OntologyClass oc) {
	char[] nameBytes = new char[data.getInt()];
	data.asCharBuffer().get(nameBytes);
	oc.name = new String(nameBytes);
	
	int count = data.getInt();
	oc.parents.ensureCapacity(count);
	for(int i = 0; i < count; i++) {
		oc.parents.add(owner.getClassFromIdentity(data.getInt()));
	}
	
	count = data.getInt();
	oc.children.ensureCapacity(count);
	for(int i = 0; i < count; i++) {
		oc.children.add(owner.getClassFromIdentity(data.getInt()));
	}
}

}
