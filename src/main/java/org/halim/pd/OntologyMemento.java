package org.halim.pd;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;

import java.util.ArrayList;

public class OntologyMemento {

public enum ActionType {
	Create, Remove, AddParent, RemoveParent, AddElement, RemoveElement, Rename
}

public ActionType actionType;
public int primaryTargetId;
public int secondaryTargetId;
public String stringPayload; // Holds Old Name or New Name for renames
public FileInterface targetFile;
public OntologyClass classSnapshot; // For resurrecting exact instances
public ArrayList<FileInterface> fileListSnapshot;

// Assuming your manager implements both Managing and Reading services
public void undo(OntologyReadingService.OntologyManagingService oms, OntologyReadingService ors) {
	switch (actionType) {
		case Create:
			classSnapshot = ors.getClassFromIdentity(primaryTargetId);
			fileListSnapshot = new ArrayList<>(ors.getOntologyElements(primaryTargetId)); // Capture the list!
			oms.removeOntologyClass(primaryTargetId);
			actionType = ActionType.Remove;
			break;
		case Remove:
			// It was Removed. Undo = Restore exact memory footprint.
			oms.restoreOntologyClass(primaryTargetId, classSnapshot,  fileListSnapshot);
			actionType = ActionType.Create;
			break;
		case AddParent:
			oms.removeParent(primaryTargetId, secondaryTargetId);
			actionType = ActionType.RemoveParent;
			break;
		case RemoveParent:
			oms.addParent(primaryTargetId, secondaryTargetId);
			actionType = ActionType.AddParent;
			break;
		case AddElement:
			oms.removeElement(primaryTargetId, targetFile);
			actionType = ActionType.RemoveElement;
			break;
		case RemoveElement:
			oms.addElement(primaryTargetId, targetFile);
			actionType = ActionType.AddElement;
			break;
		case Rename:
			// We need to fetch the CURRENT name to save it for Redo, then apply the OLD name
			String currentName = ors.getClassFromIdentity(primaryTargetId).name;
			oms.renameOntologyClass(primaryTargetId, stringPayload);
			stringPayload = currentName; // Swap payload to reverse the direction
			break;
	}
}
}