package org.halim;

import org.halim.Utilities.Pair;
import org.halim.dlake.DataLakeManager;
import org.halim.dlake.OntologyStorageV0;
import org.halim.pd.CrashReporter;
import org.halim.pd.DiagnosticStateProvider;
import org.halim.pd.OntoDirectoryService;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntoDirectoryServer implements OntoDirectoryService, DiagnosticStateProvider {

private final Map<Path, DataLakeManager> activeLakes = new HashMap<>();
private Path currentlyActiveLakePath = null;
private final List<OntoDirectoryServiceListener> listeners = new ArrayList<>();

public OntoDirectoryServer() {
	CrashReporter.registerProvider(this); // Self-register upon boot
}

// =========================================================================
// RSE TELEMETRY: DIAGNOSTIC STATE PROVIDER
// =========================================================================

@Override
public String getLayerName() {
	return "SERVICE_PORT_BOUNDARY";
}

@Override
public String captureStateDump() {
	StringBuilder dump = new StringBuilder();
	dump.append("  Loaded Lakes Count: ").append(activeLakes.size()).append("\n");
	// Use hashCode() to prove a lake is targeted without leaking the user's OS username/paths into logs
	dump.append("  Active Lake Target: ").append(currentlyActiveLakePath != null ? "[REDACTED_PATH_HASH_" + currentlyActiveLakePath.hashCode() + "]" : "NULL").append("\n");
	dump.append("  Active Listeners  : ").append(listeners.size()).append("\n");
	return dump.toString();
}

// ... [Keep ALL your existing Override methods exactly as they are] ...

@Override
public void reportFatalError(Throwable ex) {
	DataLakeService activeLake = getActiveDataLake();
	Path dumpFile;
	
	if (activeLake != null) {
		dumpFile = activeLake.generateDiagnosticDump(ex);
	} else {
		dumpFile = CrashReporter.generateGlobalDump(ex, org.halim.SettingLogic.ACTIVE_CONFIG_DIR);
	}
	
	if (dumpFile != null) {
		for (OntoDirectoryServiceListener listener : listeners) {
			listener.showBugReport(dumpFile);
		}
	}
}

@Override
public DataLakeService getActiveDataLake() {
	if (currentlyActiveLakePath == null) return null;
	return activeLakes.get(currentlyActiveLakePath);
}

@Override
public void loadDataLake(@NotNull String fullPath) {
	CrashReporter.log("[SERVER] [REQ_RECEIVED] Load Data Lake: [REDACTED_PATH_" + fullPath.hashCode() + "]");
	Path lakePath = Paths.get(fullPath);
	DataLakeManager lake = activeLakes.computeIfAbsent(lakePath, p -> new DataLakeManager(p, new OntologyStorageV0()));
	currentlyActiveLakePath = lakePath;
	
	SettingLogic.addDataLake(lakePath);
	listeners.forEach(listener -> listener.onDataLakeLoad(lake));
	CrashReporter.log("[SERVER] [REQ_ACCEPTED] Lake Loaded and Broadcasted.");
}

@Override
public void dispatchLakeChooseRequest(@NotNull Path identity) {
	CrashReporter.log("[SERVER] [REQ_RECEIVED] Dispatch Lake Focus: [REDACTED_PATH_" + identity.hashCode() + "]");
	if (activeLakes.containsKey(identity)) {
		currentlyActiveLakePath = identity;
		listeners.forEach(listener -> listener.onDataLakeLoad(activeLakes.get(identity)));
		CrashReporter.log("[SERVER] [REQ_ACCEPTED] Lake Focus Switched.");
	} else {
		CrashReporter.log("[SERVER] [REQ_REJECTED] Lake Focus Failed (Not in memory).");
	}
}

@Override
public void deleteLake(DataLakeService dataLakeService) {
	CrashReporter.log("[SERVER] [REQ_RECEIVED] Close Lake Request.");
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
		CrashReporter.log("[SERVER] [REQ_ACCEPTED] Lake Unloaded and UI Sync Dispatched.");
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
	CrashReporter.log("[SERVER] [REQ_RECEIVED] Set GUI Setting: " + key);
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