package org.halim.dlake;

import org.halim.hport.OntologyReadingService;

import java.util.HashSet;
import java.util.Set;

public interface OntologyFilter {

/** Resolves the filter directly into a mathematical Set of matching elements. */
Set<FileInterface> resolve(OntologyReadingService ors);

// ---------------------------------------------------------
// LEAF NODES (Data Fetchers)
// ---------------------------------------------------------

/** O(1) Fetch: Returns ONLY the files explicitly tagged with this specific class. */
class DirectElementOf implements OntologyFilter {
	public final int classIdentity;
	public DirectElementOf(int classIdentity) { this.classIdentity = classIdentity; }
	
	@Override
	public Set<FileInterface> resolve(OntologyReadingService ors) {
		return new HashSet<>(ors.getOntologyElements(classIdentity));
	}
	@Override public String toString() { return "DirectElementOf(ID:" + classIdentity + ")"; }
}

/** DAG Fetch: Returns files tagged with this class AND all descendant classes. */
class UnderDomain implements OntologyFilter {
	public final int classIdentity;
	public UnderDomain(int classIdentity) { this.classIdentity = classIdentity; }
	
	@Override
	public Set<FileInterface> resolve(OntologyReadingService ors) {
		return new HashSet<>(ors.getAllOntologyElements(classIdentity));
	}
	@Override public String toString() { return "UnderDomain(ID:" + classIdentity + ")"; }
}

// ---------------------------------------------------------
// LOGICAL GATES (Set Theory Combiners)
// ---------------------------------------------------------

class SubtractFilter implements OntologyFilter {
	public final OntologyFilter base, excluded;
	public SubtractFilter(OntologyFilter base, OntologyFilter excluded) {
		this.base = base;
		this.excluded = excluded;
	}
	
	@Override
	public Set<FileInterface> resolve(OntologyReadingService ors) {
		Set<FileInterface> s1 = base.resolve(ors);
		Set<FileInterface> s2 = excluded.resolve(ors);
		
		// Relative Complement: Removes all elements of s2 from s1
		s1.removeAll(s2);
		return s1;
	}
	@Override public String toString() { return "(" + base + " SUBTRACT " + excluded + ")"; }
}

class AndFilter implements OntologyFilter {
	public final OntologyFilter f1, f2;
	public AndFilter(OntologyFilter f1, OntologyFilter f2) { this.f1 = f1; this.f2 = f2; }
	
	@Override
	public Set<FileInterface> resolve(OntologyReadingService ors) {
		Set<FileInterface> s1 = f1.resolve(ors);
		Set<FileInterface> s2 = f2.resolve(ors);
		
		// Intersection: Modifies s1 to retain only elements also in s2
		s1.retainAll(s2);
		return s1;
	}
	@Override public String toString() { return "(" + f1 + " AND " + f2 + ")"; }
}

class OrFilter implements OntologyFilter {
	public final OntologyFilter f1, f2;
	public OrFilter(OntologyFilter f1, OntologyFilter f2) { this.f1 = f1; this.f2 = f2; }
	
	@Override
	public Set<FileInterface> resolve(OntologyReadingService ors) {
		Set<FileInterface> s1 = f1.resolve(ors);
		Set<FileInterface> s2 = f2.resolve(ors);
		
		// Union: Combines both sets (HashSet inherently prevents duplicates)
		s1.addAll(s2);
		return s1;
	}
	@Override public String toString() { return "(" + f1 + " OR " + f2 + ")"; }
}
}