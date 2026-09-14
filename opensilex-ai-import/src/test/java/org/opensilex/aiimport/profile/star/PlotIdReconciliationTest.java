//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Arnaud Charleroy
 */
public class PlotIdReconciliationTest {

    @Test
    public void theReferenceTemplateNeedsReconciling() throws Exception {
        PlotIdReconciliation reconciliation = of(WorkbookFixture.starStandard());

        assertTrue("the two sheets use opposite conventions", reconciliation.isNeeded());
        assertTrue("every observation must find its plot", reconciliation.isComplete());
        assertEquals(44, reconciliation.getResolvedCount());
        assertEquals(44, reconciliation.getDeclaredPlots());
        assertEquals(40, reconciliation.getRecomposedMatches());
        assertEquals("the untreated controls encode their block as a digit, so they already match",
                4, reconciliation.getDirectMatches());
    }

    @Test
    public void theFilledWorkbookBehavesTheSameWay() throws Exception {
        PlotIdReconciliation reconciliation = of(WorkbookFixture.starExample());

        assertTrue(reconciliation.isNeeded());
        assertTrue(reconciliation.isComplete());
        assertEquals(44, reconciliation.getResolvedCount());
        assertEquals(40, reconciliation.getRecomposedMatches());
        assertEquals(4, reconciliation.getDirectMatches());
    }

    @Test
    public void anIdentifierResolvesToTheOneThePlotSheetDeclares() throws Exception {
        PlotIdReconciliation reconciliation = of(WorkbookFixture.starStandard());

        assertEquals("A10", reconciliation.resolve("10A"));
        assertEquals("C2", reconciliation.resolve("2C"));
        assertEquals("a control matches without recomposition", "TNT1", reconciliation.resolve("TNT1"));
        assertNull(reconciliation.resolve("nothing like a plot"));
    }

    @Test
    public void theMismatchIsExplainedWithoutBlamingTheUser() throws Exception {
        String description = of(WorkbookFixture.starStandard()).describe();

        assertTrue(description.contains("44"));
        assertTrue("the user's data is not at fault and the message must say so",
                description.contains("not an error in your data"));
        assertTrue("nothing is applied until confirmed", description.contains("Confirm"));
    }

    @Test
    public void aWorkbookWithoutAPlotSheetReconcilesNothing() {
        PlotIdReconciliation reconciliation = new PlotIdReconciliation(null, null);

        assertEquals(0, reconciliation.getResolvedCount());
        assertTrue(reconciliation.getUnmatchedDataIds().isEmpty());
        assertNull("nothing to describe when there is nothing to reconcile", reconciliation.describe());
    }

    private PlotIdReconciliation of(WorkbookStructure workbook) {
        StarSheets sheets = new StarSheets(workbook);
        return new PlotIdReconciliation(sheets.plots().orElse(null), sheets.dataSheets());
    }
}
