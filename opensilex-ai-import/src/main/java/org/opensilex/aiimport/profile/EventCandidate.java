//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Something that happened during the trial, read from the file and not yet created.
 * <p>
 * A hailstorm, a spraying, an observation round: the templates record these in their own sheets,
 * and OpenSILEX has a place for them. What they have in common is a date, a description, and what
 * they concerned; what varies is how the file says who they concerned, which is why the targets are
 * carried as names and resolved later against the report.
 * <p>
 * The type is kept as the file wrote it. The event vocabulary shipped with OpenSILEX describes
 * device maintenance and moves, not agronomy — there is no class for a spraying — so an event takes
 * the generic type and keeps the original wording in its description rather than being forced into
 * a class that would misstate it.
 *
 * @author Arnaud Charleroy
 */
public class EventCandidate {

    private String sheet;

    private int rowNumber;

    /**
     * What the file called it, e.g. "Observation" or "Traitement phytosanitaire".
     */
    private String typeLabel;

    private String description;

    private LocalDate date;

    /**
     * Names of what the event concerned, resolved later. Empty means the file did not say, and the
     * event concerns the trial as a whole.
     */
    private final List<String> targetNames = new ArrayList<>();

    private DataPoint.TargetKind targetKind = DataPoint.TargetKind.SCIENTIFIC_OBJECT;

    public String getSheet() {
        return sheet;
    }

    public EventCandidate setSheet(String sheet) {
        this.sheet = sheet;
        return this;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public EventCandidate setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
        return this;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public EventCandidate setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EventCandidate setDescription(String description) {
        this.description = description;
        return this;
    }

    public LocalDate getDate() {
        return date;
    }

    public EventCandidate setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public List<String> getTargetNames() {
        return targetNames;
    }

    public EventCandidate addTarget(String name) {
        if (name != null && !name.trim().isEmpty()) {
            targetNames.add(name.trim());
        }
        return this;
    }

    public DataPoint.TargetKind getTargetKind() {
        return targetKind;
    }

    public EventCandidate setTargetKind(DataPoint.TargetKind targetKind) {
        this.targetKind = targetKind;
        return this;
    }

    /**
     * What the event will read as in OpenSILEX: the file's own wording for the type, then its
     * description. Kept together because the vocabulary has no class to hold the type.
     */
    public String toEventDescription() {
        boolean hasType = typeLabel != null && !typeLabel.trim().isEmpty();
        boolean hasDescription = description != null && !description.trim().isEmpty();
        if (hasType && hasDescription) {
            return typeLabel.trim() + " : " + description.trim();
        }
        if (hasType) {
            return typeLabel.trim();
        }
        return hasDescription ? description.trim() : null;
    }
}
