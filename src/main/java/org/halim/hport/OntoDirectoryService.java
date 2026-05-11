package org.halim.hport;

import javafx.util.Pair;

import java.nio.file.Path;
import java.util.ArrayList;

public interface OntoDirectoryService {

void loadDataLake(String fullPath);
void deleteLake(DataLakeService dataLakeService);
ArrayList<Pair<String, Object>> getGUI_Settings();
void setGUI_Setting(String key, Object value);

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
	
}


}
