//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolvedItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the instance already has for one uploaded file, and what it does not.
 *
 * @author Arnaud Charleroy
 */
public class ResolutionReportDTO {

    @JsonProperty("computed_at")
    private String computedAt;

    @JsonProperty("profile_id")
    private String profileId;

    private List<ResolvedItemDTO> experiments = new ArrayList<>();
    private List<ResolvedItemDTO> projects = new ArrayList<>();
    private List<ResolvedItemDTO> variables = new ArrayList<>();
    private List<ResolvedItemDTO> germplasm = new ArrayList<>();

    @JsonProperty("scientific_objects")
    private List<ResolvedItemDTO> scientificObjects = new ArrayList<>();

    private List<ResolvedItemDTO> facilities = new ArrayList<>();

    private List<ResolvedItemDTO> persons = new ArrayList<>();

    private List<ResolvedItemDTO> organizations = new ArrayList<>();

    private List<String> anomalies = new ArrayList<>();

    @JsonProperty("anomaly_messages")
    private List<ReportMessageDTO> anomalyMessages = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @JsonProperty("warning_messages")
    private List<ReportMessageDTO> warningMessages = new ArrayList<>();
    private Map<String, String> notes = new LinkedHashMap<>();

    @JsonProperty("missing_count")
    private int missingCount;

    @JsonProperty("ambiguous_count")
    private int ambiguousCount;

    @JsonProperty("is_complete")
    private boolean complete;

    public static ResolutionReportDTO fromModel(ResolutionReport model) {
        ResolutionReportDTO dto = new ResolutionReportDTO();
        dto.computedAt = String.valueOf(model.getComputedAt());
        dto.profileId = model.getProfileId();
        dto.experiments = convert(model.getExperiments());
        dto.projects = convert(model.getProjects());
        dto.variables = convert(model.getVariables());
        dto.germplasm = convert(model.getGermplasm());
        dto.scientificObjects = convert(model.getScientificObjects());
        dto.facilities = convert(model.getFacilities());
        dto.persons = convert(model.getPersons());
        dto.organizations = convert(model.getOrganizations());
        dto.anomalies = model.getAnomalies();
        dto.anomalyMessages = ReportMessageDTO.fromModels(model.getAnomalyMessages());
        dto.warnings = model.getWarnings();
        dto.warningMessages = ReportMessageDTO.fromModels(model.getWarningMessages());
        dto.notes = model.getNotes();
        dto.missingCount = model.missingCount();
        dto.ambiguousCount = model.ambiguousCount();
        dto.complete = model.isComplete();
        return dto;
    }

    private static List<ResolvedItemDTO> convert(List<ResolvedItem> items) {
        List<ResolvedItemDTO> converted = new ArrayList<>(items.size());
        items.forEach(item -> converted.add(ResolvedItemDTO.fromModel(item)));
        return converted;
    }

    public String getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(String computedAt) {
        this.computedAt = computedAt;
    }

    @ApiModelProperty(example = "vitis-explorer")
    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public List<ResolvedItemDTO> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ResolvedItemDTO> experiments) {
        this.experiments = experiments;
    }

    public List<ResolvedItemDTO> getProjects() {
        return projects;
    }

    public void setProjects(List<ResolvedItemDTO> projects) {
        this.projects = projects;
    }

    public List<ResolvedItemDTO> getVariables() {
        return variables;
    }

    public void setVariables(List<ResolvedItemDTO> variables) {
        this.variables = variables;
    }

    public List<ResolvedItemDTO> getGermplasm() {
        return germplasm;
    }

    public void setGermplasm(List<ResolvedItemDTO> germplasm) {
        this.germplasm = germplasm;
    }

    public List<ResolvedItemDTO> getScientificObjects() {
        return scientificObjects;
    }

    public void setScientificObjects(List<ResolvedItemDTO> scientificObjects) {
        this.scientificObjects = scientificObjects;
    }

    public List<ResolvedItemDTO> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<ResolvedItemDTO> facilities) {
        this.facilities = facilities;
    }

    public List<ResolvedItemDTO> getPersons() {
        return persons;
    }

    public void setPersons(List<ResolvedItemDTO> persons) {
        this.persons = persons;
    }

    public List<ResolvedItemDTO> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<ResolvedItemDTO> organizations) {
        this.organizations = organizations;
    }

    @ApiModelProperty(value = "Inconsistencies found while reading the file")
    public List<String> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<String> anomalies) {
        this.anomalies = anomalies;
    }

    public List<ReportMessageDTO> getAnomalyMessages() {
        return anomalyMessages;
    }

    public void setAnomalyMessages(List<ReportMessageDTO> anomalyMessages) {
        this.anomalyMessages = anomalyMessages;
    }

    @ApiModelProperty(value = "Lookups that could not be completed, for instance an unreachable "
            + "shared resource instance")
    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<ReportMessageDTO> getWarningMessages() {
        return warningMessages;
    }

    public void setWarningMessages(List<ReportMessageDTO> warningMessages) {
        this.warningMessages = warningMessages;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public int getAmbiguousCount() {
        return ambiguousCount;
    }

    public void setAmbiguousCount(int ambiguousCount) {
        this.ambiguousCount = ambiguousCount;
    }

    @ApiModelProperty(value = "True when nothing has to be created before the data could be imported")
    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
