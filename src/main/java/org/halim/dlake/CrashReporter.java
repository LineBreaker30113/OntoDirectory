package org.halim.dlake;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashReporter {

/**
 * Generates a deeply contextualized dump for a specific Data Lake.
 * @return The Path to the generated dump file, or null if it failed.
 */
public static Path generateCrashDump(Throwable ex, DataLakeManager activeLake) {
	if (activeLake == null) {
		System.err.println("CRITICAL: Crash occurred, but no Data Lake was active.");
		ex.printStackTrace();
		return null;
	}
	
	try {
		Path bugDir = activeLake.getBugReportsDir();
		if (!Files.exists(bugDir)) Files.createDirectories(bugDir);
		
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		Path dumpFile = bugDir.resolve("crash_dump_" + timestamp + ".txt");
		
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(dumpFile))) {
			writer.println("=========================================");
			writer.println("      ONTO DIRECTORY CRASH DUMP          ");
			writer.println("=========================================");
			writer.println("Timestamp    : " + new Date());
			
			Runtime rt = Runtime.getRuntime();
			writer.println("Total Memory : " + (rt.totalMemory() / 1024 / 1024) + " MB");
			writer.println("Free Memory  : " + (rt.freeMemory() / 1024 / 1024) + " MB");
			writer.println("Active Lake  : [REDACTED FOR PRIVACY]");
			writer.println("=========================================\n");
			
			writer.println("--- BREADCRUMB TRAIL (LAST 50 ACTIONS) ---");
			for (String bc : activeLake.getBreadcrumbs()) {
				writer.println(bc);
			}
			
			writer.println("\n" + activeLake.ontologyHierarchy.generatePrivacyMaskedDagDump());
			
			writer.println("\n--- EXCEPTION STACK TRACE ---");
			ex.printStackTrace(writer);
		}
		
		System.err.println("\nFATAL ERROR: A critical failure occurred.");
		System.err.println("A privacy-masked crash dump has been generated at: " + dumpFile.toAbsolutePath());
		return dumpFile;
		
	} catch (Exception e) {
		System.err.println("CRITICAL: CrashReporter failed to write the domain dump.");
		ex.printStackTrace();
		return null;
	}
}

/**
 * Generates a generic JVM dump when no Data Lake is active (e.g., GUI boot failures).
 * @return The Path to the generated dump file, or null if it failed.
 */
public static Path generateGlobalDump(Throwable ex, Path fallbackConfigDir) {
	try {
		Path bugDir = fallbackConfigDir.resolve("bugReports");
		if (!Files.exists(bugDir)) Files.createDirectories(bugDir);
		
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		Path dumpFile = bugDir.resolve("global_crash_dump_" + timestamp + ".txt");
		
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(dumpFile))) {
			writer.println("=========================================");
			writer.println("   ONTO DIRECTORY GLOBAL CRASH DUMP      ");
			writer.println("=========================================");
			writer.println("Timestamp    : " + new Date());
			writer.println("State        : NO ACTIVE DATA LAKE");
			
			Runtime rt = Runtime.getRuntime();
			writer.println("Total Memory : " + (rt.totalMemory() / 1024 / 1024) + " MB");
			writer.println("Free Memory  : " + (rt.freeMemory() / 1024 / 1024) + " MB");
			writer.println("=========================================\n");
			
			writer.println("--- EXCEPTION STACK TRACE ---");
			ex.printStackTrace(writer);
		}
		
		System.err.println("\nFATAL GLOBAL ERROR: A critical system failure occurred.");
		System.err.println("A global crash dump has been generated at: " + dumpFile.toAbsolutePath());
		return dumpFile;
		
	} catch (Exception e) {
		System.err.println("CRITICAL: CrashReporter failed to write the global dump.");
		ex.printStackTrace();
		return null;
	}
}
}