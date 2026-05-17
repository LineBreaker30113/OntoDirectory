package org.halim.dlake;

import org.halim.OntoDirectoryException;
import org.halim.hport.OntoDirectoryService;
import org.halim.hport.OntologyReadingService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
	  elementsPathSuffix = ".identities.bin",
	  bugReportsSuffix = "bugReports",
	  breadcrumbsSuffix = ".breadcrumbs.bin";

public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd-HH-mm-ss");

public final Path managerPath;
public boolean isCorrupted = false;

public OntologyHierarchyFast ontologyHierarchy;
private final OntologyStorageService storageService;

private final List<DataLakeServiceListener> listeners = new ArrayList<>();
private final CircularFifoQueue<String> breadcrumbs = new CircularFifoQueue<>(50);

public Path getLakePath() { return this.managerPath.resolve(lakePathSuffix); }
public Path getLakeConfig() { return this.managerPath.resolve(configFileSuffix); }
public Path getLakeImports() { return this.managerPath.resolve(importsPathSuffix); }
public Path getLakeExports() { return this.managerPath.resolve(exportsPathSuffix); }
public Path getLakeHierarchy() { return this.managerPath.resolve(hierarchyPathSuffix); }
public Path getLakeElements() { return this.managerPath.resolve(elementsPathSuffix); }
public Path getBugReportsDir() { return this.managerPath.resolve(bugReportsSuffix); }
public Path getBreadcrumbsFile() { return getBugReportsDir().resolve(breadcrumbsSuffix); }

private OntologyReadingService.OntologyManagingService ontologyHierarchyManager;

public DataLakeManager(@NotNull Path managerPath, @NotNull OntologyStorageService storageService) {
	this.managerPath = managerPath;
	this.storageService = storageService;
	
	if (!Files.exists(getLakeConfig())) {
		createLake();
	} else {
		try {
			ontologyHierarchy = new OntologyHierarchyFast();
			storageService.loadOntologyHierarchyFromFile(getLakeHierarchy(), ontologyHierarchy);
			ontologyHierarchy.fileInterfaces = storageService.loadOntologyElementsFromFile(getLakeElements());
			ontologyHierarchy.onLoad();
		} catch (OntoDirectoryException.StorageFileLockedError | OntoDirectoryException.StorageFileCorruptedError e) {
			System.err.println("Graceful Degradation Triggered: " + e.getMessage());
			isCorrupted = true;
			ontologyHierarchy = new OntologyHierarchyFast();
		} catch (IOException e) {
			throw new OntoDirectoryException("Failed to hydrate Data Lake from disk: " + e.getMessage());
		}
	}
	
	initBreadcrumbs();
	ontologyHierarchyManager = ontologyHierarchy.new OntologyHierarchyManager();
}

private void createLake() {
	try {
		Files.createDirectories(getLakePath());
		Files.createFile(getLakeConfig());
		Files.createDirectories(getLakeImports());
		Files.createDirectories(getLakeExports());
		Files.createDirectories(getBugReportsDir());
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to build directory structure: " + e.getMessage());
	}
	System.out.println("Created Data Lake directory: " + getLakePath());
	ontologyHierarchy = new OntologyHierarchyFast();
	saveChanges();
}

@Override
public void executeVacuum() {
	if (isCorrupted) {
		System.err.println("Vacuum aborted. Data Lake is flagged as corrupted/locked.");
		return;
	}
	ontologyHierarchy.vacuumDatabase(managerPath);
	saveChanges();
	logActivity("Executed Database Vacuum (Tombstone Defragmentation)");
	notifyListeners();
}

@Override
public Path generateDiagnosticDump(Throwable ex) {
	return CrashReporter.generateCrashDump(ex, this);
}

private void initBreadcrumbs() {
	try {
		if (!Files.exists(getBugReportsDir())) Files.createDirectories(getBugReportsDir());
		if (Files.exists(getBreadcrumbsFile())) {
			List<String> lines = Files.readAllLines(getBreadcrumbsFile(), StandardCharsets.UTF_8);
			int start = Math.max(0, lines.size() - 50);
			for (int i = start; i < lines.size(); i++) {
				breadcrumbs.add(lines.get(i));
			}
			Files.write(getBreadcrumbsFile(), breadcrumbs, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		}
	} catch (IOException e) {
		System.err.println("Warning: Failed to initialize breadcrumb ledger.");
	}
}

@Override
public void logActivity(String action) {
	String entry = simpleDateFormat.format(new Date()) + " | " + action;
	breadcrumbs.add(entry);
	try {
		Files.writeString(getBreadcrumbsFile(), entry + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
	} catch (IOException ignored) {}
}

public Iterable<String> getBreadcrumbs() {
	return breadcrumbs;
}

@Override
public void importFiles() { importFiles(getLakeImports()); }

@Override
public void importFiles(Path sourceDirectory) {
	if (isCorrupted) throw new OntoDirectoryException("Cannot import to a corrupted or locked Data Lake.");
	
	try {
		if (!Files.exists(sourceDirectory)) {
			if (sourceDirectory.equals(getLakeImports())) {
				Files.createDirectories(sourceDirectory);
			}
			return;
		}
		
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
		
		OntologyReadingService.OntologyManagingService oms = getOntologyManagingService();
		int temporalTagId = oms.createOntologyClass(temporalClassName, (List<Integer>) null, null);
		OntologyClass temporalTagClass = oms.getClassFromIdentity(temporalTagId);
		
		for (Path physicalFile : unindexedFiles) {
			processSingleImportInternal(physicalFile, temporalTagClass, oms);
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
	int temporalTagId = oms.createOntologyClass(temporalClassName, (List<Integer>) null, null);
	OntologyClass temporalTagClass = oms.getClassFromIdentity(temporalTagId);
	
	// Removed the duplicate root assignment bug. Wires perfectly to the temporal tag.
	processSingleImportInternal(physicalFile, temporalTagClass, oms);
}

private void processSingleImportInternal(Path physicalFile, OntologyClass rootClass, OntologyReadingService.OntologyManagingService oms) {
	int newId = ontologyHierarchy.fileInterfaces.size();
	String diskName = FileInterface.getDiskNameFor(newId);
	
	FileInterface fi = new FileInterface();
	fi.identity = newId;
	fi.actualFile = physicalFile;
	
	String originalName = physicalFile.getFileName().toString();
	byte[] nameBytes = originalName.getBytes(StandardCharsets.UTF_8);
	if (nameBytes.length > 128) {
		fi.actualName = new String(nameBytes, 0, 128, StandardCharsets.UTF_8);
	} else {
		fi.actualName = originalName;
	}
	
	fi.renameDisk(diskName, getLakePath());
	ontologyHierarchy.fileInterfaces.add(fi);
	oms.addElement(rootClass.identityNumber, fi);
	
	logActivity("Imported File ID " + newId);
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
		
		List<FileInterface> matchedFiles = getOntologyReadingService().getOntologyElements(filter);
		
		for (FileInterface file : matchedFiles) {
			Path targetPath = destinationFolder.resolve(file.actualName);
			Files.copy(file.actualFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
		
		logActivity("Exported " + matchedFiles.size() + " files to " + destinationFolder.getFileName());
		
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to export files to " + destinationFolder.getFileName() + ": " + e.getMessage());
	}
}

@Override
public void saveChanges() {
	if (isCorrupted) {
		System.err.println("Save aborted. Data Lake is flagged as corrupted/locked.");
		return;
	}
	try {
		if (Files.exists(getLakeHierarchy())) {
			Files.copy(getLakeHierarchy(), managerPath.resolve(hierarchyPathSuffix + ".bak"), StandardCopyOption.REPLACE_EXISTING);
		}
		if (Files.exists(getLakeElements())) {
			Files.copy(getLakeElements(), managerPath.resolve(elementsPathSuffix + ".bak"), StandardCopyOption.REPLACE_EXISTING);
		}
		
		storageService.saveOntologyHierarchy(getLakeHierarchy(), ontologyHierarchy);
		storageService.saveOntologyElements(getLakeElements(), ontologyHierarchy.fileInterfaces);
		System.out.println("Data Lake State Synced to Disk.");
		logActivity("DAG State Persisted to Disk.");
		
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