package org.halim.hport;

public class OntoDirectoryException extends RuntimeException {
public OntoDirectoryException(String message) {
	super(message);
}

public static class NullGivenAsOntologyClassException extends RuntimeException {
	public NullGivenAsOntologyClassException(String message) {
		super(message);
	}
}
}
