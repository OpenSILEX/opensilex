//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A misspelt name becomes a match only when a person says so, and only for what was proposed.
 *
 * @author Arnaud Charleroy
 */
public class ConfirmedMatchesTest {

    private static final URI CHARDONNAY = URI.create("http://opensilex.test/id/germplasm/chardonnay");
    private static final URI MERLOT = URI.create("http://opensilex.test/id/germplasm/merlot");

    @Test
    public void aConfirmedNameResolvesAsIfItWereSpeltRight() {
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.GERMPLASM, "Chardonay",
                new ResourceReference(CHARDONNAY, "Chardonnay"));

        ResolvedItem item = new ResolvedItem("Chardonay");
        assertTrue(confirmed.resolve(ReportCategory.GERMPLASM, item));

        assertEquals(ResolutionStatus.FOUND, item.getStatus());
        assertTrue("the interface has to be able to say a person made this link",
                item.isConfirmedByUser());
        assertEquals(CHARDONNAY, item.getMatches().get(0).getUri());
        assertEquals("AiImport.report.hint.confirmedByUser", item.getHintMessage().getKey());
    }

    /**
     * Every row that writes the name the same way — whatever its case or separators — is covered.
     */
    @Test
    public void theConfirmationHoldsForEveryWayOfWritingTheSameName() {
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.GERMPLASM, "Chardonay",
                new ResourceReference(CHARDONNAY, "Chardonnay"));

        assertTrue(confirmed.lookup(ReportCategory.GERMPLASM, "CHARDONAY").isPresent());
        assertTrue(confirmed.lookup(ReportCategory.GERMPLASM, " chardonay ").isPresent());
    }

    @Test
    public void aConfirmationBelongsToItsCategory() {
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.GERMPLASM, "Chardonay",
                new ResourceReference(CHARDONNAY, "Chardonnay"));

        assertFalse(confirmed.resolve(ReportCategory.VARIABLES, new ResolvedItem("Chardonay")));
    }

    @Test
    public void aConfirmationCanBeTakenBack() {
        ConfirmedMatches confirmed = new ConfirmedMatches();
        confirmed.confirm(ReportCategory.GERMPLASM, "Chardonay",
                new ResourceReference(CHARDONNAY, "Chardonnay"));

        assertTrue(confirmed.forget(ReportCategory.GERMPLASM, "chardonay"));
        assertFalse(confirmed.resolve(ReportCategory.GERMPLASM, new ResolvedItem("Chardonay")));
        assertFalse("nothing left to forget", confirmed.forget(ReportCategory.GERMPLASM, "Chardonay"));
    }

    /**
     * What keeps a confirmation honest: only a suggestion the report made for that very name is
     * accepted, never an arbitrary URI sent in a request.
     */
    @Test
    public void onlyASuggestionMadeForThatNameCanBeConfirmed() {
        ResolutionReport report = new ResolutionReport();
        ResolvedItem item = new ResolvedItem("Chardonay").setStatus(ResolutionStatus.MISSING);
        item.getSuggestions().add(new ResourceReference(CHARDONNAY, "Chardonnay"));
        report.getGermplasm().add(item);

        assertTrue(report.suggestion(ReportCategory.GERMPLASM, "chardonay", CHARDONNAY).isPresent());
        assertFalse("not suggested for this name",
                report.suggestion(ReportCategory.GERMPLASM, "Chardonay", MERLOT).isPresent());
        assertFalse("not in this category",
                report.suggestion(ReportCategory.VARIABLES, "Chardonay", CHARDONNAY).isPresent());
        assertFalse("not a name of the file",
                report.suggestion(ReportCategory.GERMPLASM, "Pinot", CHARDONNAY).isPresent());
    }

    /**
     * A correction taught to the instance acts on everyone's imports, so it may only be taught from
     * what this conversation proposed or what its user confirmed — never from an arbitrary URI.
     */
    @Test
    public void aCorrectionIsTaughtOnlyFromASuggestionOrAConfirmedMatch() {
        ResolutionReport report = new ResolutionReport();
        ResolvedItem confirmed = new ResolvedItem("Chardonay").setStatus(ResolutionStatus.FOUND)
                .setConfirmedByUser(true);
        confirmed.getMatches().add(new ResourceReference(CHARDONNAY, "Chardonnay"));
        ResolvedItem foundByName = new ResolvedItem("Merlot").setStatus(ResolutionStatus.FOUND);
        foundByName.getMatches().add(new ResourceReference(MERLOT, "Merlot"));
        report.getGermplasm().add(confirmed);
        report.getGermplasm().add(foundByName);

        assertTrue(report.suggestionOrConfirmedMatch(ReportCategory.GERMPLASM, "chardonay", CHARDONNAY)
                .isPresent());
        assertFalse("a name found as written needs no correction",
                report.suggestionOrConfirmedMatch(ReportCategory.GERMPLASM, "Merlot", MERLOT)
                        .isPresent());
        assertFalse("not what the user confirmed",
                report.suggestionOrConfirmedMatch(ReportCategory.GERMPLASM, "Chardonay", MERLOT)
                        .isPresent());
    }

    /**
     * Teaching takes the same right as editing that kind of resource; plot codes cannot be taught.
     */
    @Test
    public void teachingACorrectionTakesTheRightToModifyTheResource() {
        assertEquals("germplasm-modification",
                ReportCategory.GERMPLASM.getModificationCredential());
        assertEquals("variable-modification",
                ReportCategory.VARIABLES.getModificationCredential());
        assertEquals(null, ReportCategory.SCIENTIFIC_OBJECTS.getModificationCredential());
    }
}
