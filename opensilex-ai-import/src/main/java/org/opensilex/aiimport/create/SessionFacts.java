//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.service.AiImportSession;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Everything one conversation already knows, read from its session and its report.
 * <p>
 * Extracted because two very different things kept asking the same questions: the code that answers
 * "what would this creation take?" and the code that performs it. Both need the URI of a resolved
 * plot, the earliest observed date, the variables available on a shared instance. Keeping those
 * lookups in one place stops the two drifting apart — a requirement that says a creation is
 * possible while the write disagrees is the worst kind of bug this module can have.
 * <p>
 * Nothing here touches a database. Every answer comes from the resolution report, which was
 * computed once, and that is what lets the requirements be tested with no instance at all.
 *
 * @author Arnaud Charleroy
 */
public class SessionFacts {

    private final AiImportSession session;

    public SessionFacts(AiImportSession session) {
        this.session = session;
    }

    /**
     * The variables the report found on a shared resource instance, grouped by the instance they
     * came from — one copy call per instance, and the transaction belongs to that instance.
     */
    public Map<URI, List<URI>> importableVariables() {
        Map<URI, List<URI>> byInstance = new LinkedHashMap<>();
        if (this.session.getReport() == null) {
            return byInstance;
        }
        for (ResolvedItem item : this.session.getReport().getVariables()) {
            if (item.getStatus() != ResolutionStatus.FOUND_IN_SHARED_RESOURCE) {
                continue;
            }
            // The first match: several instances can carry the same name, and picking silently
            // among them is exactly the ambiguity the report exists to raise.
            item.getMatches().stream().findFirst().ifPresent(match -> {
                if (match.getSharedResourceInstance() == null) {
                    return;
                }
                byInstance.computeIfAbsent(URI.create(match.getSharedResourceInstance()),
                        key -> new ArrayList<>()).add(match.getUri());
            });
        }
        return byInstance;
    }

    public String describeInstances(Map<URI, List<URI>> byInstance) {
        List<String> parts = new ArrayList<>();
        byInstance.forEach((instance, uris) -> parts.add(uris.size() + " (" + instance + ")"));
        return String.join(", ", parts);
    }

    /**
     * @return the reconciliation the profile applied to the plot identifiers, when it applied one
     */
    public Optional<String> reconciliationNote() {
        if (this.session.getPlan() == null) {
            return Optional.empty();
        }
        String note = this.session.getPlan().getNotes().get(StarProfile.NOTE_RECONCILIATION);
        return note == null || note.isEmpty() ? Optional.empty() : Optional.of(note);
    }

    public String summarise(List<EventCandidate> events) {
        LocalDate first = null;
        LocalDate last = null;
        Set<String> kinds = new LinkedHashSet<>();
        for (EventCandidate event : events) {
            if (first == null || event.getDate().isBefore(first)) {
                first = event.getDate();
            }
            if (last == null || event.getDate().isAfter(last)) {
                last = event.getDate();
            }
            if (event.getTypeLabel() != null && !event.getTypeLabel().isEmpty()) {
                kinds.add(event.getTypeLabel());
            }
        }
        return events.size() + " (" + String.join(", ", kinds) + ") "
                + (first != null ? first + " → " + last : "");
    }

    /**
     * Turns the names an event concerns into URIs, from the report.
     */
    public List<URI> resolveTargets(EventCandidate candidate) {
        Map<String, URI> byName = candidate.getTargetKind() == DataPoint.TargetKind.FACILITY
                ? facilityUris()
                : scientificObjectUris();

        List<URI> targets = new ArrayList<>();
        for (String name : candidate.getTargetNames()) {
            URI uri = byName.get(name.toLowerCase());
            if (uri != null) {
                targets.add(uri);
            }
        }
        return targets;
    }

    /**
     * Column header to variable URI, lowercased so a header's casing cannot lose a match.
     */
    public Map<String, URI> variableUrisByColumn() {
        Map<String, URI> byColumn = new HashMap<>();
        for (ResolvedItem item : this.session.getReport().getVariables()) {
            if (item.getStatus() == ResolutionStatus.FOUND && item.getMatches().size() == 1) {
                byColumn.put(item.getSourceValue().toLowerCase(),
                        item.getMatches().get(0).getUri());
            }
        }
        return byColumn;
    }

    /**
     * The scientific objects, read from the report rather than looked up again.
     * <p>
     * Since the resolution stopped sampling, the report covers every name the file mentions, and it
     * is recomputed after each creation. Querying a second time here would repeat that work and let
     * the two disagree; reading the report keeps one source for what exists.
     */
    public Map<String, URI> scientificObjectUris() {
        Map<String, URI> byName = new HashMap<>();
        for (ResolvedItem item : this.session.getReport().getScientificObjects()) {
            if (item.getStatus() == ResolutionStatus.FOUND && item.getMatches().size() == 1) {
                byName.put(item.getSourceValue().toLowerCase(), item.getMatches().get(0).getUri());
            }
        }
        return byName;
    }

    public Map<String, URI> facilityUris() {
        Map<String, URI> byName = new HashMap<>();
        for (ResolvedItem item : this.session.getReport().getFacilities()) {
            if (item.getStatus() == ResolutionStatus.FOUND && item.getMatches().size() == 1) {
                byName.put(item.getSourceValue().toLowerCase(), item.getMatches().get(0).getUri());
            }
        }
        return byName;
    }

    public Optional<String> firstMissing(List<ResolvedItem> items) {
        if (items == null) {
            return Optional.empty();
        }
        return items.stream()
                .filter(item -> item.getStatus() == ResolutionStatus.MISSING)
                .map(ResolvedItem::getSourceValue)
                .findFirst();
    }

    public Optional<URI> firstFoundUri(List<ResolvedItem> items) {
        if (items == null) {
            return Optional.empty();
        }
        return items.stream()
                .filter(item -> item.getStatus() == ResolutionStatus.FOUND)
                .filter(item -> item.getMatches().size() == 1)
                .map(item -> item.getMatches().get(0).getUri())
                .findFirst();
    }

    public Optional<LocalDate> earliestObservation() {
        return this.session.getDataPoints().stream()
                .map(DataPoint::getDate)
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo);
    }

    public Optional<LocalDate> latestObservation() {
        return this.session.getDataPoints().stream()
                .map(DataPoint::getDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo);
    }

    public String defaultProvenanceName() {
        String fileName = this.session.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            return "Data entry file";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    public int countStatus(List<ResolvedItem> items, ResolutionStatus status) {
        int count = 0;
        for (ResolvedItem item : items) {
            if (item.getStatus() == status) {
                count++;
            }
        }
        return count;
    }
}
