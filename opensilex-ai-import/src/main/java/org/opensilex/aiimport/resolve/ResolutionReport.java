//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.aiimport.report.ReportMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the instance already has, and what it does not, for one uploaded file.
 * <p>
 * This is the deliverable of a session: it is rendered in the interface, serialised into the
 * assistant's prompt, and recomputed on demand once the user has created the missing resources.
 *
 * @author Arnaud Charleroy
 */
public class ResolutionReport {

    private Instant computedAt = Instant.now();
    private String profileId;

    private List<ResolvedItem> experiments = new ArrayList<>();
    private List<ResolvedItem> projects = new ArrayList<>();
    private List<ResolvedItem> variables = new ArrayList<>();
    private List<ResolvedItem> germplasm = new ArrayList<>();
    private List<ResolvedItem> scientificObjects = new ArrayList<>();
    private List<ResolvedItem> facilities = new ArrayList<>();

    /**
     * People the file names — a study contact, an author — as OpenSILEX persons.
     */
    private final List<ResolvedItem> persons = new ArrayList<>();

    /**
     * Inconsistencies noticed while reading the file, phrased for a human.
     */
    private final List<ReportMessage> anomalies = new ArrayList<>();

    /**
     * Lookups that could not be completed, e.g. an unreachable shared resource instance. Recorded
     * rather than raised, so that one unavailable service does not sink the whole analysis.
     */
    private final List<ReportMessage> warnings = new ArrayList<>();

    /**
     * Facts the profile wants surfaced, keyed by a short label.
     */
    private Map<String, String> notes = new LinkedHashMap<>();

    public Instant getComputedAt() {
        return computedAt;
    }

    public ResolutionReport setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public ResolutionReport setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public List<ResolvedItem> getExperiments() {
        return experiments;
    }

    public List<ResolvedItem> getProjects() {
        return projects;
    }

    public List<ResolvedItem> getVariables() {
        return variables;
    }

    public List<ResolvedItem> getGermplasm() {
        return germplasm;
    }

    public List<ResolvedItem> getScientificObjects() {
        return scientificObjects;
    }

    public List<ResolvedItem> getFacilities() {
        return facilities;
    }

    public List<ResolvedItem> getPersons() {
        return persons;
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

    public List<String> getWarnings() {
        return ReportMessage.english(warnings);
    }

    public List<ReportMessage> getWarningMessages() {
        return warnings;
    }

    public ResolutionReport addAnomaly(ReportMessage anomaly) {
        anomalies.add(anomaly);
        return this;
    }

    public ResolutionReport addAnomaly(String anomaly) {
        return addAnomaly(ReportMessage.plain(anomaly));
    }

    /**
     * Adds the anomalies a profile found, which are already messages.
     */
    public ResolutionReport addAnomalies(List<ReportMessage> found) {
        anomalies.addAll(found);
        return this;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public ResolutionReport addWarning(ReportMessage warning) {
        // The same lookup failing twice says nothing twice.
        if (!ReportMessage.alreadySaid(warnings, warning)) {
            warnings.add(warning);
        }
        return this;
    }

    public ResolutionReport addWarning(String warning) {
        return addWarning(ReportMessage.plain(warning));
    }

    public int count(List<ResolvedItem> items, ResolutionStatus status) {
        int total = 0;
        for (ResolvedItem item : items) {
            if (item.getStatus() == status) {
                total++;
            }
        }
        return total;
    }

    /**
     * @return true when nothing has to be created before the data could be imported
     */
    public boolean isComplete() {
        return missingCount() == 0 && ambiguousCount() == 0;
    }

    public int missingCount() {
        return count(experiments, ResolutionStatus.MISSING)
                + count(projects, ResolutionStatus.MISSING)
                + count(variables, ResolutionStatus.MISSING)
                + count(germplasm, ResolutionStatus.MISSING)
                + count(scientificObjects, ResolutionStatus.MISSING)
                + count(facilities, ResolutionStatus.MISSING);
    }

    public int ambiguousCount() {
        return count(experiments, ResolutionStatus.AMBIGUOUS)
                + count(projects, ResolutionStatus.AMBIGUOUS)
                + count(variables, ResolutionStatus.AMBIGUOUS)
                + count(germplasm, ResolutionStatus.AMBIGUOUS)
                + count(scientificObjects, ResolutionStatus.AMBIGUOUS)
                + count(facilities, ResolutionStatus.AMBIGUOUS);
    }
}
