package org.halim;

import org.halim.cli.CliController;
import org.halim.pd.CrashReporter;
import org.halim.sgui.ApplicationController;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class Main {

//private static @NotNull JPanel createComingSoonPanel(String title) {
//	JPanel panel = new JPanel(new GridBagLayout());
//	JLabel label = new JLabel(title + " - Coming Soon");
//	label.setFont(label.getFont().deriveFont(24f));
//	label.setForeground(Color.GRAY);
//	panel.add(label);
//	return panel;
//}
//
//private static void onLoad() {
//	SettingLogic.onLoad();
//	FlatDarkLaf.setup();
//
//	JFrame frame = new JFrame("Onto Directory");
//	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	frame.setSize(1200, 800);
//
//	// 1. Build the GUI container
//	FullGUI mainUI = new FullGUI();
//
//	// 2. Initialize the Controller and hand it the Workspace
//	// Notice we changed WorksapceController.onLoad() to init(...)
//	WorkspaceController.init(mainUI.getCenterPanel().workspace);
//
//	frame.add(mainUI);
//	frame.setLocationRelativeTo(null);
//	frame.setVisible(true);
//
//	// 3. Build your Data Lake (Your existing test code)
//	DataLakeManager lakeA = new DataLakeManager(Path.of("/home/..."));
//	// ... (add your test nodes) ...
//
//	// 4. Set the Active Lake
//	WorkspaceController.setActiveLake(lakeA.ontologyHierarchy);
//
//	// 5. Force the Tree View to open by default on startup so you can see it
//	WorkspaceController.toggleTreeView();
//}

//private static void onLoad() {
//	// 1. Initialize core settings and Look & Feel
//	SettingLogic.onLoad();
//	FlatDarkLaf.setup();
//
//	// 2. Initialize the Workspace Controller (Creates the UI Views)
//	WorksapceController.onLoad();
//
//	// 3. Setup the Main Window
//	JFrame frame = new JFrame("Onto Directory");
//	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	frame.setSize(1200, 800);
//
//	SwitcherControllerPanelLeft mainUI = new SwitcherControllerPanelLeft();
//
//	// 4. Inject the actual Tree View from the controller into the UI
//	mainUI.addPanel("Tree View", WorksapceController.getThreeView());
//	mainUI.addPanel("Venn View", createComingSoonPanel("Venn Diagram View"));
//	mainUI.addPanel("Ontology Graph", createComingSoonPanel("Directed Graph Editor"));
//	mainUI.addPanel("Notes", createComingSoonPanel("Notes & Attachments"));
//
//	frame.add(mainUI);
//	frame.setLocationRelativeTo(null);
//	frame.setVisible(true);
//
//	// 5. Build your test Data Lake
//	DataLakeManager lakeA = new DataLakeManager(Path.of("/home/abdulhalimesen/unsortedORtemporal/ontoDir/LakeA"));
//	OntologyHierarchy.OntologyHierarchyManager l1m = lakeA.ontologyHierarchy.manager;
//
//	l1m.createNewClass("Engineering Field")
//		  .createNewSubClass("Engineering Field", "AI")
//		  .createNewSubClass("Engineering Field", "Data")
//		  .createNewSubClass("Engineering Field", "Software")
//		  .createNewSubClass("Engineering Field", "Computer")
//		  .createNewSubClass("Engineering Field", "Electric")
//		  .createNewSubClass("Engineering Field", "Electronics")
//		  .createNewIntersectionClass(new String[] { "AI", "Data" })
//		  .addParent("Data", "Software")
//		  .createNewClass("Logic")
//		  .createNewSubClass("Logic", "Mathematics")
//		  .createNewSubClass("Logic", "Philosophy")
//		  .createNewUnionClass(new String[] { "Logic", "Engineering Field" })
//		  .createNewSubClass("AI", "Machine Learning")
//		  .createNewUnionClass(new String[] { "Electric", "Electronics" })
//		  .createNewIntersectionClass(new String[] { "Computer", "Electronics" });
//
//	// 6. Broadcast the Lake to the UI!
//	// This triggers onLakeSwitched, runs your BFS, and paints the JTree.
//	WorksapceController.setActiveLake(lakeA.ontologyHierarchy);
//
//}

//public static void main(String[] args) throws InterruptedException {
//	// Unleash the GUI
////	javax.swing.SwingUtilities.invokeLater(Main::onLoad);
//	SettingLogic.onLoad();
//	FlatDarkLaf.setup();
//	JFrame appWindow = new JFrame("onto Directory");
//	ApplicationControllerDeprecated appContr = new ApplicationControllerDeprecated();
//	while(appContr.view == null) { Thread.sleep(8); }
//	appContr.deployTo(appWindow);
//}
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