//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

/**
 * Whether a sheet needs an experiment, and whether one of its own columns names it. The one place
 * both are decided: the profile reads them to ask for an experiment, and the description tells the
 * screen which sheets lack one so it can offer to choose or create it there.
 *
 * @author Arnaud Charleroy
 */
public final class ExperimentNaming {

    private ExperimentNaming() {
    }

    /**
     * @return whether rows of this kind belong to an experiment: objects, observations, events and the
     * levels of a factor are declared in one
     */
    public static boolean needs(SheetKind kind) {
        return kind.describesObjects() || kind.holdsData() || kind == SheetKind.FACTORS || kind == SheetKind.EVENTS;
    }

    /**
     * @return whether a column of the sheet described as the experiment holds a name
     */
    public static boolean names(DescribedSheet sheet) {
        return sheet.columns(DescribedRole.TRIAL).stream()
                .anyMatch(column -> !sheet.sheet().distinctValues(column).isEmpty());
    }
}
