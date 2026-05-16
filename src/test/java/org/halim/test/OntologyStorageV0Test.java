package org.halim.test;

import org.halim.dlake.FileInterface;
import org.halim.dlake.OntologyClass;
import org.halim.dlake.OntologyHierarchyFast;
import org.halim.dlake.OntologyStorageV0;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OntologyStorageV0Test extends OntoDirectoryTestBase {

@Test
public void storage_ReadWriteParity_WithTombstones_MaintainsIntegrity() throws Exception {
	OntologyStorageV0 storage = new OntologyStorageV0();
	Path hierarchyFile = testSandboxRoot.resolve("ontologyClasses.bin");
	Path elementsFile = testSandboxRoot.resolve("identities.bin");
	
	OntologyHierarchyFast sourceHierarchy = new OntologyHierarchyFast();
	sourceHierarchy.ontologyClasses.clear();
	
	OntologyClass root = OntologyClass.makeROOT_ONTOLOGY_CLASS("File");
	root.identityNumber = 0;
	sourceHierarchy.ontologyClasses.add(root);
	
	OntologyClass physics = new OntologyClass("Physics", root);
	physics.identityNumber = 1;
	sourceHierarchy.ontologyClasses.add(physics);
	
	sourceHierarchy.ontologyClasses.add(null);
	
	OntologyClass quantum = new OntologyClass("Quantum", physics);
	quantum.identityNumber = 3;
	sourceHierarchy.ontologyClasses.add(quantum);
	
	ArrayList<FileInterface> originalFiles = new ArrayList<>();
	
	FileInterface f0 = new FileInterface();
	f0.identity = 0; f0.actualName = "ordinary.txt"; f0.diskName = "0x0011aa";
	f0.tagsByIdentity = new ArrayList<>(List.of(1, 3));
	originalFiles.add(f0);
	
	FileInterface f1 = new FileInterface();
	f1.identity = 1; f1.actualName = "root_only.pdf"; f1.diskName = "0x0011ab";
	f1.tagsByIdentity = new ArrayList<>(List.of(0));
	originalFiles.add(f1);
	
	originalFiles.add(null);
	
	FileInterface f3 = new FileInterface();
	f3.identity = 3; f3.actualName = "ordinary2.txt"; f3.diskName = "0x0011ac";
	f3.tagsByIdentity = new ArrayList<>(List.of(3));
	originalFiles.add(f3);
	
	storage.saveOntologyHierarchy(hierarchyFile, sourceHierarchy);
	storage.saveOntologyElements(elementsFile, originalFiles);
	
	OntologyHierarchyFast hydratedHierarchy = new OntologyHierarchyFast();
	storage.loadOntologyHierarchyFromFile(hierarchyFile, hydratedHierarchy);
	ArrayList<FileInterface> hydratedFiles = storage.loadOntologyElementsFromFile(elementsFile);
	
	assertThat(hydratedHierarchy.ontologyClasses).hasSize(4);
	assertThat(hydratedHierarchy.ontologyClasses.get(0).name).isEqualTo("File");
	assertThat(hydratedHierarchy.ontologyClasses.get(1).name).isEqualTo("Physics");
	assertThat(hydratedHierarchy.ontologyClasses.get(2)).isNull();
	assertThat(hydratedHierarchy.ontologyClasses.get(3).name).isEqualTo("Quantum");
	assertThat(hydratedHierarchy.ontologyClasses.get(3).parents.getFirst().name).isEqualTo("Physics");
	
	assertThat(hydratedFiles).hasSize(4);
	assertThat(hydratedFiles.get(0).actualName).isEqualTo("ordinary.txt");
	assertThat(hydratedFiles.get(1).tagsByIdentity).containsExactly(0);
	assertThat(hydratedFiles.get(2)).isNull();
	assertThat(hydratedFiles.get(3).diskName).isEqualTo("0x0011ac");
}
}