package org.halim.dlake;

import org.halim.OntoDirectoryException;
import org.halim.hport.OntoDirectoryService;
import org.halim.hport.OntologyReadingService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataLakeManager implements OntoDirectoryService.DataLakeService {

public static final String lakePathSuffix = ".DATA_LAKE",
	  configFileSuffix = ".odConfig.bin",
	  importsPathSuffix = "imports",
	  exportsPathSuffix = "exports",
	  hierarchyPathSuffix = ".ontologyClasses.bin",
	  elementsPathSuffix = ".identities.bin";

public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd-HH-mm-ss");

public final Path managerPath;

// The Domain Objects
public OntologyHierarchyFast ontologyHierarchy;
private final OntologyStorageService storageService;

private final List<DataLakeServiceListener> listeners = new ArrayList<>();

public Path getLakePath() { return this.managerPath.resolve(lakePathSuffix); }
public Path getLakeConfig() { return this.managerPath.resolve(configFileSuffix); }
public Path getLakeImports() { return this.managerPath.resolve(importsPathSuffix); }
public Path getLakeExports() { return this.managerPath.resolve(exportsPathSuffix); }
public Path getLakeHierarchy() { return this.managerPath.resolve(hierarchyPathSuffix); }
public Path getLakeElements() { return this.managerPath.resolve(elementsPathSuffix); }

private OntologyReadingService.OntologyManagingService ontologyHierarchyManager;

public DataLakeManager(@NotNull Path managerPath, @NotNull OntologyStorageService storageService) {
	this.managerPath = managerPath;
	this.storageService = storageService;
	
	if(!Files.exists(getLakeConfig())) { createLake();
	} else try {
		ontologyHierarchy = new OntologyHierarchyFast();
		storageService.loadOntologyHierarchyFromFile(getLakeHierarchy(), ontologyHierarchy);
		ontologyHierarchy.fileInterfaces = storageService.loadOntologyElementsFromFile(getLakeElements());
		ontologyHierarchy.onLoad();
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to hydrate Data Lake from disk: " + e.getMessage());
	}
	ontologyHierarchyManager = ontologyHierarchy.new OntologyHierarchyManager();
}

private void createLake() {
	try {
		Files.createDirectories(getLakePath());
		Files.createFile(getLakeConfig());
		Files.createDirectories(getLakeImports());
		Files.createDirectories(getLakeExports());
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to build directory structure: " + e.getMessage());
	}
	System.out.println("Created Data Lake directory: " + getLakePath());
	ontologyHierarchy = new OntologyHierarchyFast();
	saveChanges();
}

@Override
public void importFiles() { importFiles(getLakeImports()); }

@Override
public void importFiles(Path sourceDirectory) {
	try {
		if (!Files.exists(sourceDirectory)) {
			if (sourceDirectory.equals(getLakeImports())) {
				Files.createDirectories(sourceDirectory);
			}
			return;
		}
		
		// If a single file is passed instead of a directory, handle it securely
		if (Files.isRegularFile(sourceDirectory)) {
			processSingleImport(sourceDirectory);
			saveChanges();
			notifyListeners();
			return;
		}
		
		List<Path> unindexedFiles = Files.walk(sourceDirectory, 1)
			  .filter(Files::isRegularFile)
			  .toList();
		
		if (unindexedFiles.isEmpty()) return;
		
		String temporalClassName = "imported_at_" + simpleDateFormat.format(new Date());
		
		// Use the manager port to safely create the class
		OntologyReadingService.OntologyManagingService oms = getOntologyManagingService();
		oms.createOntologyClass(temporalClassName, (List<Integer>) null, null);
//		OntologyClass temporalClass = oms.getClassFromName(temporalClassName);
		OntologyClass rootClass = oms.getClassFromIdentity(0);
		
		for (Path physicalFile : unindexedFiles) {
			processSingleImportInternal(physicalFile, /*temporalClass,*/ rootClass, oms);
		}
		
		saveChanges();
		notifyListeners();
		
	} catch (IOException e) {
		throw new OntoDirectoryException("Fatal Error during Ingestion: " + e.getMessage());
	}
}

private void processSingleImport(Path physicalFile) {
	long timestamp = System.currentTimeMillis() / 1000L;
	String temporalClassName = "imported_at_" + timestamp;
	
	OntologyReadingService.OntologyManagingService oms = getOntologyManagingService();
	oms.createOntologyClass(temporalClassName, (List<Integer>) null, null);
	
	processSingleImportInternal(
		  physicalFile,
//		  oms.getClassFromName(temporalClassName),
		  oms.getClassFromIdentity(0),
		  oms
	);
}

private void processSingleImportInternal(Path physicalFile, /*OntologyClass temporalClass,*/ OntologyClass rootClass, OntologyReadingService.OntologyManagingService oms) {
	int newId = ontologyHierarchy.fileInterfaces.size();
	String diskName = FileInterface.getDiskNameFor(newId);
	
	FileInterface fi = new FileInterface();
	fi.identity = newId;
	fi.actualFile = physicalFile; // Temporary old path
	
	String originalName = physicalFile.getFileName().toString();
	byte[] nameBytes = originalName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
	if (nameBytes.length > 128) {
		fi.actualName = new String(nameBytes, 0, 128, java.nio.charset.StandardCharsets.UTF_8);
	} else {
		fi.actualName = originalName;
	}
	
	// Trigger robust rename and move
	fi.renameDisk(diskName, getLakePath());
	
	ontologyHierarchy.fileInterfaces.add(fi);
	
	// Wire it into the DAG using the manager port
//	oms.addElement(temporalClass.name, fi);
	oms.addElement(rootClass.name, fi);
}

@Override
public void exportFiles(OntologyFilter filter) {
	exportFiles(filter, getLakeExports());
}

@Override
public void exportFiles(OntologyFilter filter, Path destinationFolder) {
	try {
		if (!Files.exists(destinationFolder)) {
			Files.createDirectories(destinationFolder);
		}
		
		// Delegate the search entirely to the port
		List<FileInterface> matchedFiles = getOntologyReadingService().getOntologyElements(filter);
		
		for (FileInterface file : matchedFiles) {
			Path targetPath = destinationFolder.resolve(file.actualName);
			
			// Copy so the archive retains the original
			Files.copy(file.actualFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to export files to " + destinationFolder.getFileName() + ": " + e.getMessage());
	}
}

@Override
public void saveChanges() {
	try {
		storageService.saveOntologyHierarchy(getLakeHierarchy(), ontologyHierarchy);
		storageService.saveOntologyElements(getLakeElements(), ontologyHierarchy.fileInterfaces);
		System.out.println("Data Lake State Synced to Disk.");
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to save Lake state: " + e.getMessage());
	}
}

@Override
public OntologyReadingService getOntologyReadingService() {
	return ontologyHierarchy.new OntologyHierarchyReader();
}

@Override
public OntologyReadingService.OntologyManagingService getOntologyManagingService() {
	return ontologyHierarchyManager;
}

@Override
public Path getRootPath() {
	return managerPath;
}

@Override
public void addDataLakeServiceListener(DataLakeServiceListener listener) {
	if (!listeners.contains(listener)) listeners.add(listener);
}

private void notifyListeners() {
	for (DataLakeServiceListener listener : listeners) listener.onChange();
}
}