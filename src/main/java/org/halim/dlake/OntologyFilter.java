package org.halim.dlake;

import org.halim.hport.OntologyReadingService;

public interface OntologyFilter {


boolean filter(FileInterface element);

String toString();

class ElementOf implements OntologyFilter {
	public OntologyClass ontologyClass;
	public OntologyReadingService ontologyReadingService;
	public ElementOf(OntologyClass ontologyClass, OntologyReadingService ontologyReadingService) {
		this.ontologyClass = ontologyClass;
		this.ontologyReadingService = ontologyReadingService;
	}
	@Override
	public boolean filter(FileInterface element) {
		return ontologyReadingService.isElementOfFilter(ontologyClass, element);
	}
	@Override
	public String toString() { return "Element of \"" + ontologyClass + "\""; }
}
class NotFilter implements OntologyFilter {
	public OntologyFilter ontologyFilter;
	public NotFilter(OntologyFilter ontologyFilter) {
		this.ontologyFilter = ontologyFilter;
	}
	@Override
	public boolean filter(FileInterface element) {
		return !ontologyFilter.filter(element);
	}
	@Override
	public String toString() { return "not(" + ontologyFilter + ")"; }
}
class AndFilter implements OntologyFilter {
	public OntologyFilter ontologyFilter1, ontologyFilter2;
	public AndFilter(OntologyFilter ontologyFilter1, OntologyFilter ontologyFilter2) {
		this.ontologyFilter1 = ontologyFilter1;
		this.ontologyFilter2 = ontologyFilter2;
	}
	@Override
	public boolean filter(FileInterface element) {
		return ontologyFilter1.filter(element) && ontologyFilter2.filter(element);
	}
	@Override
	public String toString() { return "(" + ontologyFilter1 + " AND " + ontologyFilter2 + ")"; }
}
class OrFilter implements OntologyFilter {
	public OntologyFilter ontologyFilter1, ontologyFilter2;
	public OrFilter(OntologyFilter ontologyFilter1, OntologyFilter ontologyFilter2) {
		this.ontologyFilter1 = ontologyFilter1;
		this.ontologyFilter2 = ontologyFilter2;
	}
	@Override
	public boolean filter(FileInterface element) {
		return ontologyFilter1.filter(element) || ontologyFilter2.filter(element);
	}
	@Override
	public String toString() { return "(" + ontologyFilter1 + " OR " + ontologyFilter2 + ")"; }
}


}
