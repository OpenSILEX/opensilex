//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session;

import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookAssembler;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.graphimport.preview.PreviewMemo;
import org.opensilex.graphimport.workbook.SheetLayout;
import org.opensilex.graphimport.workbook.WorkbookAdditions;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One import: the files the user brought, read, and later what they decided about them.
 * <p>
 * The sheets of every file are read together, as one workbook: an identifier repeated in a sheet of
 * another file is the same resource. A CSV file is one sheet named after the file.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportSession {

    /**
     * How many files one import holds: enough for a plan, its measurements and their annexes, few
     * enough that one session never claims the memory of the server.
     */
    public static final int MAX_FILES = 10;

    private final String id;
    private final URI owner;
    private final Instant createdAt;
    private final List<SessionFile> files = new ArrayList<>();

    /** The layouts the user chose, by file and sheet. */
    private final Map<String, ChosenLayout> layouts = new LinkedHashMap<>();

    /** What the user decided about each sheet, by its name in the assembled workbook. */
    private final Map<String, ImportDecisions.SheetDecisions> decisions = new LinkedHashMap<>();

    /** What the creation steps wrote, oldest first. */
    private final List<CreationRecord> creations = new ArrayList<>();

    /** The resources the user declared, by identifier, in the order they were declared. */
    private final Map<String, ImportDecisions.DeclaredResource> declared = new LinkedHashMap<>();

    /** The experiment whose scientific objects may be recognised; null while none is chosen. */
    private URI experiment;

    /** The provenance the data of the import are created with; null while none is chosen. */
    private URI provenance;

    /**
     * The sheets of all files together, worked out when first asked for and forgotten when a file
     * comes or goes.
     */
    private WorkbookStructure workbook;

    /**
     * The last preview, kept while it holds for the workbook, decisions and experiment it was worked out
     * from (see {@link PreviewMemo#holdsFor}); never stored.
     */
    private PreviewMemo previewMemo;

    /**
     * The workbook read, kept while the files' workbook is the same object and the decisions equal, so
     * that the same import gives the same object and the preview memo still holds.
     */
    private KeptRead read;

    /** The workbook read, with what it was read from. */
    private record KeptRead(WorkbookStructure files, ImportDecisions decisions, WorkbookAdditions.ReadWorkbook read) {
    }

    public GraphImportSession(String id, URI owner, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.owner = Objects.requireNonNull(owner);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() {
        return id;
    }

    public URI getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isOwnedBy(URI account) {
        return owner.equals(account);
    }

    /**
     * @return the files, in the order they were added
     */
    public List<SessionFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

    /**
     * @return what the import is called in the list of stored imports: the name of its first file
     */
    public String getTitle() {
        return files.isEmpty() ? "" : files.get(0).getName();
    }

    //#region files

    /**
     * @return why a file of this name cannot be added, or empty when it can
     */
    public Optional<String> whyNotAdd(String fileName) {
        if (files.size() >= MAX_FILES) {
            return Optional.of("An import holds at most " + MAX_FILES + " files.");
        }
        if (files.stream().anyMatch(file -> file.getName().equalsIgnoreCase(fileName))) {
            return Optional.of("A file called '" + fileName + "' is already in this import.");
        }
        return Optional.empty();
    }

    /**
     * @return why a file cannot be taken out, or empty when it can. An import keeps one file at
     * least: without one, there is nothing to resume.
     */
    public Optional<String> whyNotRemove() {
        if (files.size() <= 1) {
            return Optional.of("An import keeps at least one file. Delete the import instead.");
        }
        return Optional.empty();
    }

    /**
     * Adds a file read by the caller. The rules of {@link #whyNotAdd(String)} are the caller's to
     * check first.
     */
    public void add(SessionFile file) {
        files.add(Objects.requireNonNull(file));
        workbook = null;
    }

    /**
     * @return true when a file carried this identifier
     */
    public boolean remove(String fileId) {
        // The decisions about the file's sheets leave with it: a corrected file added back under the same
        // name must not inherit choices made on other columns.
        getFile(fileId).ifPresent(file -> file.getStructure().getSheets()
                .forEach(sheet -> assembledName(fileId, sheet.getName()).ifPresent(decisions::remove)));
        boolean removed = files.removeIf(file -> file.getId().equals(fileId));
        if (removed) {
            layouts.values().removeIf(chosen -> chosen.fileId().equals(fileId));
            workbook = null;
        }
        return removed;
    }

    public Optional<SessionFile> getFile(String fileId) {
        return files.stream().filter(file -> file.getId().equals(fileId)).findFirst();
    }

    //#endregion

    //#region decisions

    /** What the user decided so far, for the interpretation. */
    public ImportDecisions getDecisions() {
        return new ImportDecisions(decisions, List.copyOf(declared.values()));
    }

    public List<CreationRecord> getCreations() {
        return Collections.unmodifiableList(creations);
    }

    public void recordCreation(CreationRecord record) {
        creations.add(record);
    }

    /** Adds a declared resource, or replaces the one with the same identifier where it stands. */
    public void declare(ImportDecisions.DeclaredResource resource) {
        declared.put(resource.id(), resource);
    }

    /** Forgets a declared resource; false when there was none with this identifier. */
    public boolean forgetDeclared(String id) {
        return declared.remove(id) != null;
    }

    /**
     * The workbook the assistant reads: the files' sheets with the values the user gave and the
     * resources they declared (see {@link WorkbookAdditions}), and the decisions to read it with.
     */
    public WorkbookAdditions.ReadWorkbook getReadWorkbook() {
        WorkbookStructure files = getWorkbook();
        ImportDecisions current = getDecisions();
        if (read == null || read.files() != files || !read.decisions().equals(current)) {
            read = new KeptRead(files, current, WorkbookAdditions.apply(files, current));
        }
        return read.read();
    }

    /** Replaces what the user decided about a sheet; nothing decided forgets the sheet. */
    public void decide(String sheet, ImportDecisions.SheetDecisions sheetDecisions) {
        if (sheetDecisions == null || sheetDecisions.equals(ImportDecisions.SheetDecisions.NONE)) {
            decisions.remove(sheet);
        } else {
            decisions.put(sheet, sheetDecisions);
        }
    }

    public URI getExperiment() {
        return experiment;
    }

    /** The caller checks first that the user may read it. */
    public void setExperiment(URI experiment) {
        this.experiment = experiment == null ? null : Uris.expand(experiment);
    }

    public URI getProvenance() {
        return provenance;
    }

    /** The caller checks first that it exists. */
    public void setProvenance(URI provenance) {
        this.provenance = provenance == null ? null : Uris.expand(provenance);
    }

    /** Forgets the column decisions of a sheet whose rows or columns just changed. */
    private void forgetColumnDecisions(String fileId, String sheetName) {
        assembledName(fileId, sheetName).ifPresent(name -> {
            ImportDecisions.SheetDecisions decided = decisions.get(name);
            if (decided != null) {
                decide(name, decided.withoutColumns());
            }
        });
    }

    /** The name a sheet of a file has in the assembled workbook. */
    private Optional<String> assembledName(String fileId, String sheetName) {
        List<WorkbookStructure> structures = files.stream().map(SessionFile::getStructure).toList();
        List<List<String>> names = WorkbookAssembler.sheetNames(structures);
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            if (files.get(fileIndex).getId().equals(fileId)) {
                List<SheetStructure> sheets = structures.get(fileIndex).getSheets();
                for (int sheetIndex = 0; sheetIndex < sheets.size(); sheetIndex++) {
                    if (sheets.get(sheetIndex).getName().equals(sheetName)) {
                        return Optional.of(names.get(fileIndex).get(sheetIndex));
                    }
                }
            }
        }
        return Optional.empty();
    }

    //#endregion

    //#region sheet layouts

    /** Where a sheet of the assembled workbook comes from: a file, and the sheet's name in it. */
    public record SheetLocation(SessionFile file, String sheetName) {
    }

    /** The header and first data rows the user chose for a sheet of a file. */
    public record ChosenLayout(String fileId, String sheetName, SheetLayout layout) {
    }

    /**
     * @param sheet the name of a sheet as the assembled workbook shows it
     * @return the file it comes from and its own name there
     */
    public Optional<SheetLocation> locate(String sheet) {
        List<WorkbookStructure> structures = files.stream().map(SessionFile::getStructure).toList();
        List<List<String>> names = WorkbookAssembler.sheetNames(structures);
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            int sheetIndex = names.get(fileIndex).indexOf(sheet);
            if (sheetIndex >= 0) {
                String ownName = structures.get(fileIndex).getSheets().get(sheetIndex).getName();
                return Optional.of(new SheetLocation(files.get(fileIndex), ownName));
            }
        }
        return Optional.empty();
    }

    /**
     * Replaces a sheet of a file by the same sheet laid out by the user, and remembers the layout so
     * that it is applied again when the import is resumed.
     *
     * @param laidOut the sheet as the layout reads it
     */
    public void lay(String fileId, String sheetName, SheetLayout layout, SheetStructure laidOut) {
        forgetColumnDecisions(fileId, sheetName);
        if (replaceSheet(fileId, sheetName, laidOut)) {
            layouts.put(layoutKey(fileId, sheetName), new ChosenLayout(fileId, sheetName, layout));
        }
    }

    /**
     * Gives a sheet back the layout the reader found, and forgets the one the user chose.
     *
     * @param read the sheet as the reader made it
     */
    public void unlay(String fileId, String sheetName, SheetStructure read) {
        forgetColumnDecisions(fileId, sheetName);
        if (replaceSheet(fileId, sheetName, read)) {
            layouts.remove(layoutKey(fileId, sheetName));
        }
    }

    private boolean replaceSheet(String fileId, String sheetName, SheetStructure replacement) {
        for (int index = 0; index < files.size(); index++) {
            SessionFile file = files.get(index);
            if (file.getId().equals(fileId)) {
                WorkbookStructure structure = file.getStructure();
                List<SheetStructure> sheets = new ArrayList<>();
                structure.getSheets().forEach(sheet -> sheets.add(sheet.getName().equals(sheetName) ? replacement : sheet));
                WorkbookStructure replaced = new WorkbookStructure()
                        .setFileName(structure.getFileName())
                        .setFileSizeBytes(structure.getFileSizeBytes())
                        .setDate1904(structure.isDate1904())
                        .setSheets(sheets);
                files.set(index, new SessionFile(file.getId(), file.getName(), replaced));
                workbook = null;
                return true;
            }
        }
        return false;
    }

    /** The layout the user chose for a sheet of a file, if any. */
    public Optional<SheetLayout> layoutOf(String fileId, String sheetName) {
        return Optional.ofNullable(layouts.get(layoutKey(fileId, sheetName))).map(ChosenLayout::layout);
    }

    /** Every layout the user chose, in the order they chose them. */
    public List<ChosenLayout> getLayouts() {
        return List.copyOf(layouts.values());
    }

    private static String layoutKey(String fileId, String sheetName) {
        return fileId + "\u0000" + sheetName;
    }

    //#endregion

    /**
     * @return the sheets of every file, as one workbook; two sheets of the same name in two files are
     * told apart by the name of their file
     */
    public WorkbookStructure getWorkbook() {
        if (workbook == null) {
            List<WorkbookStructure> structures = new ArrayList<>();
            files.forEach(file -> structures.add(file.getStructure()));
            workbook = WorkbookAssembler.assemble(structures);
        }
        return workbook;
    }

    /** The preview last worked out, with what it was worked out from; null before the first. */
    public PreviewMemo getPreviewMemo() {
        return previewMemo;
    }

    public void setPreviewMemo(PreviewMemo previewMemo) {
        this.previewMemo = previewMemo;
    }
}
