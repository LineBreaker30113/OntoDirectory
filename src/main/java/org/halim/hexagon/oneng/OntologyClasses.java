package org.halim.hexagon.oneng;

import java.util.ArrayList;

/**
 * A data driven class for containing Ontology Classes in a memory optimized way.
 * Aimed to remove the object overheads of the former {@link org.halim.dlake.OntologyClass},
 * and their <b>unnecessary</b> {@link org.halim.dlake.OntologyClass#parents}
 * {@link org.halim.dlake.OntologyClass#parents} array overheads.
 * */
public class OntologyClasses {

private int expectedChildCount = 5;

/**
 * A positive value means the identity (also the index) of the parent.
 * A negative value means multiple parents, and is index of the array list of parents.
 * Assumption: It is likely a class will have a single parent.
 **/
private ArrayList<Integer> parents;
/** For classes with multiple parents,
 * {@link OntologyClasses#classMultipleParents} element with the same index points to the owner of the parents. */
private ArrayList<ArrayList<Integer>> multipleParents;
/** Owner identity for multipleParents, points to the owner of
 * {@link OntologyClasses#multipleParents} element with the same index. */
private ArrayList<Integer> classMultipleParents;
/**
 * Children of the classes, five slots each.
 * A null value means empty slot.
 * A positive value means the identity (also the index) of the children.
 * A negative value means more than {@link OntologyClasses#expectedChildCount} children, and is index of the array list of children.
 * Assumption: It is likely each class will have less than {@link OntologyClasses#expectedChildCount} children.
 * */
private ArrayList<Integer> childrens;
/** For classes with multiple children,
 * {@link OntologyClasses#classManyChildrens} element with the same index points to the owner of the children. */
private ArrayList<ArrayList<Integer>> manyChildrens;
/** Owner identity for manyChildrens, points to the owner of
 * {@link OntologyClasses#manyChildrens} element with the same index. */
private ArrayList<Integer> classManyChildrens;

/** Instance identities array for quick traversal. */
private ArrayList<ArrayList<Integer>> elementIdentities;


/** Names of the classes, simple. */
private ArrayList<String> names;

public void newClass(int parent, String name) {
	parents.add(parent);
	names.add(name);
	childrens.ensureCapacity(childrens.size() + expectedChildCount);
}


}
