package org.halim.hport;

public class OntologyMemento {

public static class OntologyClassState {
String name;
int[] parents, children;
}

public enum ActionType {
	Create, Remove, AddParent, RemoveParent, AddElement, RemoveElement, Rename
}

ActionType actionType;
public OntologyClassState[] beforeAction, afterAction;

/** Undoes the action, then inverses itself for ability to use as "redo". */
public void undo(OntologyReadingService.OntologyManagingService oms) {

}

}
