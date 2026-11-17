//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template;

import org.junit.Test;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;

/**
 * How much a sheet looks like one a template describes: by its headers, however they are written.
 *
 * @author Arnaud Charleroy
 */
public class FileTemplateMatcherTest {

    private static final List<FileTemplate> TEMPLATES = List.of(TemplateFixtures.planAndMeasures());

    private static SheetStructure sheetOf(String... headers) {
        return sheet("any", List.of(headers));
    }

    @Test
    public void aSheetWithTheSameHeadersMatchesItsTemplateSheet() {
        Optional<FileTemplateMatcher.Match> match = FileTemplateMatcher.best(
                sheetOf("plot_id", "variete", "traitement", "altitude"), TEMPLATES);

        assertTrue(match.isPresent());
        assertEquals("plan", match.get().sheet().getName());
        assertEquals(1.0, match.get().score(), 0.0001);
        assertEquals(4, match.get().recognised());
        assertEquals(4, match.get().total());
    }

    @Test
    public void accentsAndCaseDoNotMatter() {
        Optional<FileTemplateMatcher.Match> match = FileTemplateMatcher.best(
                sheetOf("PLOT_ID", "Variété", "Traitement", "Altitude"), TEMPLATES);

        assertTrue(match.isPresent());
        assertEquals(4, match.get().recognised());
    }

    @Test
    public void anExtraColumnLowersTheScoreWithoutRulingTheTemplateOut() {
        Optional<FileTemplateMatcher.Match> match = FileTemplateMatcher.best(
                sheetOf("plot_id", "variete", "traitement", "altitude", "remarque"), TEMPLATES);

        assertTrue(match.isPresent());
        assertEquals(0.8, match.get().score(), 0.0001);
        assertEquals("the sheet's own headers are the total", 5, match.get().total());
        assertEquals(4, match.get().recognised());
    }

    @Test
    public void theBestSheetOfTheTemplateIsTheOneChosen() {
        Optional<FileTemplateMatcher.Match> match = FileTemplateMatcher.best(
                sheetOf("plot_id", "date", "hauteur_cm"), TEMPLATES);

        assertEquals("mesures", match.orElseThrow().sheet().getName());
    }

    @Test
    public void aSheetBelowTheThresholdIsProposedNothing() {
        assertFalse(FileTemplateMatcher.best(sheetOf("plot_id", "nom", "adresse", "ville"), TEMPLATES).isPresent());
        assertFalse(FileTemplateMatcher.best(sheetOf("plot_id", "variete"), List.of()).isPresent());
    }
}
