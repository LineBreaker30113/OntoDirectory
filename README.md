# The Ontology Powered File Manager: "OntoDirectory"

**Designed Elevating the Computer Experience:** By finally giving a proper way to
organize files, you can properly structure your archive with ease.

#### Temporally licenced as copy righted and commercial

**Early Access!!** The app do not work as intended (specifically) the remove tag
action gives error and illegal states, many of the things are unimplemented.

To compile use JDK21, clone this repo, "mvn clean package".

Goal is MVP, made of 4 things working properly "Welcome/Instructions Manual", "dlake package", "Tree View", "Files View".

Can gain information about many architecture decisions from the "main/resources/manual.md"

You can make a text representative full code base you can run 
```shell
java DirectoryToText.java
```

To debug the Ontology Hierarchy actions, just copy and paste the latest crash_dump_YYYYMMDD_HHmmSS.txt
file from the "<data lake path>/bugReports" to chatbot of your choice, alongside some of those: "OntologyHierarchy",
"DataLakeManager", "OntologyClass", "ApplicationController", "WorkspaceController", "WorkspacePanel",
"HierarchyTreeView", "LoadedLakesSection", <pd package classes>.
