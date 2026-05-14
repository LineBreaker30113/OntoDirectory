package org.halim;

import org.halim.dlake.OntologyClass;

public interface Listeners {

public interface LakeChangeListener {
	void onLakeSwitched(OntologyHierarchy newHierarchy);
}
public interface TagChangeListener {
	void onTagChange(OntologyClass ntag, OntologyHierarchy.OntologyHierarchyReader reader);
}

}
