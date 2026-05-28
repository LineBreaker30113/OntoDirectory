package org.halim;

import org.halim.cli.CliController;
import org.halim.pd.CrashReporter;
import org.halim.sgui.ApplicationController;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class Main {

public static OntoDirectoryServer servicePort;

// Adapters (Only one will ever be instantiated per runtime)
public static ApplicationController guiAdapter;
public static CliController cliAdapter;

private static void onGuiLoad() {
	JFrame frame = new JFrame("Onto Directory");
	frame.setSize(1200, 800);
	guiAdapter.deployTo(frame);
}

public static void main(String @NotNull [] args) {
	// 1. ARM THE TRIPWIRE FIRST
	Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
		System.err.println("Uncaught Exception intercepted on thread: " + thread.getName());
		if (servicePort != null) {
			servicePort.reportFatalError(exception);
		} else {
			// PRE-BOOT FAILURE: servicePort isn't alive yet. Force a global dump manually.
			System.err.println("Pre-boot failure detected. Routing to global fallback.");
			java.nio.file.Path fallback = java.nio.file.Paths.get(System.getProperty("user.home"), ".config", "onto-directory");
			CrashReporter.generateGlobalDump(exception, fallback);
		}
		
	});
	
	// 2. NOW PROCEED WITH BOOT SEQUENCE
	SettingLogic.onLoad();
	servicePort = new OntoDirectoryServer();
	
	if (args.length == 0) {
		// MODE 1: GUI (Default)
		com.formdev.flatlaf.FlatDarkLaf.setup();
		guiAdapter = new ApplicationController(servicePort);
		CrashReporter.registerProvider(guiAdapter);
		SwingUtilities.invokeLater(Main::onGuiLoad);
		
	} else if (args.length == 1 && (args[0].equals("-i") || args[0].equals("--interactive"))) {
		// MODE 2: Interactive REPL Shell
		cliAdapter = new CliController(servicePort);
		cliAdapter.startInteractiveShell();
		
	} else {
		// MODE 3: One-Shot Script Execution
		cliAdapter = new CliController(servicePort);
		cliAdapter.executeOneShot(args);
	}
}


}