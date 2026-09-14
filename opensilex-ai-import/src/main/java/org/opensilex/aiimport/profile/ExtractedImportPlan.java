//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.opensilex.aiimport.report.ReportMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a profile could read out of a workbook: the names it will have to match against the
 * instance, and the inconsistencies it noticed while reading.
 * <p>
 * Nothing here is resolved yet. {@code ResolutionService} turns these names into URIs, or reports
 * them as missing.
 *
 * @author Arnaud Charleroy
 */
public class ExtractedImportPlan {

    private String profileId;
    private List<String> experimentNames = new ArrayList<>();
    private List<String> projectNames = new ArrayList<>();
    private List<String> germplasmNames = new ArrayList<>();
    private List<String> scientificObjectNames = new ArrayList<>();

    /**
     * Places the trial was run on. A field in a STAR workbook is a facility, not a scientific
     * object: it outlives the experiment and is referenced by it.
     */
    private List<String> facilityNames = new ArrayList<>();
    private List<String> observerNames = new ArrayList<>();

    /**
     * People the file names as such — a study contact, an author, a data submitter — as opposed to
     * the observer codes that end up on a provenance.
     */
    private final List<PersonCandidate> persons = new ArrayList<>();
    private List<VariableCandidate> variables = new ArrayList<>();

    /**
     * Inconsistencies worth raising with the user, phrased for a human, e.g. a declared season that
     * does not match the observation dates.
     */
    private final List<ReportMessage> anomalies = new ArrayList<>();

    /**
     * Facts the profile wants the assistant to know, keyed by a short label.
     */
    private Map<String, String> notes = new LinkedHashMap<>();

    public String getProfileId() {
        return profileId;
    }

    public ExtractedImportPlan setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public List<String> getExperimentNames() {
        return experimentNames;
    }

    public List<String> getProjectNames() {
        return projectNames;
    }

    public List<String> getGermplasmNames() {
        return germplasmNames;
    }

    public List<String> getScientificObjectNames() {
        return scientificObjectNames;
    }

    public List<String> getFacilityNames() {
        return facilityNames;
    }

    public List<String> getObserverNames() {
        return observerNames;
    }

    public List<PersonCandidate> getPersons() {
        return persons;
    }

    public List<VariableCandidate> getVariables() {
        return variables;
    }

    /**
     * The anomalies in English, which is what the prompt carries.
     */
    public List<String> getAnomalies() {
        return ReportMessage.english(anomalies);
    }

    /**
     * The same anomalies as keys and parameters, which is what the interface renders.
     */
    public List<ReportMessage> getAnomalyMessages() {
        return anomalies;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public ExtractedImportPlan addAnomaly(ReportMessage anomaly) {
        if (!ReportMessage.alreadySaid(anomalies, anomaly)) {
            anomalies.add(anomaly);
        }
        return this;
    }

    public ExtractedImportPlan addAnomaly(String anomaly) {
        return addAnomaly(ReportMessage.plain(anomaly));
    }

    public ExtractedImportPlan note(String key, String value) {
        notes.put(key, value);
        return this;
    }
}
