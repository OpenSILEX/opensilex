//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fallback profile: knows nothing about the file beyond the fact that it holds tables.
 * <p>
 * It sets aside the columns whose header names an identifier, a date or an observer, and offers
 * every remaining column as a possible variable. The assistant is then told explicitly that the
 * mapping is a guess and must be confirmed with the user.
 *
 * @author Arnaud Charleroy
 */
public class GenericTabularProfile implements ImportProfile {

    public static final String ID = "generic";

    private static final Set<String> NON_VARIABLE_HEADERS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("uri", "id", "identifiant", "code", "name", "nom", "label", "libelle",
                    "date", "datetime", "timestamp", "annee", "year", "campagne", "millesime",
                    "observateur", "observer", "operateur", "comment", "commentaire", "remarque",
                    "obs_libre", "notes", "note libre")));

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel() {
        return "Generic tabular file";
    }

    @Override
    public int match(WorkbookStructure structure) {
        // Always applicable, never preferred over a profile that recognises the template.
        return 1;
    }

    @Override
    public String getPromptContext(WorkbookStructure structure) {
        return String.join("\n", Arrays.asList(
                "No known template matched this workbook, so the column mapping below is a guess.",
                "Ask the user to confirm, for each sheet: which column identifies the observed",
                "object, which column holds the observation date, and which columns hold measured",
                "variables. Do not assume a mapping is correct because it looks plausible."
        ));
    }

    @Override
    public ExtractedImportPlan extract(WorkbookStructure structure) {
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId(ID);

        for (SheetStructure sheet : structure.getSheets()) {
            if (!sheet.isTabular()) {
                continue;
            }
            for (String header : sheet.getHeaders()) {
                if (header.isEmpty() || isNonVariableHeader(header)) {
                    continue;
                }
                if (!columnHoldsMeasurements(sheet, header)) {
                    continue;
                }
                candidate(plan, header).addSheet(sheet.getName());
            }
        }

        plan.note("sheets", String.valueOf(structure.getSheets().size()));
        plan.note("mapping confidence", "guessed, needs confirmation");
        return plan;
    }

    private boolean isNonVariableHeader(String header) {
        String lower = header.toLowerCase();
        if (NON_VARIABLE_HEADERS.contains(lower)) {
            return true;
        }
        return ExcelValueParser.looksLikeDateColumn(header);
    }

    /**
     * A column is offered as a variable when at least one of its values is not free text: a
     * variable carries measurements or codes, not sentences.
     */
    private boolean columnHoldsMeasurements(SheetStructure sheet, String header) {
        List<String> values = sheet.distinctValues(header);
        if (values.isEmpty()) {
            return false;
        }
        int shortValues = 0;
        for (String value : values) {
            if (ExcelValueParser.parseNumber(value) != null || value.length() <= 20) {
                shortValues++;
            }
        }
        return shortValues * 2 >= values.size();
    }

    private VariableCandidate candidate(ExtractedImportPlan plan, String columnKey) {
        for (VariableCandidate existing : plan.getVariables()) {
            if (existing.getColumnKey().equalsIgnoreCase(columnKey)) {
                return existing;
            }
        }
        VariableCandidate created = new VariableCandidate(columnKey);
        plan.getVariables().add(created);
        return created;
    }
}
