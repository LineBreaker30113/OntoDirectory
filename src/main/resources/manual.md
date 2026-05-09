//# ==============================================================================
//# ONTO DIRECTORY - LIVING ARCHITECTURE & MANUAL SPECIFICATION
//# ==============================================================================
//# DEV NOTE: This file is parsed by ManualParser.java.
//# Lines starting with '//' are skipped by the GUI parser but remain here
//# to instruct the AI on backend business logic and architectural constraints.
//# ==============================================================================

# Introduction to Onto Directory
````html
Onto Directory is an elegant, highly structured environment built to safely organize long-term file archives.
<div style='color: #FFD700; margin-top: 8px;'><b>⚠ Early Development Notice:</b>
    Operate the application within intended parameters.
    Unorthodox usage may destabilize the graph or file identities.</div>
While standard applications assume you instantly understand their workflow,
Onto Directory includes this integrated guide to ensure you master its paradigm. Scan the summaries below,
and expand the details only when you are ready to dive deeper into the mechanics.
````

````html
Standard file managers force you to memorize rigid paths (e.g., <code>Archive/Work/2026/File.pdf</code>),
causing friction when a file belongs in multiple categories. <br><br>
Powered by an <b>Ontology Engine</b>, Onto Directory eliminates folders entirely.
You simply place files into a unified "Data Lake" and dynamically assign conceptual attributes (e.g., "Physics", "Draft").
This allows you to cross-reference and retrieve data instantly, mirroring how the human brain actually categorizes information.
````

# The Left Sidebar (Command Center)
````html
The Left Sidebar is your global navigation hub. It manages your Data Lakes, application settings, and workspace views.
The sidebar is intentionally divided into three stable zones:<br>
<ul>
    <li><b>Top (Anchor):</b> Persistent entry points like the Welcome Page and this Manual.</li>
    <li><b>Middle (Lakes):</b> Your active Data Lakes. <i>Best Practice:</i>
        Onto Directory is optimized for a few massive macro-lakes rather than dozens of micro-lakes.</li>
    <li><b>Bottom (Workspace):</b> View toggles (Tree View, Venn View, Files View) to dictate how you interact with your data.</li>
</ul>
<b>Pro Tip:</b> Keep the sidebar expanded while establishing your workflow, then toggle it closed using the top arrow
icon for a distraction-free view of your ontology graph.
````

````html

````

# The Data Lake Concept

//# ARCHITECTURE SPEC: INGESTION PIPELINE
//# 1. User places files in actual_folder/imports/
//# 2. App scans /imports/, assigns sequential Integer IDs.
//# 3. Physical files are moved to actual_folder/.DATA_LAKE/ and RENAMED to their Integer ID in HEX.
//# 4. Original names & extensions are saved to identities.bin.
//# 5. A temporal OntologyClass is created: "imported_at_[UNIX_TIMESTAMP]".
//# 6. The new files are mapped to this temporal class, the descendant of the root, "File" class.
````html
A Data Lake is the root workspace for your files. It combines the freedom of raw storage with the mathematical
precision of an ontology graph, offering multiple views to organize and display your contents.<br><br>
<b>Importing Files:</b> To add files to the Data Lake, place them into the lake's designated
<code>/imports</code> directory using your standard OS file explorer. Click the <b>Load</b> button under the
Data Lake header in the Left Sidebar. The application will safely ingest them, tagging them automatically with
an <i>"imported_at_[date]"</i> tag so you can sort them at your own pace.
````

````html
<b>Exporting Files:</b> Locate the file in the Files View, right-click, and select "Export."
You can then choose a destination folder to extract a pristine copy of the file.<br><br>
<b>System Safety:</b> Onto Directory creates a hidden <b>.DATA_LAKE</b> folder.
This directory contains critical indexing files like <code>identities.bin</code> and <code>ontologyHierarchy.bin</code>.
<b>Never manually modify the contents of this hidden folder</b>, as it serves as the brain of your archive.
````

# Ontology Hierarchy & Tags

//# ARCHITECTURE SPEC: DAG RULES
//# 1. Every class MUST descend from the root 'File' class (makeROOT_ONTOLOGY_CLASS).
//# 2. A class can have multiple parents (ArrayList<OntologyClass> parents).
//# 3. Cycles must be prevented during the addParent() operation.
```html
Onto Directory uses a Directed Acyclic Graph (DAG) rather than rigid folders. This allows a single file to have multiple conceptual parents without duplicating the file itself.
The word "Tag" here refers to an ontological class. Every file automatically inherits the root <b>"File"</b> tag, but its classification is up to you.<br><br>
In a traditional file system, an invoice must live in either 'Finances' or 'Projects'. If you want it in both, you must create a shortcut or duplicate the file. With our DAG structure, you simply assign both tags to the file. The file mathematically exists in both spaces simultaneously, resolving the primary limitation of standard file explorers.
````

````html
````