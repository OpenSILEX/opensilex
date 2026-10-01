//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What an experiment holds, read once from the instance, in the terms a STAR workbook needs.
 * <p>
 * Names rather than URIs wherever the format expects a name — a STAR file points from one sheet to
 * another by name, and a URI where a name belongs would resolve to nothing when the file is read
 * back. The URIs travel alongside, in columns of their own, so nothing about the origin is lost.
 * <p>
 * Kept apart from the reading and from the writing: the reader is the only part that knows the
 * platform, the writer the only part that knows the workbook, and this is all they share.
 *
 * @author Arnaud Charleroy
 */
public class ExperimentSnapshot {

    private URI uri;
    private String name;
    private String objective;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

    private final List<String> projects = new ArrayList<>();
    private final List<String> organizations = new ArrayList<>();
    private final List<String> suborganizations = new ArrayList<>();
    private final List<String> emails = new ArrayList<>();

    private final List<Facility> facilities = new ArrayList<>();
    private final List<Level> levels = new ArrayList<>();
    private final List<ObjectType> objectTypes = new ArrayList<>();
    private final List<Observation> observations = new ArrayList<>();
    private final Map<URI, Variable> variables = new LinkedHashMap<>();

    /**
     * Observations whose target is neither an object of the experiment nor one of its facilities:
     * counted, and said in the workbook, rather than dropped in silence.
     */
    private int observationsWithoutTarget;

    //#region the parts

    /**
     * A facility of the experiment, where it is and how it is described.
     *
     * @param properties its other properties, by column name, as the type of the facility gives
     *                   them
     */
    public record Facility(URI uri, String name, String town, Double latitude, Double longitude,
                           Map<String, String> properties) {
    }

    /**
     * A level of one of the experiment's factors: a treatment, in STAR's words.
     */
    public record Level(String factor, String name, String description) {
    }

    /**
     * The objects of one type, which become one design sheet.
     *
     * @param localName     the last segment of the type URI, which names the sheet
     * @param propertyNames the columns of the properties beyond the ones STAR names, by property URI
     */
    public record ObjectType(URI uri, String localName, String label, List<ExportedObject> objects,
                             Map<URI, String> propertyNames) {
    }

    /**
     * One object, with what the format has a column for, and the rest of its properties.
     *
     * @param properties values by property URI, already turned into names where the value is a
     *                   resource with one
     */
    public record ExportedObject(URI uri, String name, String germplasm, List<String> levels,
                                 String parent, String x, String y, String comment,
                                 Map<URI, String> properties) {
    }

    /**
     * One value, as recorded: a date alone when the instance holds a date alone.
     */
    public record Observation(URI target, URI variable, LocalDateTime date, boolean dateOnly,
                              Object value) {
    }

    /**
     * A variable as the dictionary describes it.
     *
     * @param code the name the data sheets use as a column header: the alternative name when there
     *             is one, since that is the short code a STAR file expects
     * @param uri  the ontology term it matches when there is one, the variable itself otherwise
     */
    public record Variable(String code, String description, String trait, String method,
                           String unit, String datatype, String uri) {
    }

    //#endregion

    //#region accessors

    public URI getUri() {
        return uri;
    }

    public ExperimentSnapshot setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public ExperimentSnapshot setName(String name) {
        this.name = name;
        return this;
    }

    public String getObjective() {
        return objective;
    }

    public ExperimentSnapshot setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ExperimentSnapshot setDescription(String description) {
        this.description = description;
        return this;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public ExperimentSnapshot setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ExperimentSnapshot setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public List<String> getProjects() {
        return projects;
    }

    public List<String> getOrganizations() {
        return organizations;
    }

    public List<String> getSuborganizations() {
        return suborganizations;
    }

    public List<String> getEmails() {
        return emails;
    }

    public List<Facility> getFacilities() {
        return facilities;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public List<ObjectType> getObjectTypes() {
        return objectTypes;
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public Map<URI, Variable> getVariables() {
        return variables;
    }

    public int getObservationsWithoutTarget() {
        return observationsWithoutTarget;
    }

    public ExperimentSnapshot setObservationsWithoutTarget(int observationsWithoutTarget) {
        this.observationsWithoutTarget = observationsWithoutTarget;
        return this;
    }

    //#endregion
}
