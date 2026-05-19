package org.halim.cli;

import org.halim.pd.OntoDirectoryService;
import java.nio.file.Path;
import java.util.Scanner;

public class CliController {

private final OntoDirectoryService servicePort;
private boolean isRunning = true;

public CliController(OntoDirectoryService servicePort) {
	this.servicePort = servicePort;
}

/**
 * The REPL (Read-Eval-Print Loop) - Acts like Python's IDLE.
 */
public void startInteractiveShell() {
	System.out.println("==========================================");
	System.out.println(" Onto Directory - Interactive Headless Shell");
	System.out.println(" Type 'help' for commands, 'exit' to quit.");
	System.out.println("==========================================");
	
	Scanner scanner = new Scanner(System.in);
	
	while (isRunning) {
		System.out.print("\nonto-dir> ");
		if (!scanner.hasNextLine()) break;
		
		String input = scanner.nextLine().trim();
		if (input.isEmpty()) continue;
		
		String[] tokens = input.split("\\s+");
		String command = tokens[0].toLowerCase();
		
		try {
			executeCommand(command, tokens);
		} catch (Exception e) {
			System.err.println("Error executing command: " + e.getMessage());
		}
	}
	
	System.out.println("Shutting down Onto Directory daemon...");
	System.exit(0); // Guarantee thread teardown
}

/**
 * Executes a single command and terminates immediately.
 */
public void executeOneShot(String[] args) {
	try {
		// Reconstruct the command from args
		String command = args[0].toLowerCase();
		executeCommand(command, args);
	} catch (Exception e) {
		System.err.println("Fatal Error: " + e.getMessage());
		System.exit(1);
	}
	System.exit(0);
}

// --- Command Router ---

private void executeCommand(String command, String[] tokens) {
	switch (command) {
		case "help":
			System.out.println("Available commands:");
			System.out.println("  load <path>   - Loads a Data Lake into active memory.");
			System.out.println("  import        - Imports unindexed files into the active lake.");
			System.out.println("  status        - Shows currently active Data Lake.");
			System.out.println("  exit          - Safely saves and closes the application.");
			break;
		
		case "load":
			if (tokens.length < 2) throw new IllegalArgumentException("Missing path. Usage: load <path>");
			System.out.println("Loading Data Lake at: " + tokens[1]);
			servicePort.loadDataLake(tokens[1]);
			// In a CLI, loading implicitly means we want to interact with it now
			servicePort.dispatchLakeChooseRequest(Path.of(tokens[1]));
			break;
		
		case "import":
			OntoDirectoryService.DataLakeService lake = servicePort.getActiveDataLake();
			if (lake == null) throw new IllegalStateException("No active Data Lake. Use 'load <path>' first.");
			System.out.println("Commencing ingestion pipeline...");
			lake.importFiles();
			System.out.println("Ingestion complete. Changes saved to disk.");
			break;
		
		case "status":
			OntoDirectoryService.DataLakeService active = servicePort.getActiveDataLake();
			if (active == null) System.out.println("State: Idle. No active Data Lake.");
			else System.out.println("State: Active. Target: " + active.getRootPath());
			break;
		
		case "exit":
		case "quit":
			OntoDirectoryService.DataLakeService closingLake = servicePort.getActiveDataLake();
			if (closingLake != null) closingLake.saveChanges();
			isRunning = false;
			break;
		
		default:
			System.out.println("Unknown command: '" + command + "'. Type 'help' for syntax.");
	}
}
}