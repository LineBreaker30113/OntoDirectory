package org.halim.dlake;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Represents a discrete file residing within the Data Lake's immutable vault.
 * Memory Layout (140 Bytes Total):
 * [0-3]   : 4-byte Integer Identity
 * [4-11]  : 8-byte ASCII Disk Name (Obfuscated prefix + Hex ID)
 * [12-139]: 128-byte UTF-8 Actual File Name (padded with null bytes)
 */
public class FileInterface {

public int identity;
public String diskName;
public String actualName;

public ArrayList<Integer> tagsByIdentity;

/** The resolved, absolute physical path to the file on disk. */
public Path actualFile;

@Contract(pure = true)
public static FileInterface @NotNull [] getEmptyArray(int elementCount) {
	FileInterface[] result = new FileInterface[elementCount];
	for (int i = 0; i < elementCount; i++) { result[i] = new FileInterface(); }
	return result;
}

/** Serializes the object into a strict 140-byte buffer. */
public void serialize(ByteBuffer buffer) {
	buffer.putInt(identity);
	
	// Safely write 8-byte disk name
	byte[] diskBytes = diskName.getBytes(StandardCharsets.US_ASCII);
	buffer.put(diskBytes, 0, Math.min(8, diskBytes.length));
	for (int i = diskBytes.length; i < 8; i++) buffer.put((byte) 0);
	
	// Safely write 128-byte name
	byte[] nameBytes = actualName.getBytes(StandardCharsets.UTF_8);
	buffer.put(nameBytes, 0, Math.min(128, nameBytes.length));
	for (int i = nameBytes.length; i < 128; i++) buffer.put((byte) 0);
}

/** Hydrates the object from a 140-byte buffer. */
public void deserialize(DataLakeManager dataLakeManager, ByteBuffer buffer) {
	this.identity = buffer.getInt();
	
	byte[] diskBytes = new byte[8];
	buffer.get(diskBytes);
	this.diskName = new String(diskBytes, StandardCharsets.US_ASCII).trim();
	
	byte[] nameBytes = new byte[128];
	buffer.get(nameBytes);
	this.actualName = new String(nameBytes, StandardCharsets.UTF_8).trim();
	
	// Reconstruct the physical file path dynamically
	this.actualFile = dataLakeManager.lakePath.resolve(diskName);
}

}


//package org.halim.dlake;
//
//import org.jetbrains.annotations.Contract;
//import org.jetbrains.annotations.NotNull;
//
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Path;
//import java.util.ArrayList;
//
///**
// * Represents a discrete file residing within the Data Lake's immutable vault.
// * <p>
// * This interface binds a physical file path to a unique integer identity,
// * allowing the file to be referenced efficiently within the {@link OntologyHierarchy}.
// */
//public class FileInterface {
//
//public static final int RECORD_SIZE = 140;
//
///** The resolved, absolute physical path to the file on disk. */
//public Path actualFile;
//
///** The unique identifier mapped to this file within the system. */
//public int identity;
//
//public ArrayList<Integer> tagsByIdentity;
//
///**
// * Generates a pre-allocated array of empty FileInterface instances.
// * Used for efficient memory allocation prior to bulk binary hydration.
// *
// * @param elementCount The number of instances to generate.
// * @return An array of uninitialized {@code FileInterface} objects.
// */
//@Contract(pure = true)
//public static FileInterface @NotNull [] getEmptyArray(int elementCount) {
//	FileInterface[] result = new FileInterface[elementCount];
//	for(int i = 0; i < elementCount; i++) { result[i] = new FileInterface(); }
//	return result;
//}
//
///**
// * Hydrates this object's state from a binary payload.
// *
// * @param dataLakeManager The parent hierarchy used to resolve relative paths.
// * @param data The binary slice containing the filename length, string, and identity.
// */
//public void read(DataLakeManager dataLakeManager, ByteBuffer data) {
//	char[] name = new char[data.getInt()];
//	// BUG FIX: get() instead of put() to read from the buffer
//	data.asCharBuffer().get(name);
//	actualFile = dataLakeManager.lakePath.resolve(new String(name));
//	identity = data.getInt();
//}
//}