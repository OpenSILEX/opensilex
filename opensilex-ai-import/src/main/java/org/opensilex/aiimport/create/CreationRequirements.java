//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import java.util.ArrayList;
import java.util.List;

/**
 * What it would take to create one kind of resource from the open conversation.
 * <p>
 * Two different things are kept apart. A field is something the user can type; a blocker is
 * something they cannot, because it depends on a resource that does not exist yet. Only the second
 * kind makes the button unavailable outright.
 *
 * @author Arnaud Charleroy
 */
public class CreationRequirements {

    private CreationTarget target;

    private List<RequiredField> fields = new ArrayList<>();

    /**
     * Reasons the creation cannot be attempted at all, phrased for a human.
     */
    private List<String> blockers = new ArrayList<>();

    /**
     * Things worth knowing before creating, that do not prevent it.
     */
    private List<String> warnings = new ArrayList<>();

    public CreationRequirements(CreationTarget target) {
        this.target = target;
    }

    public CreationTarget getTarget() {
        return target;
    }

    public CreationRequirements setTarget(CreationTarget target) {
        this.target = target;
        return this;
    }

    public List<RequiredField> getFields() {
        return fields;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public CreationRequirements block(String reason) {
        if (!blockers.contains(reason)) {
            blockers.add(reason);
        }
        return this;
    }

    public CreationRequirements warn(String warning) {
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
        return this;
    }

    public CreationRequirements field(RequiredField field) {
        fields.add(field);
        return this;
    }

    /**
     * @return true when nothing outside the form stands in the way
     */
    public boolean isAvailable() {
        return blockers.isEmpty();
    }
}
