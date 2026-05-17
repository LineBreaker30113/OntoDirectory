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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

// --- Thread Safety Perimeter ---
private final ReentrantReadWriteLock ontoLock = new ReentrantReadWriteLock();
private final AtomicBoolean dirtyFlag = new AtomicBoolean(false);

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

// Internal raw manager. We NEVER leak this directly to the ports anymore.
private final OntologyReadingService.OntologyManagingService rawHierarchyManager;
// Thread-safe facades
private final ThreadSafeReadingService safeReadingService;
private final ThreadSafeManagingService safeManagingService;

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
	this.rawHierarchyManager = ontologyHierarchy.new OntologyHierarchyManager();
	this.safeReadingService = new ThreadSafeReadingService(rawHierarchyManager);
	this.safeManagingService = new ThreadSafeManagingService(rawHierarchyManager);
}

public boolean isDirty() {
	return dirtyFlag.get();
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
	dirtyFlag.set(true);
	saveChanges();
}

@Override
public void executeVacuum() {
	ontoLock.writeLock().lock();
	try {
		if (isCorrupted) {
			System.err.println("Vacuum aborted. Data Lake is flagged as corrupted/locked.");
			return;
		}
		ontologyHierarchy.vacuumDatabase(managerPath);
		dirtyFlag.set(true);
		logActivity("Executed Database Vacuum (Tombstone Defragmentation)");
		notifyListeners();
	} finally {
		ontoLock.writeLock().unlock();
	}
}

@Override
public Path generateDiagnosticDump(Throwable ex) {
	ontoLock.readLock().lock();
	try {
		return CrashReporter.generateCrashDump(ex, this);
	} finally {
		ontoLock.readLock().unlock();
	}
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
	ontoLock.writeLock().lock();
	try {
		if (isCorrupted) throw new OntoDirectoryException("Cannot import to a corrupted or locked Data Lake.");
		
		if (!Files.exists(sourceDirectory)) {
			if (sourceDirectory.equals(getLakeImports())) {
				Files.createDirectories(sourceDirectory);
			}
			return;
		}
		
		if (Files.isRegularFile(sourceDirectory)) {
			processSingleImport(sourceDirectory);
			dirtyFlag.set(true);
			notifyListeners();
			return;
		}
		
		List<Path> unindexedFiles = Files.walk(sourceDirectory, 1)
			  .filter(Files::isRegularFile)
			  .toList();
		
		if (unindexedFiles.isEmpty()) return;
		
		String temporalClassName = "imported_at_" + simpleDateFormat.format(new Date());
		
		OntologyReadingService.OntologyManagingService oms = rawHierarchyManager;
		int temporalTagId = oms.createOntologyClass(temporalClassName, (List<Integer>) null, null);
		OntologyClass temporalTagClass = oms.getClassFromIdentity(temporalTagId);
		
		for (Path physicalFile : unindexedFiles) {
			processSingleImportInternal(physicalFile, temporalTagClass, oms);
		}
		
		dirtyFlag.set(true);
		notifyListeners();
		
	} catch (IOException e) {
		throw new OntoDirectoryException("Fatal Error during Ingestion: " + e.getMessage());
	} finally {
		ontoLock.writeLock().unlock();
	}
}

private void processSingleImport(Path physicalFile) {
	long timestamp = System.currentTimeMillis() / 1000L;
	String temporalClassName = "imported_at_" + timestamp;
	
	OntologyReadingService.OntologyManagingService oms = rawHierarchyManager;
	int temporalTagId = oms.createOntologyClass(temporalClassName, (List<Integer>) null, null);
	OntologyClass temporalTagClass = oms.getClassFromIdentity(temporalTagId);
	
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
	ontoLock.readLock().lock();
	try {
		if (!Files.exists(destinationFolder)) {
			Files.createDirectories(destinationFolder);
		}
		
		List<FileInterface> matchedFiles = rawHierarchyManager.getOntologyElements(filter);
		
		for (FileInterface file : matchedFiles) {
			Path targetPath = destinationFolder.resolve(file.actualName);
			Files.copy(file.actualFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
		
		logActivity("Exported " + matchedFiles.size() + " files to " + destinationFolder.getFileName());
		
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to export files to " + destinationFolder.getFileName() + ": " + e.getMessage());
	} finally {
		ontoLock.readLock().unlock();
	}
}

@Override
public void saveChanges() {
	ontoLock.writeLock().lock();
	try {
		if (isCorrupted) {
			System.err.println("Save aborted. Data Lake is flagged as corrupted/locked.");
			return;
		}
		if (Files.exists(getLakeHierarchy())) {
			Files.copy(getLakeHierarchy(), managerPath.resolve(hierarchyPathSuffix + ".bak"), StandardCopyOption.REPLACE_EXISTING);
		}
		if (Files.exists(getLakeElements())) {
			Files.copy(getLakeElements(), managerPath.resolve(elementsPathSuffix + ".bak"), StandardCopyOption.REPLACE_EXISTING);
		}
		
		storageService.saveOntologyHierarchy(getLakeHierarchy(), ontologyHierarchy);
		storageService.saveOntologyElements(getLakeElements(), ontologyHierarchy.fileInterfaces);
		dirtyFlag.set(false); // Clean the dirty flag ONLY after a successful flush
		System.out.println("Data Lake State Synced to Disk.");
		
	} catch (IOException e) {
		throw new OntoDirectoryException("Failed to save Lake state: " + e.getMessage());
	} finally {
		ontoLock.writeLock().unlock();
	}
}

@Override
public OntologyReadingService getOntologyReadingService() {
	return safeReadingService;
}

@Override
public OntologyReadingService.OntologyManagingService getOntologyManagingService() {
	return safeManagingService;
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

// =========================================================================
// THE HEXAGONAL PROXIES (Strict Thread-Safe Enclosures)
// =========================================================================

private class ThreadSafeReadingService implements OntologyReadingService {
	protected final OntologyReadingService delegate;
	public ThreadSafeReadingService(OntologyReadingService delegate) { this.delegate = delegate; }
	
	@Override public OntologyClass getRootOntologyClass() { ontoLock.readLock().lock(); try { return delegate.getRootOntologyClass(); } finally { ontoLock.readLock().unlock(); } }
	@Override public OntologyClass getClassFromIdentity(int identity) { ontoLock.readLock().lock(); try { return delegate.getClassFromIdentity(identity); } finally { ontoLock.readLock().unlock(); } }
	@Override public int getDomainIdentity() { ontoLock.readLock().lock(); try { return delegate.getDomainIdentity(); } finally { ontoLock.readLock().unlock(); } }
	@Override public void setDomain(OntologyClass domain) { ontoLock.writeLock().lock(); try { delegate.setDomain(domain); } finally { ontoLock.writeLock().unlock(); } }
	@Override public ArrayList<FileInterface> getOntologyElements(int classIdentity) { ontoLock.readLock().lock(); try { return delegate.getOntologyElements(classIdentity); } finally { ontoLock.readLock().unlock(); } }
	@Override public ArrayList<FileInterface> getAllOntologyElements(int classIdentity) { ontoLock.readLock().lock(); try { return delegate.getAllOntologyElements(classIdentity); } finally { ontoLock.readLock().unlock(); } }
	@Override public boolean isElementForFilter(OntologyClass oc, FileInterface fi) { ontoLock.readLock().lock(); try { return delegate.isElementForFilter(oc, fi); } finally { ontoLock.readLock().unlock(); } }
	@Override public boolean isDescendentForFilter(OntologyClass oc, FileInterface fi) { ontoLock.readLock().lock(); try { return delegate.isDescendentForFilter(oc, fi); } finally { ontoLock.readLock().unlock(); } }
	@Override public List<FileInterface> getOntologyElements(@NotNull OntologyFilter filter) { ontoLock.readLock().lock(); try { return delegate.getOntologyElements(filter); } finally { ontoLock.readLock().unlock(); } }
	@Override public void addOntologyServiceListener(OntologyServiceListener listener) { ontoLock.writeLock().lock(); try { delegate.addOntologyServiceListener(listener); } finally { ontoLock.writeLock().unlock(); } }
}

private class ThreadSafeManagingService extends ThreadSafeReadingService implements OntologyReadingService.OntologyManagingService {
	private final OntologyReadingService.OntologyManagingService delegate;
	public ThreadSafeManagingService(OntologyReadingService.OntologyManagingService delegate) {
		super(delegate);
		this.delegate = delegate;
	}
	
	private void markDirty() { dirtyFlag.set(true); }
	
	@Override public int filterToClass(OntologyFilter f, String n) {
		ontoLock.writeLock().lock();
		try {
			int r = delegate.filterToClass(f, n);
			markDirty();
			logActivity("Filtered to new class: " + n);
			return r;
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void copyContentsTo(int s, int t, boolean p, boolean c, boolean f) {
		ontoLock.writeLock().lock();
		try {
			delegate.copyContentsTo(s, t, p, c, f);
			markDirty();
			logActivity("Copied contents from ID:" + s + " to ID:" + t);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public int createOntologyClass(String n, List<Integer> p, List<Integer> c) {
		ontoLock.writeLock().lock();
		try {
			int r = delegate.createOntologyClass(n, p, c);
			markDirty();
			logActivity("Created Ontology Class: " + n);
			return r;
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void removeOntologyClass(int i) {
		ontoLock.writeLock().lock();
		try {
			delegate.removeOntologyClass(i);
			markDirty();
			logActivity("Removed Ontology Class ID: " + i);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void restoreOntologyClass(int i, OntologyClass s, ArrayList<FileInterface> f) {
		ontoLock.writeLock().lock();
		try {
			delegate.restoreOntologyClass(i, s, f);
			markDirty();
			logActivity("Restored Ontology Class ID: " + i);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void addParent(int c, int p) {
		ontoLock.writeLock().lock();
		try {
			delegate.addParent(c, p);
			markDirty();
			logActivity("Added Parent ID: " + p + " to Child ID: " + c);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void removeParent(int c, int p) {
		ontoLock.writeLock().lock();
		try {
			delegate.removeParent(c, p);
			markDirty();
			logActivity("Removed Parent ID: " + p + " from Child ID: " + c);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void addElement(int i, FileInterface f) {
		ontoLock.writeLock().lock();
		try {
			delegate.addElement(i, f);
			markDirty();
			logActivity("Added File " + f.actualName + " to Class ID: " + i);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void removeElement(int i, FileInterface f) {
		ontoLock.writeLock().lock();
		try {
			delegate.removeElement(i, f);
			markDirty();
			logActivity("Removed File " + f.actualName + " from Class ID: " + i);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void renameOntologyClass(int i, String n) {
		ontoLock.writeLock().lock();
		try {
			delegate.renameOntologyClass(i, n);
			markDirty();
			logActivity("Renamed Class ID: " + i + " to " + n);
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void undo() {
		ontoLock.writeLock().lock();
		try {
			delegate.undo();
			markDirty();
			logActivity("Executed Undo");
		} finally { ontoLock.writeLock().unlock(); }
	}
	
	@Override public void redo() {
		ontoLock.writeLock().lock();
		try {
			delegate.redo();
			markDirty();
			logActivity("Executed Redo");
		} finally { ontoLock.writeLock().unlock(); }
	}
}
}