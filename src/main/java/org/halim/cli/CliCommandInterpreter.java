package org.halim.cli;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class CliCommandInterpreter {

public static final String COMMAND_HELP = "help";
public static final String COMMAND_VERSION = "version";
public static final String COMMAND_EXIT = "exit";
public static final String COMMAND_REPL = "repl";


public CliService servicePort;
public CliWriter cliWriter;

/** Assumes none of the elements are null*/
public void interpret(@NotNull List<String> args) {
	args.removeIf(arg -> arg.equals(";"));
	if(args.size() < 1) {
	
	}
	Iterator<String> iterator = args.iterator();
	while (iterator.hasNext()) {
		String arg = iterator.next();
		if(arg.equals(COMMAND_EXIT)) {
			servicePort.executeExit();
		} else if(arg.equals(COMMAND_VERSION)) {
			servicePort.executeVersion();
		} else if(arg.equals(COMMAND_REPL)) {
			servicePort.executeREPL();
		} else if(arg.equals(COMMAND_HELP)) {
			servicePort.executeHelp();
		}
	}
}


}
