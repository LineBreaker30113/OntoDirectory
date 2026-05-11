package org.halim.hport;

import javafx.util.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Future;

public interface OntoDirectoryService {

Future<DataLakeService> loadDataLake(String fullPath);
void deleteLake(DataLakeService dataLakeService);
ArrayList<Pair<String, Object>> getSettings();
void setSetting(String key, Object value);



public interface DataLakeService {
	
	void saveChanges();
	OntologyReadingService getOntologyReadingService();
	OntologyReadingService.OntologyManagingService getOntologyManagingService();
	Path getRootPath();
	
}


}
