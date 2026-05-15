package org.halim.hport;

import javafx.util.Pair;
import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyFilter;

import java.nio.file.Path;
import java.util.ArrayList;

public interface OntoDirectoryService {

void loadDataLake(String fullPath);
void deleteLake(DataLakeService dataLakeService);
ArrayList<Pair<String, Object>> getGUI_Settings();
void setGUI_Setting(String key, Object value);

public DataLakeService getActiveDataLake();
public void dispatchLakeChooseRequest(Path identity);

void addOntoDirectoryServiceListener(OntoDirectoryServiceListener ontoDirectoryServiceListener);

interface OntoDirectoryServiceListener {
	void onGUI_Change();
	void onDataLakeLoad(DataLakeService dataLakeService);
}

interface DataLakeService {
	
	void saveChanges();
	OntologyReadingService getOntologyReadingService();
	OntologyReadingService.OntologyManagingService getOntologyManagingService();
	Path getRootPath();
	
	void addDataLakeServiceListener(DataLakeServiceListener dataLakeServiceListener);
	
	interface DataLakeServiceListener {
		void onChange();
	}
	
	void importFiles();
	void importFiles(Path sourceDirectory);
	void exportFiles(OntologyFilter filter);
	void exportFiles(OntologyFilter filter, Path destinationFolder);
	
}

}
