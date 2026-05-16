package org.halim.hport;

import org.halim.dlake.FileInterface;
import java.util.ArrayList;

public class OntologyMemento {

public static class OntologyClassState {
	public String name;
	public int[] parents, children;
}

public enum ActionType {
	Create, Remove, AddParent, RemoveParent, AddElement, RemoveElement, Rename
}

public ActionType actionType;
//public OntologyClassState[] beforeAction, afterAction;

// Supplementary data required to perform exact inverse operations
public String primaryTarget;
public String secondaryTarget;
public FileInterface targetFile;

/** Undoes the action, then inverses itself for ability to use as "redo". */
public void undo(OntologyReadingService.OntologyManagingService oms) {
	switch (actionType) {
		case Create:
			oms.removeOntologyClass(primaryTarget);
			actionType = ActionType.Remove;
			break;
		case Remove:
			// Recreate requires knowing previous parents/children.
			// We pass empty arrays for MVP to satisfy the signature.
			oms.createOntologyClass(primaryTarget, new ArrayList<Integer>(), new ArrayList<>());
			actionType = ActionType.Create;
			break;
		case AddParent:
			oms.removeParent(primaryTarget, secondaryTarget);
			actionType = ActionType.RemoveParent;
			break;
		case RemoveParent:
			oms.addParent(primaryTarget, secondaryTarget);
			actionType = ActionType.AddParent;
			break;
		case AddElement:
			oms.removeElement(primaryTarget, targetFile);
			actionType = ActionType.RemoveElement;
			break;
		case RemoveElement:
			oms.addElement(primaryTarget, targetFile);
			actionType = ActionType.AddElement;
			break;
		case Rename:
			oms.renameOntologyClass(secondaryTarget, primaryTarget);
			// Swap primary and secondary to reverse the rename direction
			String temp = primaryTarget;
			primaryTarget = secondaryTarget;
			secondaryTarget = temp;
			// actionType remains Rename
			break;
	}
}
}