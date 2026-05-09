#Header
```html
<span> kayko dsfjsdafklfs hello <a></a> </span> 
```

# Onto Directory - True MVP Progress Tracker

## Phase 1: Foundation (Completed)
- [x] Project architecture established (Zulu JDK 21 FX).
- [x] Maven dependencies mapped (FlatLaf).

## Phase 2: Global State & IO (Completed)
- [x] Create `SettingLogic.java` to handle application state.
- [x] Implement pure Java DOM XML parser.
- [x] Implement "Rebuild on Save" synchronization for `~/.config/onto-directory/settings.xml`.

## Phase 3: The Data Lake Engine (Core Backend)
- [x] **DAG Ontology Structures:** `OntologyClass` implemented with multi-parent/child capabilities.
- [x] **Ontology Manager:** `OntologyHierarchy` implemented to manage the graph and intersection classes.
- [x] **File Mapping:** `FileInterface` implemented to bind integer identities to `Path` objects.
- [x] **Binary Parsing:** High-speed `ByteBuffer` and `DataInputStream` serialization/hydration implemented for `.bin` storage.
- [ ] **The Immutable Vault Ingestion:** (Pending) Write the logic to physically copy dropped PDFs into the `.DATA_LAKE` folder and flag them as Read-Only via OS permissions.
- [ ] **Cryptographic Integrity:** (Pending) Add SHA-256 hashing to `FileInterface` and binary serialization to detect file tampering.

## Phase 4: Graphical User Interface (Next Immediate Steps)
- [ ] **Bootstrapping `Main.java`:**
    - [ ] Delete IntelliJ template code.
    - [ ] Invoke `SettingLogic.onLoad()`.
    - [ ] Initialize `FlatDarkLaf.setup()`.
    - [ ] Construct the primary `JFrame`.
- [ ] **Navigation Shell:**
    - [ ] Implement `SwitcherControllerPanel` (CardLayout).
    - [ ] Build the left-side vertical Data Lake selector strip.
- [ ] **View 1: Welcome Page:**
    - [ ] Build UI mapping to `SettingLogic.dataLakes`.
    - [ ] Add native `java.awt.FileDialog` trigger to register new lakes.
- [ ] **View 2: Data Lake Explorer (Three-Pane):**
    - [ ] Implement `JSplitPane` hierarchy.
    - [ ] **Left Pane:** `JTree` backed by `OntologyHierarchy`.
    - [ ] **Center Pane:** `JTable` rendering `FileInterface` lists.
    - [ ] **Right Pane:** Metadata details and "Export to Workspace" button.

## Phase 5: Threading & Polish
- [ ] Move `DataLakeManager` hydration to a Virtual Thread to prevent UI locking on startup.
- [ ] Wire `JTree` selection events to update the `JTable` file filter.