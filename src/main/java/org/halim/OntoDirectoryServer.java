package org.halim;

import org.halim.Utilities.Pair;
import org.halim.dlake.DataLakeManager;
import org.halim.dlake.OntologyStorageV0;
import org.halim.hport.OntoDirectoryService;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntoDirectoryServer implements OntoDirectoryService {

private final Map<Path, DataLakeManager> activeLakes = new HashMap<>();
private Path currentlyActiveLakePath = null;
private final List<OntoDirectoryServiceListener> listeners = new ArrayList<>();

@Override
public void reportFatalError(Throwable ex) {
	DataLakeService activeLake = getActiveDataLake();
	Path dumpFile;
	
	if (activeLake != null) {
		dumpFile = activeLake.generateDiagnosticDump(ex);
	} else {
		dumpFile = org.halim.dlake.CrashReporter.generateGlobalDump(ex, org.halim.SettingLogic.ACTIVE_CONFIG_DIR);
	}
	
	if (dumpFile != null) {
		for (OntoDirectoryServiceListener listener : listeners) {
			listener.showBugReport(dumpFile);
		}
	}
}

@Override
public void loadDataLake(String fullPath) {
	Path lakePath = Paths.get(fullPath);
	DataLakeManager lake = activeLakes.computeIfAbsent(lakePath, p -> new DataLakeManager(p, new OntologyStorageV0()));
	currentlyActiveLakePath = lakePath;
	
	SettingLogic.addDataLake(lakePath);
	
	// Broadcast to the GUI that a new lake is loaded and active
	listeners.forEach(listener -> listener.onDataLakeLoad(lake));
}

@Override
public DataLakeService getActiveDataLake() {
	if (currentlyActiveLakePath == null) return null;
	return activeLakes.get(currentlyActiveLakePath);
}

@Override
public void dispatchLakeChooseRequest(Path identity) {
	if (activeLakes.containsKey(identity)) {
		currentlyActiveLakePath = identity;
		listeners.forEach(listener -> listener.onDataLakeLoad(activeLakes.get(identity)));
	}
}

@Override
public void deleteLake(DataLakeService dataLakeService) {
	if (dataLakeService instanceof DataLakeManager manager) {
		activeLakes.remove(manager.managerPath);
		if (manager.managerPath.equals(currentlyActiveLakePath)) {
			currentlyActiveLakePath = null;
		}
		
		SwingUtilities.invokeLater(() -> {
			for (OntoDirectoryServiceListener listener : listeners) {
				listener.onGUI_Change();
			}
		});
	}
}

@Override
public ArrayList<Pair<String, Object>> getGUI_Settings() {
	ArrayList<Pair<String, Object>> settings = new ArrayList<>();
	settings.add(new Pair<>("DarkTheme", SettingLogic.isDarkTheme));
	settings.add(new Pair<>("NativeFileChooser", SettingLogic.isSystemFileChooser));
	return settings;
}

@Override
public void setGUI_Setting(@NotNull String key, Object value) {
	if (key.equals("DarkTheme") && value instanceof Boolean) {
		SettingLogic.setSystemTheme((Boolean) value);
	} else if (key.equals("NativeFileChooser") && value instanceof Boolean) {
		SettingLogic.setSystemFileChooser((Boolean) value);
	}
	listeners.forEach(OntoDirectoryServiceListener::onGUI_Change);
}

@Override
public void addOntoDirectoryServiceListener(@NotNull OntoDirectoryServiceListener listener) {
	if (!listeners.contains(listener)) {
		listeners.add(listener);
	}
}
}