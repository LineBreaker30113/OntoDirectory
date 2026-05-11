package org.halim.dlake;

import org.halim.hport.OntoDirectoryService;
import org.halim.hport.OntologyReadingService;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataLakeManager implements OntoDirectoryService.DataLakeService {

public Path managerPath;
public Path lakePath;
public OntologyHierarchy ontologyHierarchy;
public ArrayList<FileInterface> files = new ArrayList<>();

public DataLakeManager(@NotNull Path managerPath) {
	this.managerPath = managerPath;
	this.lakePath = managerPath.resolve(".DATA_LAKE");
	
	if (!Files.exists(lakePath)) {
		try {
			Files.createDirectories(lakePath);
			Files.createDirectories(managerPath.resolve("imports"));
			Files.createDirectories(managerPath.resolve("exports"));
		} catch (IOException e) {
			System.err.println("Failed to build directory structure: " + e.getMessage());
		}
		ontologyHierarchy = new OntologyHierarchy(this);
		saveStateToDisk();
	} else {
		// FIX: Hydrate File Identities FIRST so the Ontology Graph has instances to point to
		Path isp = lakePath.resolve("identities.bin");
		if (Files.exists(isp)) {
			try (RandomAccessFile raf = new RandomAccessFile(isp.toFile(), "r")) {
				int elementCount = (int) (raf.length() / FileInterface.RECORD_SIZE);
				// Pre-allocate empty instances
				for(int i=0; i<elementCount; i++) files.add(new FileInterface());
				
				byte[] recordBuffer = new byte[FileInterface.RECORD_SIZE];
				for (FileInterface file : files) {
					raf.readFully(recordBuffer);
					file.deserialize(this, ByteBuffer.wrap(recordBuffer));
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to hydrate File Identities", e);
			}
		}
		
		// FIX: Hydrate Hierarchy SECOND
		try {
			ontologyHierarchy = new OntologyHierarchy(this, lakePath);
		} catch (IOException e) {
			throw new RuntimeException("Failed to hydrate Ontology Hierarchy", e);
		}
	}
}

public void saveStateToDisk() {
	try {
		ontologyHierarchy.saveToDisk(lakePath);
		
		Path isp = lakePath.resolve("identities.bin");
		try (RandomAccessFile raf = new RandomAccessFile(isp.toFile(), "rw")) {
			raf.setLength((long) files.size() * FileInterface.RECORD_SIZE);
			raf.seek(0);
			
			ByteBuffer buffer = ByteBuffer.allocate(FileInterface.RECORD_SIZE);
			for (FileInterface file : files) {
				buffer.clear();
				file.serialize(buffer);
				raf.write(buffer.array());
			}
		}
		System.out.println("Data Lake State Synced to Disk.");
	} catch (IOException e) {
		System.err.println("Failed to save Lake state: " + e.getMessage());
	}
}

public OntologyClass scanImports() {
	Path importsPath = managerPath.resolve("imports");
	
	try {
		if (!Files.exists(importsPath)) {
			Files.createDirectories(importsPath);
			return null;
		}
		
		List<Path> unindexedFiles = Files.walk(importsPath, 1)
			  .filter(Files::isRegularFile)
			  .toList();
		
		if (unindexedFiles.isEmpty()) return null;
		
		long timestamp = System.currentTimeMillis() / 1000L;
		String temporalClassName = "imported_at_" + timestamp;
		ontologyHierarchy.manager.createNewClass(temporalClassName);
		OntologyClass temporalClass = ontologyHierarchy.manager.getClassFromName(temporalClassName);
		
		Random rnd = new Random();
		String nonHexChars = "ghijklmnopqrstuvwxyz";
		
		for (Path physicalFile : unindexedFiles) {
			int newId = files.size();
			String hexSuffix = Integer.toHexString(newId).toLowerCase();
			
			StringBuilder diskNameBuilder = new StringBuilder();
			int padLength = 8 - hexSuffix.length();
			for (int i = 0; i < padLength; i++) {
				diskNameBuilder.append(nonHexChars.charAt(rnd.nextInt(nonHexChars.length())));
			}
			diskNameBuilder.append(hexSuffix);
			String diskName = diskNameBuilder.toString();
			
			Path newLocation = lakePath.resolve(diskName);
			Files.move(physicalFile, newLocation);
			
			FileInterface fi = new FileInterface();
			fi.identity = newId;
			fi.diskName = diskName;
			
			String originalName = physicalFile.getFileName().toString();
			byte[] nameBytes = originalName.getBytes(StandardCharsets.UTF_8);
			if (nameBytes.length > 128) {
				fi.actualName = new String(nameBytes, 0, 128, StandardCharsets.UTF_8);
			} else {
				fi.actualName = originalName;
			}
			
			fi.actualFile = newLocation;
			files.add(fi);
			
			ontologyHierarchy.manager.addFileToClass(fi, temporalClass);
			ontologyHierarchy.manager.addFileToClass(fi, ontologyHierarchy.manager.getClassFromName("File"));
		}
		
		saveStateToDisk();
		return temporalClass;
		
	} catch (IOException e) {
		System.err.println("Fatal Error during Ingestion: " + e.getMessage());
		return null;
	}
}

public FileInterface getFileFromIdentity(int identity) {
	for(FileInterface fi : files) {
		if(fi.identity == identity) { return fi; }
	}
	return null;
}

@Override
public void saveChanges() {

}

@Override
public OntologyReadingService getOntologyReadingService() {
	return null;
}

@Override
public OntologyReadingService.OntologyManagingService getOntologyManagingService() {
	return null;
}

@Override
public Path getRootPath() {
	return null;
}

@Override
public void addDataLakeServiceListener(DataLakeServiceListener dataLakeServiceListener) {

}
}