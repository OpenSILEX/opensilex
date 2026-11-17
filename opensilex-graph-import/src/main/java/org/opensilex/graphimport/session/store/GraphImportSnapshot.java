//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.graphimport.binding.ColumnReading;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.graphimport.ontology.literal.DateOrder;
import org.opensilex.graphimport.session.CreationRecord;
import org.opensilex.graphimport.session.GraphImportSession;

import java.net.URI;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What is kept of an import between two visits. The cells are not: they are read again from the
 * stored files, so that a better reader tomorrow reaches the imports already made.
 * <p>
 * New fields must be optional: the platform's mapper ignores what it does not know, and a field
 * missing from an older import must read as "not decided yet".
 *
 * @author Arnaud Charleroy
 */
public class GraphImportSnapshot {

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("files")
    private List<StoredFile> files = new ArrayList<>();

    @JsonProperty("layouts")
    private List<StoredLayout> layouts = new ArrayList<>();

    @JsonProperty("experiment")
    private URI experiment;

    @JsonProperty("decisions")
    private List<StoredDecisions> decisions = new ArrayList<>();

    /** The provenance of the import's data; absent from imports stored before it could be chosen. */
    @JsonProperty("provenance")
    private URI provenance;

    /** The resources the user declared; absent from imports stored before they could. */
    @JsonProperty("declared")
    private List<StoredDeclared> declared = new ArrayList<>();

    /** What the creation steps wrote; absent from imports stored before they could. */
    @JsonProperty("creations")
    private List<CreationRecord> creations = new ArrayList<>();

    public static GraphImportSnapshot of(GraphImportSession session) {
        GraphImportSnapshot snapshot = new GraphImportSnapshot();
        snapshot.createdAt = session.getCreatedAt();
        session.getFiles().forEach(file -> snapshot.files.add(new StoredFile(file.getId(), file.getName())));
        session.getLayouts().forEach(chosen -> snapshot.layouts.add(new StoredLayout(chosen.fileId(),
                chosen.sheetName(), chosen.layout().headerRow(), chosen.layout().firstDataRow())));
        snapshot.experiment = session.getExperiment();
        snapshot.provenance = session.getProvenance();
        session.getDecisions().sheets().forEach((sheet, decided) -> snapshot.decisions.add(StoredDecisions.of(sheet, decided)));
        snapshot.decisions.sort(Comparator.comparing(StoredDecisions::getSheet));
        session.getDecisions().declared().forEach(resource -> snapshot.declared.add(StoredDeclared.of(resource)));
        snapshot.creations.addAll(session.getCreations());
        return snapshot;
    }

    /**
     * @return the envelope the store keeps this snapshot in
     */
    public static StoredSession<GraphImportSnapshot> envelope(GraphImportSession session) {
        GraphImportSnapshot snapshot = of(session);
        return new StoredSession<>(session.getId(), session.getOwner(), session.getTitle(), session.getCreatedAt(),
                snapshot.files.stream().map(StoredFile::getId).toList(), snapshot);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<StoredFile> getFiles() {
        return files;
    }

    public void setFiles(List<StoredFile> files) {
        this.files = files == null ? new ArrayList<>() : files;
    }

    public List<StoredLayout> getLayouts() {
        return layouts;
    }

    public void setLayouts(List<StoredLayout> layouts) {
        this.layouts = layouts == null ? new ArrayList<>() : layouts;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public List<StoredDecisions> getDecisions() {
        return decisions;
    }

    public URI getProvenance() {
        return provenance;
    }

    public void setProvenance(URI provenance) {
        this.provenance = provenance;
    }

    public List<CreationRecord> getCreations() {
        return creations;
    }

    public void setCreations(List<CreationRecord> creations) {
        this.creations = creations == null ? new ArrayList<>() : creations;
    }

    public List<StoredDeclared> getDeclared() {
        return declared;
    }

    public void setDeclared(List<StoredDeclared> declared) {
        this.declared = declared == null ? new ArrayList<>() : declared;
    }

    public void setDecisions(List<StoredDecisions> decisions) {
        this.decisions = decisions == null ? new ArrayList<>() : decisions;
    }

    /**
     * What the user decided about a sheet, named as the assembled workbook names it.
     */
    public static class StoredDecisions {

        @JsonProperty("sheet")
        private String sheet;

        @JsonProperty("class_uri")
        private URI classUri;

        @JsonProperty("identifier_column")
        private Integer identifierColumn;

        /** The property of each decided column, keyed by the column position as text. */
        @JsonProperty("properties")
        private Map<String, URI> properties = new TreeMap<>();

        /** How the dates of some columns are written, keyed by the column position as text. */
        @JsonProperty("readings")
        private Map<String, StoredReading> readings = new TreeMap<>();

        /** The columns the user left out; absent from imports stored before the decision existed. */
        @JsonProperty("left_out")
        private List<Integer> leftOut = new ArrayList<>();

        /** The values given for every row, by property; absent from imports stored before they could. */
        @JsonProperty("given")
        private Map<String, String> given = new TreeMap<>();

        /** The variable of each column of measures, keyed by the column position as text. */
        @JsonProperty("measures")
        private Map<String, URI> measures = new TreeMap<>();

        static StoredDecisions of(String sheet, ImportDecisions.SheetDecisions decided) {
            StoredDecisions stored = new StoredDecisions();
            stored.sheet = sheet;
            stored.classUri = decided.classUri();
            stored.identifierColumn = decided.identifierColumn();
            decided.properties().forEach((column, property) -> stored.properties.put(String.valueOf(column), property));
            decided.readings().forEach((column, reading) -> stored.readings.put(String.valueOf(column),
                    StoredReading.of(reading)));
            stored.leftOut = new ArrayList<>(new TreeSet<>(decided.leftOut()));
            decided.given().forEach((property, value) -> stored.given.put(property.toString(), value));
            decided.measures().forEach((column, variable) -> stored.measures.put(String.valueOf(column), variable));
            return stored;
        }

        /**
         * The decisions; a stored column position that is not a number, or a reading that no longer
         * parses, is left out.
         */
        public ImportDecisions.SheetDecisions toModel() {
            Map<Integer, URI> byColumn = new HashMap<>();
            if (properties != null) {
                properties.forEach((column, property) -> {
                    if (isPosition(column) && property != null) {
                        byColumn.put(Integer.valueOf(column), property);
                    }
                });
            }
            Map<Integer, ColumnReading> readingByColumn = new HashMap<>();
            if (readings != null) {
                readings.forEach((column, reading) -> {
                    if (isPosition(column) && reading != null) {
                        reading.toModel().ifPresent(model -> readingByColumn.put(Integer.valueOf(column), model));
                    }
                });
            }
            Set<Integer> left = new HashSet<>();
            if (leftOut != null) {
                leftOut.stream().filter(column -> column != null && column >= 0).forEach(left::add);
            }
            Map<URI, String> values = new HashMap<>();
            if (given != null) {
                given.forEach((property, value) -> Uris.asUri(property).ifPresent(uri -> values.put(uri, value)));
            }
            Map<Integer, URI> measured = new HashMap<>();
            if (measures != null) {
                measures.forEach((column, variable) -> {
                    if (isPosition(column) && variable != null) {
                        measured.put(Integer.valueOf(column), variable);
                    }
                });
            }
            return new ImportDecisions.SheetDecisions(classUri, identifierColumn, byColumn, readingByColumn, left,
                    values, measured);
        }

        public Map<String, URI> getMeasures() {
            return measures;
        }

        public void setMeasures(Map<String, URI> measures) {
            this.measures = measures == null ? new TreeMap<>() : measures;
        }

        public Map<String, String> getGiven() {
            return given;
        }

        public void setGiven(Map<String, String> given) {
            this.given = given == null ? new TreeMap<>() : given;
        }

        private static boolean isPosition(String column) {
            return column != null && column.matches("\\d{1,6}");
        }

        public List<Integer> getLeftOut() {
            return leftOut;
        }

        public void setLeftOut(List<Integer> leftOut) {
            this.leftOut = leftOut == null ? new ArrayList<>() : leftOut;
        }

        public Map<String, StoredReading> getReadings() {
            return readings;
        }

        public void setReadings(Map<String, StoredReading> readings) {
            this.readings = readings == null ? new TreeMap<>() : readings;
        }

        public String getSheet() {
            return sheet;
        }

        public void setSheet(String sheet) {
            this.sheet = sheet;
        }

        public URI getClassUri() {
            return classUri;
        }

        public void setClassUri(URI classUri) {
            this.classUri = classUri;
        }

        public Integer getIdentifierColumn() {
            return identifierColumn;
        }

        public void setIdentifierColumn(Integer identifierColumn) {
            this.identifierColumn = identifierColumn;
        }

        public Map<String, URI> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, URI> properties) {
            this.properties = properties == null ? new TreeMap<>() : properties;
        }
    }

    /**
     * A resource the user declared, with its values by property.
     */
    public static class StoredDeclared {

        @JsonProperty("id")
        private String id;

        @JsonProperty("class_uri")
        private URI classUri;

        /** Its values, by property URI, in the order the user gave them. */
        @JsonProperty("values")
        private Map<String, List<String>> values = new LinkedHashMap<>();

        static StoredDeclared of(ImportDecisions.DeclaredResource resource) {
            StoredDeclared stored = new StoredDeclared();
            stored.id = resource.id();
            stored.classUri = resource.classUri();
            resource.values().forEach((property, list) -> stored.values.put(property.toString(), list));
            return stored;
        }

        /** The resource; a property that is not a URI is left out. */
        public ImportDecisions.DeclaredResource toModel() {
            Map<URI, List<String>> byProperty = new LinkedHashMap<>();
            if (values != null) {
                values.forEach((property, list) -> Uris.asUri(property)
                        .ifPresent(uri -> byProperty.put(uri, list == null ? List.of() : list)));
            }
            return new ImportDecisions.DeclaredResource(id, classUri, byProperty);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public URI getClassUri() {
            return classUri;
        }

        public void setClassUri(URI classUri) {
            this.classUri = classUri;
        }

        public Map<String, List<String>> getValues() {
            return values;
        }

        public void setValues(Map<String, List<String>> values) {
            this.values = values == null ? new LinkedHashMap<>() : values;
        }
    }

    /**
     * How the user said the dates of a column are written.
     */
    public static class StoredReading {

        @JsonProperty("date_order")
        private DateOrder dateOrder;

        /** The offset as {@code +02:00}, or null. */
        @JsonProperty("zone")
        private String zone;

        static StoredReading of(ColumnReading reading) {
            StoredReading stored = new StoredReading();
            stored.dateOrder = reading.dateOrder();
            stored.zone = reading.zone() == null ? null : reading.zone().getId();
            return stored;
        }

        /** The reading, or empty when its zone no longer parses. */
        Optional<ColumnReading> toModel() {
            try {
                return Optional.of(new ColumnReading(dateOrder, zone == null ? null : ZoneOffset.of(zone)));
            } catch (DateTimeException e) {
                return Optional.empty();
            }
        }

        public DateOrder getDateOrder() {
            return dateOrder;
        }

        public void setDateOrder(DateOrder dateOrder) {
            this.dateOrder = dateOrder;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }
    }

    /**
     * The header and first data rows the user chose for a sheet of a stored file, as positions from 0.
     */
    public static class StoredLayout {

        @JsonProperty("file_id")
        private String fileId;

        @JsonProperty("sheet")
        private String sheet;

        @JsonProperty("header_row")
        private int headerRow;

        @JsonProperty("first_data_row")
        private int firstDataRow;

        public StoredLayout() {
            // for Jackson
        }

        public StoredLayout(String fileId, String sheet, int headerRow, int firstDataRow) {
            this.fileId = fileId;
            this.sheet = sheet;
            this.headerRow = headerRow;
            this.firstDataRow = firstDataRow;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getSheet() {
            return sheet;
        }

        public void setSheet(String sheet) {
            this.sheet = sheet;
        }

        public int getHeaderRow() {
            return headerRow;
        }

        public void setHeaderRow(int headerRow) {
            this.headerRow = headerRow;
        }

        public int getFirstDataRow() {
            return firstDataRow;
        }

        public void setFirstDataRow(int firstDataRow) {
            this.firstDataRow = firstDataRow;
        }
    }

    /**
     * A stored file: the identifier its copy is stored under, and the name the user gave it, which
     * tells the reader its kind.
     */
    public static class StoredFile {

        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        public StoredFile() {
            // for Jackson
        }

        public StoredFile(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
