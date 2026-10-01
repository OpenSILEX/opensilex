//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The profile used when no known template matches: a guess, said to be one.
 *
 * @author Arnaud Charleroy
 */
public class GenericTabularProfileTest {

    private final GenericTabularProfile profile = new GenericTabularProfile();

    private WorkbookStructure workbook() {
        SheetStructure data = new SheetStructure()
                .setName("mesures")
                .setTabular(true)
                .setHeaders(List.of("plot", "date", "height", "comment", ""))
                .setRows(List.of(
                        List.of("P1", "2024-06-01", "12.5",
                                "a long free-text remark nobody would call a measurement", ""),
                        List.of("P2", "2024-06-01", "14", "another long remark about the weather today", "")));
        SheetStructure readme = new SheetStructure().setName("ReadMe").setTabular(false)
                .setHeaders(new ArrayList<>()).setRows(new ArrayList<>()).setText("How to fill this file");
        return new WorkbookStructure().setFileName("anything.xlsx").setSheets(List.of(data, readme));
    }

    @Test
    public void itMatchesEverythingButLosesToAnyTemplate() {
        assertEquals(1, profile.match(workbook()));
        assertEquals(GenericTabularProfile.ID, profile.getId());
        assertFalse(profile.getLabel().isEmpty());
    }

    /**
     * Measurements are offered as variables; identifiers, dates, free text and prose sheets are not.
     */
    @Test
    public void onlyColumnsHoldingMeasurementsBecomeVariables() {
        ExtractedImportPlan plan = profile.extract(workbook());

        List<String> variables = plan.getVariables().stream()
                .map(VariableCandidate::getColumnKey).collect(Collectors.toList());
        assertTrue(variables.toString(), variables.contains("height"));
        assertTrue("plot is short codes, so offered too", variables.contains("plot"));
        assertFalse("a date column is never a variable", variables.contains("date"));
        assertFalse("free text is not a measurement", variables.contains("comment"));
        assertEquals(GenericTabularProfile.ID, plan.getProfileId());
        assertEquals("guessed, needs confirmation", plan.getNotes().get("mapping confidence"));
    }

    @Test
    public void thePromptSaysTheMappingIsAGuess() {
        assertTrue(profile.getPromptContext(workbook()).contains("guess"));
    }
}
