package org.halim.dlake;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Represents a node within the Data Lake's ontology hierarchy.
 * <p>
 * This class maps the represents a single class within ontology hierarchy,
 * allowing for complex, multi-parent hierarchical structures (directed acyclic graphs).
 * <p><b>Class Invariants (System Assumptions):</b>
 * <ul>
 * <li><b>Security Delegation:</b> This class assumes external systems handle all security
 * and permission checks prior to invoking ontology mutations.</li>
 * <li><b>Identity Uniqueness:</b> Different object instances represent semantically different
 * classes, regardless of whether they share the same {@code name}.</li>
 * <li><b>Identity Generation:</b> Identities are assumed to be linear, sequential increments
 * generated at the time of persistent storage.</li>
 * </ul>
 * * @author Abdülhalim
 */
public class OntologyClass {

/** The human-readable identifier for this class. */
public String name;

/** The list of immediate super-categories containing this class. */
public ArrayList<OntologyClass> parents = new ArrayList<>();
// THINK: Later those may need to vary depending on the subclasses, thus consider making getter and setters.
/** The list of immediate sub-categories contained by this class. */
public ArrayList<OntologyClass> children = new ArrayList<>();

/**
 * Resets the parent hierarchy, anchoring this class directly to the root.
 */
public void clearParents() {
	OntologyClass root = findROOT();
	parents.clear();
	parents.add(root);
}

/**
 * Constructs a new ontology class linked to a specific parent.
 *
 * @param name   The descriptive name of the tag.
 * @param parent The initial parent class to link to.
 */
public OntologyClass(String name, OntologyClass parent) {
	this.name = name;
	this.addParent(parent);
}

/** Private constructor utilized internally for binary array hydration and ROOT class generation. */
private OntologyClass() { }

/** * The absolute origin point of the ontology graph.
 * By definition, it is its own ancestry.
 * But it is not its own child because that would cause a loop while scanning the children.
 */
@Contract("_ -> new")
public static @NotNull OntologyClass makeROOT_ONTOLOGY_CLASS(String name) {
	final String rcname = name;
	return new OntologyClass() {
		{
			this.name = rcname;
			this.parents.add(this);
		}
		@Override
		public boolean isAncestry(OntologyClass candidate) {
			return candidate == this;
		}
		@Override
		public OntologyClass addParent(OntologyClass candidate) {
			System.out.println("Ontologic Parent ("+candidate.name+") attempted to add Root: "+name);
			return this;
		}
	};
}

/** * Finds the ROOT of the ontology graph using a clever and simple algorithm.
 * Is not peak performant like checking it using a static variable but more versatile and performance would not matter.
 */
public @NotNull OntologyClass findROOT() {
	OntologyClass result = parents.getFirst();
	while(result.parents.getFirst() != result) { result = result.parents.getFirst(); }
	return result;
}

////////// Ontology Functions

/** * Determines if the current class is a subclass (descendant) of the provided candidate.
 * <p>
 * This operation performs a recursive Depth-First Search (DFS) upwards through the parent hierarchy.
 * * @param candidate The class to test against the ancestry chain.
 * @return {@code true} if this class is identical to or descends from the candidate; {@code false} otherwise.
 */
public boolean isAncestry(OntologyClass candidate) {
	if(this == candidate) { return true; }
	for(OntologyClass p : parents) {
		if(p.isAncestry(candidate)) { return true; }
	}
	return false;
}

/** * Determines if the current class is a superclass (ancestor) of the provided candidate.
 * <p>
 * This operation performs a recursive Depth-First Search (DFS) downwards through the children hierarchy.
 * * @param candidate The class to test against the heritage chain.
 * @return {@code true} if this class is identical to or an ancestor of the candidate; {@code false} otherwise.
 */
public boolean isHeritage(OntologyClass candidate) {
	if(this == candidate) { return true; }
	for(OntologyClass c : children) {
		if(c.isHeritage(candidate)) { return true; }
	}
	return false;
}

///////// Safe Functions

/** * Safely links a new parent to this class, restructuring the hierarchy to prevent cyclic dependencies.
 * <p>
 * The resolution logic follows strict rules based on the candidate's relationship to this class:
 * <ul>
 * <li>If the candidate is already a direct parent or an existing ancestor, no action is taken.</li>
 * <li>If the candidate is a descendant of a current parent, the tree is re-routed:
 * the old parent is replaced by the candidate, moving this class down the tree.</li>
 * <li>Otherwise, the candidate is introduced as an additional parent.</li>
 * </ul>
 * <p>
 * <b>Note:</b> Changes to indirect delegates are managed by the children structures recursively.
 *
 * @param candidate The proposed new parent class.
 * @return The current instance ({@code this}) to allow for method chaining.
 */
public OntologyClass addParent(OntologyClass candidate) {
	if(parents.contains(candidate)) { return this; }
	if(isAncestry(candidate)) { return this; }
	
	boolean candidateAdded = false;
	Iterator<OntologyClass> iterator = parents.iterator();
	
	while (iterator.hasNext()) {
		OntologyClass currentParent = iterator.next();
		if (candidate.isAncestry(currentParent)) {
			// Safely sever the old tie while iterating
			currentParent.children.remove(this);
			iterator.remove();
			
			// Link the candidate only once
			if (!candidateAdded) {
				candidate.children.add(this);
				candidateAdded = true;
			}
		}
	}
	
	// If it was a completely unrelated branch
	if (!candidateAdded) {
		parents.add(candidate);
		candidate.children.add(this);
	}
	return this;
}

/** * Safely unlinks a parent from this class.
 * <p>
 * If the specified parent is not a direct parent, no action is taken. If the removal
 * leaves this class orphaned (0 parents), it is automatically re-anchored to the
 * {@link #makeROOT_ONTOLOGY_CLASS}.
 * <p><b>Postcondition:</b> This operation is absolute. Removing a parent severs the edge permanently.
 * It does not restore or rollback to previously subsumed ancestors (historical graph states are not preserved).
 * * @param parent The direct parent class to remove.
 * @return The current instance ({@code this}) to allow for method chaining.
 */
public OntologyClass removeParent(OntologyClass parent) {
	int pi = parents.indexOf(parent);
	if(pi == -1) { return this; }
	
	parent.children.remove(this);
	if(parents.size() == 1) {
		clearParents();
		return this;
	}
	parents.remove(pi);
	return this;
}

/** * Serializes this class's state into a binary format.
 * <p>
 * <b>Precondition:</b> The caller must ensure that this class exists within the provided
 * {@link OntologyHierarchy}. The self identity is assumed to be its index resolved by the {@code owner}.
 * * @param owner The map resolving object instances to integer identities.
 * @return A tightly packed {@link ByteBuffer} containing the serialized class data.
 */
public ByteBuffer serialize(@NotNull OntologyHierarchy owner) {
	int size = 4 + name.length()*2;
	size += 4 + parents.size() * 4;
	size += 4 + children.size() * 4;
	
	ByteBuffer buffer = ByteBuffer.allocate(size + 4);
	
	buffer.putInt(owner.getIdentityFromClass(this));
	buffer.putInt(name.length());
	buffer.asCharBuffer().put(name.toCharArray());
	buffer.putInt(parents.size());
	for(OntologyClass p : parents) { buffer.putInt(owner.getIdentityFromClass(p)); }
	buffer.putInt(children.size());
	for(OntologyClass p : children) { buffer.putInt(owner.getIdentityFromClass(p)); }
	return buffer;
}

/**
 * Hydrates this object's state from a binary payload.
 * <p>
 * <b>Usage Protocol:</b>
 * <ol>
 * <li>Read the total number of tags from the file header.</li>
 * <li>Allocate the required instances using {@link #getEmptyArray(int)}.</li>
 * <li>Invoke this method on each instance, passing its specific data slice.</li>
 * </ol>
 *
 * @param owner The map resolving integer identities back to {@code OntologyClass} instances.
 * @param data  The binary payload containing this class's serialized state.
 */
public void unPack(OntologyHierarchy owner, ByteBuffer data) {
	char[] nameBytes = new char[data.getInt()];
	data.asCharBuffer().get(nameBytes);
	this.name = new String(nameBytes);
	
	int count = data.getInt();
	parents.ensureCapacity(count);
	for(int i = 0; i < count; i++) {
		parents.add(owner.getClassFromIdentity(data.getInt()));
	}
	
	count = data.getInt();
	children.ensureCapacity(count);
	for(int i = 0; i < count; i++) {
		children.add(owner.getClassFromIdentity(data.getInt()));
	}
}

/**
 * Generates a pre-allocated array of empty ontology classes.
 * This is primarily used as a precursor step to binary deserialization.
 *
 * @param count The number of empty instances to generate.
 * @return An array containing {@code count} uninitialized {@code OntologyClass} objects.
 */
public static OntologyClass[] getEmptyArray(int count) {
	OntologyClass[] result = new OntologyClass[count];
	for(int i = 0; i < count; i++) { result[i] = new OntologyClass(); }
	return result;
}

public static void main(String[] args) {
	// Main execution entry point for isolated testing
}

}

//package org.halim.dlake;
//
//import org.jetbrains.annotations.Contract;
//import org.jetbrains.annotations.NotNull;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.Iterator;
//
///**
// * Represents a node within the Data Lake's ontology hierarchy.
// * <p>
// * This class maps the represents a single class within ontology hierarchy,
// * allowing for complex, multi-parent hierarchical structures (directed acyclic graphs).
// * <p><b>Class Invariants (System Assumptions):</b>
// * <ul>
// * <li><b>Security Delegation:</b> This class assumes external systems handle all security
// * and permission checks prior to invoking ontology mutations.</li>
// * <li><b>Identity Uniqueness:</b> Different object instances represent semantically different
// * classes, regardless of whether they share the same {@code name}.</li>
// * <li><b>Identity Generation:</b> Identities are assumed to be linear, sequential increments
// * generated at the time of persistent storage.</li>
// * </ul>
// * * @author Abdülhalim
// */
//public class OntologyClass {
//
///** The human-readable identifier for this class. */
//public String name;
//
///** The list of immediate super-categories containing this class. */
//public ArrayList<OntologyClass> parents = new ArrayList<>();
//// THINK: Later those may need to vary depending on the subclasses, thus consider making getter and setters.
///** The list of immediate sub-categories contained by this class. */
//public ArrayList<OntologyClass> children = new ArrayList<>();
//
///**
// * Resets the parent hierarchy, anchoring this class directly to the root.
// */
//public void clearParents() {
//	OntologyClass root = findROOT();
//	parents.clear();
//	parents.add(root);
//}
//
///**
// * Constructs a new ontology class linked to a specific parent.
// *
// * @param name   The descriptive name of the tag.
// * @param parent The initial parent class to link to.
// */
//public OntologyClass(String name, OntologyClass parent) {
//	this.name = name;
//	this.addParent(parent);
//}
//
///** Private constructor utilized internally for binary array hydration and ROOT class generation. */
//private OntologyClass() { }
//
///** * The absolute origin point of the ontology graph.
// * By definition, it is its own ancestry.
// * But it is not its own child because that would cause a loop while scanning the children.
// */
//@Contract("_ -> new")
//public static @NotNull OntologyClass makeROOT_ONTOLOGY_CLASS(String name) {
//	final String rcname = name;
//	return new OntologyClass() {
//		{
//			this.name = rcname;
//			this.parents.add(this);
//		}
//		@Override
//		public boolean isAncestry(OntologyClass candidate) {
//			return candidate == this;
//		}
//		@Override
//		public OntologyClass addParent(OntologyClass candidate) {
//			System.out.println("Ontologic Parent ("+candidate.name+") attempted to add Root: "+name);
//			return this;
//		}
//	};
//}
//
///** * Finds the ROOT of the ontology graph using a clever and simple algorithm.
// * Is not peak performant like checking it using a static variable but more versatile and performance would not matter.
// */
//public @NotNull OntologyClass findROOT() {
//	OntologyClass result = parents.getFirst();
//	while(result.parents.getFirst() != result) { result = result.parents.getFirst(); }
//	return result;
//}
//
//////////// Ontology Functions
//
///** * Determines if the current class is a subclass (descendant) of the provided candidate.
// * <p>
// * This operation performs a recursive Depth-First Search (DFS) upwards through the parent hierarchy.
// * * @param candidate The class to test against the ancestry chain.
// * @return {@code true} if this class is identical to or descends from the candidate; {@code false} otherwise.
// */
//public boolean isAncestry(OntologyClass candidate) {
//	if(this == candidate) { return true; }
//	for(OntologyClass p : parents) {
//		if(p.isAncestry(candidate)) { return true; }
//	}
//	return false;
//}
//
///** * Determines if the current class is a superclass (ancestor) of the provided candidate.
// * <p>
// * This operation performs a recursive Depth-First Search (DFS) downwards through the children hierarchy.
// * * @param candidate The class to test against the heritage chain.
// * @return {@code true} if this class is identical to or an ancestor of the candidate; {@code false} otherwise.
// */
//public boolean isHeritage(OntologyClass candidate) {
//	if(this == candidate) { return true; }
//	for(OntologyClass c : children) {
//		if(c.isHeritage(candidate)) { return true; }
//	}
//	return false;
//}
//
/////////// Safe Functions
//
///** * Safely links a new parent to this class, restructuring the hierarchy to prevent cyclic dependencies.
// * <p>
// * The resolution logic follows strict rules based on the candidate's relationship to this class:
// * <ul>
// * <li>If the candidate is already a direct parent or an existing ancestor, no action is taken.</li>
// * <li>If the candidate is a descendant of a current parent, the tree is re-routed:
// * the old parent is replaced by the candidate, moving this class down the tree.</li>
// * <li>Otherwise, the candidate is introduced as an additional parent.</li>
// * </ul>
// * <p>
// * <b>Note:</b> Changes to indirect delegates are managed by the children structures recursively.
// *
// * @param candidate The proposed new parent class.
// * @return The current instance ({@code this}) to allow for method chaining.
// */
//public OntologyClass addParent(OntologyClass candidate) {
//	if(parents.contains(candidate)) { return this; }
//	if(isAncestry(candidate)) { return this; }
//
//	boolean candidateAdded = false;
//	Iterator<OntologyClass> iterator = parents.iterator();
//
//	while (iterator.hasNext()) {
//		OntologyClass currentParent = iterator.next();
//		if (candidate.isAncestry(currentParent)) {
//			// Safely sever the old tie while iterating
//			currentParent.children.remove(this);
//			iterator.remove();
//
//			// Link the candidate only once
//			if (!candidateAdded) {
//				candidate.children.add(this);
//				candidateAdded = true;
//			}
//		}
//	}
//
//	// If it was a completely unrelated branch
//	if (!candidateAdded) {
//		parents.add(candidate);
//		candidate.children.add(this);
//	}
//	return this;
//}
//
///** * Safely unlinks a parent from this class.
// * <p>
// * If the specified parent is not a direct parent, no action is taken. If the removal
// * leaves this class orphaned (0 parents), it is automatically re-anchored to the
// * {@link #makeROOT_ONTOLOGY_CLASS}.
// * <p><b>Postcondition:</b> This operation is absolute. Removing a parent severs the edge permanently.
// * It does not restore or rollback to previously subsumed ancestors (historical graph states are not preserved).
// * * @param parent The direct parent class to remove.
// * @return The current instance ({@code this}) to allow for method chaining.
// */
//public OntologyClass removeParent(OntologyClass parent) {
//	int pi = parents.indexOf(parent);
//	if(pi == -1) { return this; }
//
//	parent.children.remove(this);
//	if(parents.size() == 1) {
//		clearParents();
//		return this;
//	}
//	parents.remove(pi);
//	return this;
//}
//
///** * Serializes this class's state into a binary format.
// * <p>
// * <b>Precondition:</b> The caller must ensure that this class exists within the provided
// * {@link OntologyHierarchy}. The self identity is assumed to be its index resolved by the {@code owner}.
// * * @param owner The map resolving object instances to integer identities.
// * @return A tightly packed {@link ByteBuffer} containing the serialized class data.
// */
///** Serializes this class's state into a binary format. */
//public ByteBuffer serialize(@NotNull OntologyHierarchy owner) {
//	int size = 4; // Identity
//	size += 4 + name.length() * 2; // Name length + Chars
//	size += 4 + parents.size() * 4; // Parents count + Ints
//	size += 4 + children.size() * 4; // Children count + Ints
//
//	ByteBuffer buffer = ByteBuffer.allocate(size);
//
//	buffer.putInt(owner.getIdentityFromClass(this));
//	buffer.putInt(name.length());
//	for (char c : name.toCharArray()) { buffer.putChar(c); }
//
//	buffer.putInt(parents.size());
//	for (OntologyClass p : parents) { buffer.putInt(owner.getIdentityFromClass(p)); }
//
//	buffer.putInt(children.size());
//	for (OntologyClass p : children) { buffer.putInt(owner.getIdentityFromClass(p)); }
//
//	return buffer;
//}
//
///**
// * Hydrates this object's state from a binary payload.
// * <p>
// * <b>Usage Protocol:</b>
// * <ol>
// * <li>Read the total number of tags from the file header.</li>
// * <li>Allocate the required instances using {@link #getEmptyArray(int)}.</li>
// * <li>Invoke this method on each instance, passing its specific data slice.</li>
// * </ol>
// *
// * @param owner The map resolving integer identities back to {@code OntologyClass} instances.
// * @param data  The binary payload containing this class's serialized state.
// */
//public void unPack(OntologyHierarchy owner, @NotNull ByteBuffer data) {
//	int storedIdentity = data.getInt();
//
//	int nameLen = data.getInt();
//	char[] nameBytes = new char[nameLen];
//	for (int i = 0; i < nameLen; i++) { nameBytes[i] = data.getChar(); }
//	this.name = new String(nameBytes);
//
//	int count = data.getInt();
//	parents.clear(); // THE FIX: Clear default instantiation states
//	parents.ensureCapacity(count);
//	for (int i = 0; i < count; i++) { parents.add(owner.getClassFromIdentity(data.getInt())); }
//
//	count = data.getInt();
//	children.clear(); // THE FIX: Clear default instantiation states
//	children.ensureCapacity(count);
//	for (int i = 0; i < count; i++) { children.add(owner.getClassFromIdentity(data.getInt())); }
//}
//
///**
// * Generates a pre-allocated array of empty ontology classes.
// * This is primarily used as a precursor step to binary deserialization.
// *
// * @param count The number of empty instances to generate.
// * @return An array containing {@code count} uninitialized {@code OntologyClass} objects.
// */
//public static OntologyClass[] getEmptyArray(int count) {
//	OntologyClass[] result = new OntologyClass[count];
//	for(int i = 0; i < count; i++) { result[i] = new OntologyClass(); }
//	return result;
//}
//
//public static void main(String[] args) {
//	// Main execution entry point for isolated testing
//}
//
//}



/* Deprecated lines of code, just as a reminder in case of forgetting context.



 * Constructs a new ontology class with multiple initial parents.
 * For creating a class that is heritage of intersections.
 *
 * @param name    The descriptive name of the tag.
 * @param parents A list of initial parent classes.
 * /
public OntologyClass(String name, @NotNull List<OntologyClass> parents) {
	this.name = name;
	parents.forEach(this::addParent);
}


 * Constructs a new ROOT ontology class, usable only for root classes and empty generations.
 *
 * @param name The descriptive name of the ROOT.
 * /
private OntologyClass(String name) {
	this.name = name;
}


@Contract("_, _ -> new")
public static @NotNull OntologyClass makeTemporalWithParents(String name, @NotNull List<OntologyClass> requiredParents) {
	OntologyClass result = new OntologyClass(name, ROOT_TEMPORAL_CLASS);
	result.parents.addAll(requiredParents);
	return result;
}
@Contract("_, _ -> new")
public static @NotNull OntologyClass makeTemporalWithChildren(String name, @NotNull List<OntologyClass> requiredChildren) {
	OntologyClass result = new OntologyClass(name, ROOT_TEMPORAL_CLASS);
	result.children.addAll(requiredChildren);
	return result;
}
@Contract("_, _ -> new")
public static @NotNull OntologyClass makeTemporalOf(String name, @NotNull OntologyClass requiredParent) {
	OntologyClass result = new OntologyClass(name, ROOT_TEMPORAL_CLASS);
	result.parents.add(requiredParent);
	return result;
}
@Contract("_, _ -> new")
public static @NotNull OntologyClass makeTemporalOf(String name) {
	return new OntologyClass(name, ROOT_TEMPORAL_CLASS);
}


*/