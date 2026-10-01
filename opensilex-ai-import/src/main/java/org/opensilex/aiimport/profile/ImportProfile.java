//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.mapping.HeaderRoleDictionary;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.Collections;
import java.util.List;

/**
 * Knowledge about one family of import files.
 * <p>
 * A profile decides whether it recognises a workbook, tells the assistant what the conventions of
 * that family are, and pulls out the names that will have to be matched against the instance.
 * Implementations are discovered with {@link java.util.ServiceLoader}, so another module can
 * contribute a profile for its own template without touching this one.
 *
 * @author Arnaud Charleroy
 */
public interface ImportProfile {

    /**
     * @return a stable identifier, used in the API and in the configuration
     */
    String getId();

    /**
     * @return a short human label
     */
    String getLabel();

    /**
     * @return how well this profile recognises the workbook, from 0 for "not mine" upwards. The
     * highest score wins; the generic profile scores 1 so that it only wins by default.
     */
    int match(WorkbookStructure structure);

    /**
     * @return the domain conventions of this file family, in prose, to be inserted in the system
     * prompt. Describe what the columns mean and how they map onto OpenSILEX concepts; do not
     * describe the data itself, which the model receives separately.
     */
    String getPromptContext(WorkbookStructure structure);

    /**
     * @return the names to be looked up in the instance, and the inconsistencies noticed while
     * reading
     */
    ExtractedImportPlan extract(WorkbookStructure structure);

    /**
     * Reads the observed objects the workbook describes, one per row, for bulk creation.
     * <p>
     * A profile that cannot tell which sheet lists the objects returns nothing, and bulk creation
     * of objects is simply not offered for that file.
     */
    default List<ObjectRow> extractObjectRows(WorkbookStructure structure) {
        return Collections.emptyList();
    }

    /**
     * Says what the columns of an object sheet become, as far as this profile knows: the starting
     * position of the mapping the user adjusts, sheet by sheet, before the objects are created.
     * <p>
     * The default knows nothing, and every column starts unmapped.
     */
    default ObjectSheetDefaults objectSheetDefaults(WorkbookStructure structure, String sheetName) {
        return ObjectSheetDefaults.none();
    }

    /**
     * Reads the treatments the workbook declares, which become the levels of the experiment's
     * factors — and have to, before any object can name one.
     * <p>
     * A profile with no treatment sheet returns nothing, and factor creation is not offered.
     */
    default List<FactorLevelCandidate> extractFactorLevels(WorkbookStructure structure) {
        return Collections.emptyList();
    }

    /**
     * Reads what happened during the trial: sprayings, incidents, observation rounds.
     * <p>
     * Templates keep these apart from the measurements, in sheets of their own, so they are read
     * apart too. A profile with no such sheet returns nothing and event creation is not offered.
     */
    default List<EventCandidate> extractEvents(WorkbookStructure structure) {
        return Collections.emptyList();
    }

    /**
     * Reads the observations out of the file, still in the file's own words.
     * <p>
     * A profile that cannot tell which column identifies the observed object, or which column
     * holds the date, returns nothing: guessing here would file measurements against the wrong
     * plots, which is worse than refusing. Data creation is then simply not offered.
     *
     * @return the observations, or an empty list when this profile cannot map them with confidence
     */
    default List<DataPoint> extractDataPoints(WorkbookStructure structure) {
        return Collections.emptyList();
    }

    /**
     * Says what a column stands for.
     * <p>
     * The default only recognises what a header name gives away — a date, a comment — and calls
     * everything else a measurement. A profile that knows its template should override it, since
     * this mapping is what the type checks and the creation forms are built on.
     */
    default ColumnRole roleOf(WorkbookStructure structure, String sheetName, String header) {
        if (header == null || header.isEmpty()) {
            return ColumnRole.UNKNOWN;
        }
        // The header dictionary knows the vocabulary of agronomic spreadsheets in both languages,
        // and it is a table lookup: an unrecognised file still gets a business mapping when the
        // language model is unreachable, and the assistant gets a better prompt when it is not.
        ColumnRole known = HeaderRoleDictionary.roleOf(header);
        if (known != ColumnRole.UNKNOWN) {
            return known;
        }
        // Whatever is left holds values against a subject the other columns identify, which is what
        // a variable is. Said as the default rather than as a finding: the generic profile tells
        // the user in as many words that its mapping is a guess.
        return ColumnRole.VARIABLE;
    }
}
