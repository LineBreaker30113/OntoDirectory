package org.halim.cli;

import org.jetbrains.annotations.NotNull;

public interface CliService {


void executeExit();
void executeHelp();
void executeVersion();
void executeREPL();

void endOfStream();



}
