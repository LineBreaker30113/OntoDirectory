package org.halim.hport;

import org.halim.Utilities.Pair;
import org.halim.dlake.OntologyFilter;
import java.nio.file.Path;
import java.util.ArrayList;

public interface OntoDirectoryService {

void loadDataLake(String fullPath);
void deleteLake(DataLakeService dataLakeService);
ArrayList<Pair<String, Object>> getGUI_Settings();
void setGUI_Setting(String key, Object value);

DataLakeService getActiveDataLake();
void dispatchLakeChooseRequest(Path identity);

void addOntoDirectoryServiceListener(OntoDirectoryServiceListener listener);

void reportFatalError(Throwable ex);

interface OntoDirectoryServiceListener {
	void onGUI_Change();
	void onDataLakeLoad(DataLakeService dataLakeService);
	void showBugReport(Path reportFile);
}

interface DataLakeService {
	void saveChanges();
	OntologyReadingService getOntologyReadingService();
	OntologyReadingService.OntologyManagingService getOntologyManagingService();
	Path getRootPath();
	
	void executeVacuum();
	
	void addDataLakeServiceListener(DataLakeServiceListener listener);
	
	interface DataLakeServiceListener {
		void onChange();
		
	}
	
	void importFiles();
	void importFiles(Path sourceDirectory);
	void exportFiles(OntologyFilter filter);
	void exportFiles(OntologyFilter filter, Path destinationFolder);
	
	void logActivity(String action);
	Path generateDiagnosticDump(Throwable ex);
}
}