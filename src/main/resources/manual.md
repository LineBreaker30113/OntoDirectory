//# ==============================================================================
//# ONTO DIRECTORY - LIVING ARCHITECTURE & MANUAL SPECIFICATION
//# ==============================================================================
//# DEV NOTE: This file is parsed by ManualParser.java.
//# Lines starting with '//' are skipped by the GUI parser but remain here.
//# ==============================================================================

# Introduction to Onto Directory
````html
Onto Directory is an elegant, highly structured environment built to safely organize long-term file archives.
<div style='color: #FFD700; margin-top: 8px;'><b>⚠ Paradigm Shift Notice:</b>
    This application does not use folders. It uses a Directed Acyclic Graph (DAG).</div>
While standard applications assume you instantly understand their workflow, Onto Directory includes this guide to ensure you master its paradigm. Scan the summaries below, and expand the details only when you are ready to dive deeper into the mechanics.
````

````html
Standard file managers force you to memorize rigid paths (e.g., <code>Archive/Work/2026/Invoice.pdf</code>), causing friction when a file belongs in multiple categories. <br><br>
Powered by an <b>Ontology Engine</b>, Onto Directory eliminates folders entirely. You simply place files into a unified "Data Lake" and dynamically assign conceptual attributes (e.g., "Finance", "2026"). This allows you to cross-reference and retrieve data instantly, mirroring how the human brain actually categorizes information.
````

# 1. Getting Started: Your First Data Lake
````html
A Data Lake is the secure, isolated vault for your files. Think of it as a master database. You should aim to create one or two large Data Lakes (e.g., "Personal" and "Work") rather than many small ones.
````

````html
<b>To begin:</b> Click the <b>+ Create / Load Data Lake</b> button on the Welcome Page. Select an empty folder on your hard drive. Onto Directory will immediately initialize a secure <code>.DATA_LAKE</code> indexing structure inside it.<br><br>
Once loaded, your Lake will appear in the Left Sidebar. You can freely switch between Lakes, but only one can be active in your workspace at a time. To ensure your data is perfectly synchronized, always use the <b>Close</b> button on the Lake's header before exiting the application.
````

# 2. Importing & Exporting Files
````html
Files must be ingested into the Data Lake before they can be tagged. Once inside, they are protected. When you need them outside the application, you extract a clean copy via Export.
````

````html
<b>Importing:</b> Click the <b>Import</b> button on your active Data Lake in the sidebar. You can choose to import a specific directory from your computer, or drop files directly into the <code>/imports</code> folder generated next to your Lake. <br><br>
<i>Note:</i> When you import a directory, Onto Directory intelligently reads your existing folder structure and converts it into Tags automatically, saving you hours of manual organization.<br><br>
<b>Exporting:</b> Right-click any file in the File Explorer view and select <b>Export</b>. This will not delete the file from your Lake; it simply writes a pristine copy of the file to the destination of your choice.
````

# 3. The Graph: Tags vs. Folders
````html
Onto Directory uses a Directed Acyclic Graph (DAG). In this system, you do not put files "inside" tags. You assign tags "onto" files. This allows one file to exist in many contexts simultaneously.
````

````html
In a traditional file system, an invoice must live in either 'Finances' or 'Projects'. If you want it in both, you must create a shortcut or duplicate the file.<br><br>
With our ontology structure, you simply assign both tags to the file. <b>How to tag:</b>
<ul>
    <li>Select files in the File Explorer, right-click, and choose <b>Add Tag to Selected</b>.</li>
    <li>Select files in the File Explorer, and drag them directly onto a Tag in the Hierarchy Tree.</li>
</ul>
Every file automatically inherits the root <b>"File"</b> tag.
````

# 4. Building the Hierarchy Tree
````html
Tags can be nested. If you assign the tag "Physics" to be a child of "Science", any file tagged with "Physics" will automatically appear when you search for "Science".
````

````html
The <b>Hierarchy Tree</b> view is your architect's canvas. Here you define the relationships between concepts.<br><br>
<b>Right-click</b> any tag in the tree to:
<ul>
    <li><b>Create Sub-Tag:</b> Generate a new child tag beneath it.</li>
    <li><b>Add Existing Tag as Child:</b> Link an already existing tag underneath the current one. This allows a single tag to have multiple parents (e.g., 'Data Science' can be a child of both 'Math' and 'Computer Science').</li>
    <li><b>Delete Tag:</b> Removes the tag. Any files holding only that tag will safely fall back to the root "File" category.</li>
</ul>
<i>Pro Tip:</i> You can double-click a tag in the tree to instantly view all files associated with it.
````

# 5. Finding Data: The AST Filter Builder
````html
The File Explorer uses an Abstract Syntax Tree (AST) to filter files. Instead of a simple text search, you snap together logical blocks to build highly precise queries.
````

````html
At the top of the File Explorer is the Filter Track. Click <b>[ + ]</b> to begin building a query.<br>
<ul>
    <li><b>Domain Of:</b> Finds all files holding this tag AND any of its children.</li>
    <li><b>Direct Element Of:</b> Finds ONLY files explicitly assigned this exact tag, ignoring children.</li>
    <li><b>AND / OR / SUBTRACT:</b> Logical operators to combine tags. Want to find files that are "Science" AND "2026" but SUBTRACT "Drafts"? Build the blocks, then click <b>Apply Filter</b>.</li>
</ul>
If you build a very useful, complex filter, click <b>Save</b> to permanently generate a new Tag that holds the results of that exact query.
````

# 6. Advanced Organization Perspectives
````html
Because files can hold infinite tags, you can create "Classification Perspectives" to sort your archive from entirely different angles without disrupting your primary tree.
````

````html
Don't just tag by subject. Create parallel hierarchies at the root level:<br>
<ul>
    <li><b>By Extension:</b> Create a parent tag called 'FileTypes', with children like 'PDF', 'JPG', and 'TXT'.</li>
    <li><b>By Status:</b> Create a parent tag called 'Workflow', with children like 'To-Do', 'In Progress', and 'Archived'.</li>
</ul>
When you select a file in the Explorer, hovering over it will reveal a clean tooltip showing every perspective it belongs to.
````

# 7. System Maintenance & Telemetry
````html
Onto Directory tracks your actions safely and allows you to reverse mistakes. It also includes an Optimization protocol to keep your graph running at lightning speed.
````

````html
<b>Undo / Redo:</b> The system maintains a memory of your last 100 actions. If you accidentally delete a tag, delete an edge, or rename a file, simply press <b>Ctrl+Z</b> to undo it, or <b>Ctrl+Y</b> to redo it.<br><br>
<b>Database Optimization (Vacuum):</b> When you delete tags, the system leaves "tombstones" (empty spaces) in the database to keep your graph running quickly. Over months of use, these empty spaces can add up. Click the <b>Optimize</b> button on your Data Lake header to safely defragment the graph and reclaim memory.
````