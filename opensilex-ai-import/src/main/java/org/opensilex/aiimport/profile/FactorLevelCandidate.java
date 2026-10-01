//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

/**
 * One treatment as the workbook describes it, before it is a level of a factor of the experiment.
 * <p>
 * The code is what the object sheets write in their treatment column, so it becomes the level's
 * name: the platform matches a treatment on that name when the objects are imported, and any other
 * choice would leave every plot without its treatment.
 *
 * @param factor      the factor the file names for it, or {@code null} when the file names none —
 *                    a STAR workbook has one factor and does not name it
 * @param code        the treatment's code, as the object sheets write it
 * @param name        its short name, or {@code null}
 * @param description what the file says about it, or {@code null}
 * @author Arnaud Charleroy
 */
public record FactorLevelCandidate(String sheet, int row, String factor, String code, String name,
                                   String description) {

    /**
     * The level's description: the short name, then what the file says of it.
     */
    public String levelDescription() {
        boolean named = name != null && !name.isBlank() && !name.trim().equals(code);
        boolean described = description != null && !description.isBlank();
        if (named && described) {
            return name.trim() + " — " + description.trim();
        }
        if (named) {
            return name.trim();
        }
        return described ? description.trim() : null;
    }
}
