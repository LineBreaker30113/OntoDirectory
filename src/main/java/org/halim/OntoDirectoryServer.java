package org.halim;

import javafx.util.Pair;
import org.halim.dlake.DataLakeManager;
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

// Memory map to hold and instantly swap between instantiated Data Lakes
private final Map<Path, DataLakeManager> activeLakes = new HashMap<>();

// Output Port Listeners (Pub/Sub)
private final List<OntoDirectoryServiceListener> listeners = new ArrayList<>();

@Override
public void loadDataLake(String fullPath) {
	Path lakePath = Paths.get(fullPath);
	DataLakeManager lake = new DataLakeManager(lakePath);
	listeners.forEach(listener -> listener.onDataLakeLoad(lake));
}

/** This meant to completely remove the data lake from the disk. */
@Override
public void deleteLake(DataLakeService dataLakeService) {
	if (dataLakeService instanceof DataLakeManager) {
		DataLakeManager manager = (DataLakeManager) dataLakeService;
		activeLakes.remove(manager.managerPath);
		// MVP Note: This only removes it from active RAM. Physical disk deletion is separate.
		
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