//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The corrections an instance is taught, written to and read back from a real RDF4J store — in
 * memory, but the same SPARQL a server runs.
 *
 * @author Arnaud Charleroy
 */
public class CorrectionStoreTest {

    private static final URI BASE = URI.create("http://opensilex.test/");
    private static final URI CHARDONNAY = URI.create("http://opensilex.test/id/germplasm/chardonnay");
    private static final URI MERLOT = URI.create("http://opensilex.test/id/germplasm/merlot");
    private static final URI ALICE = URI.create("http://opensilex.test/id/account/alice");

    private SailRepository repository;
    private SPARQLService sparql;
    private CorrectionStore store;

    @Before
    public void openAStore() throws Exception {
        repository = new SailRepository(new MemoryStore());
        repository.init();
        sparql = new SPARQLService(new RDF4JConnection(repository.getConnection()));
        store = new CorrectionStore(sparql, CorrectionStore.graphFor(BASE));

        // The resources exist, with a type, as they would in an instance.
        sparql.executeUpdateQuery("INSERT DATA { GRAPH <http://opensilex.test/set/germplasm> { "
                + "<" + CHARDONNAY + "> a <http://www.opensilex.org/vocabulary/oeso#Variety> . "
                + "<" + MERLOT + "> a <http://www.opensilex.org/vocabulary/oeso#Variety> . } }");
    }

    @After
    public void closeIt() {
        repository.shutDown();
    }

    @Test
    public void aTaughtCorrectionIsReadBackWithWhoAndWhen() throws Exception {
        store.remember(ReportCategory.GERMPLASM, "Chardonay", CHARDONNAY, "Chardonnay",
                ALICE, "Alice Martin");

        LearnedCorrection read = store.load().lookup(ReportCategory.GERMPLASM, "CHARDONAY")
                .orElseThrow(AssertionError::new);
        assertEquals(CHARDONNAY, read.getTarget());
        assertEquals("Chardonay", read.getLabel());
        assertEquals("Chardonnay", read.getTargetName());
        assertEquals("Alice Martin", read.getAuthor());
        assertNotNull(read.getCreated());
    }

    /**
     * The misspelling is also put on the resource as a plain skos:hiddenLabel, so any SPARQL
     * client can use what was learned, not only this module.
     */
    @Test
    public void theResourceCarriesAStandardHiddenLabel() throws Exception {
        store.remember(ReportCategory.GERMPLASM, "Chardonay", CHARDONNAY, "Chardonnay", ALICE, null);

        assertTrue(sparql.executeAskQuery(new org.apache.jena.arq.querybuilder.AskBuilder()
                .addWhere("<" + CHARDONNAY + ">", "<http://www.w3.org/2004/02/skos/core#hiddenLabel>",
                        "\"Chardonay\"")));
    }

    @Test
    public void aForgottenCorrectionLeavesNothingBehind() throws Exception {
        LearnedCorrection taught = store.remember(ReportCategory.GERMPLASM, "Chardonay",
                CHARDONNAY, "Chardonnay", ALICE, "Alice Martin");
        store.forget(taught);

        assertTrue(store.load().isEmpty());
        assertFalse(sparql.executeAskQuery(new org.apache.jena.arq.querybuilder.AskBuilder()
                .addWhere("<" + CHARDONNAY + ">", "<http://www.w3.org/2004/02/skos/core#hiddenLabel>",
                        "?any")));
    }

    /**
     * A misspelling means one thing: teaching it again replaces the first answer.
     */
    @Test
    public void teachingTheSameSpellingAgainReplacesIt() throws Exception {
        store.remember(ReportCategory.GERMPLASM, "Chardonay", MERLOT, "Merlot", ALICE, null);
        store.remember(ReportCategory.GERMPLASM, "chardonay", CHARDONNAY, "Chardonnay", ALICE, null);

        assertEquals(CHARDONNAY, store.load().lookup(ReportCategory.GERMPLASM, "Chardonay")
                .orElseThrow(AssertionError::new).getTarget());
    }

    /**
     * A correction pointing at a resource that has since been deleted must not resolve a name to
     * something that no longer exists.
     */
    @Test
    public void aCorrectionToADeletedResourceIsNotReadBack() throws Exception {
        store.remember(ReportCategory.GERMPLASM, "Chardonay", CHARDONNAY, "Chardonnay", ALICE, null);
        sparql.executeUpdateQuery("DELETE WHERE { GRAPH <http://opensilex.test/set/germplasm> { "
                + "<" + CHARDONNAY + "> ?p ?o } }");

        assertFalse(store.load().lookup(ReportCategory.GERMPLASM, "Chardonay").isPresent());
    }

    /**
     * A spreadsheet cell can hold anything; a quote must not be able to end the literal.
     */
    @Test
    public void aSpellingWithQuotesIsStoredAsText() throws Exception {
        store.remember(ReportCategory.GERMPLASM, "Chardo\"nay\\ } ; DROP ALL", CHARDONNAY,
                "Chardonnay", ALICE, null);

        assertTrue(store.load().lookup(ReportCategory.GERMPLASM, "Chardo\"nay\\ } ; DROP ALL")
                .isPresent());
    }

    @Test
    public void aCorrectionBelongsToItsCategory() throws Exception {
        store.remember(ReportCategory.GERMPLASM, "Chardonay", CHARDONNAY, "Chardonnay", ALICE, null);

        assertFalse(store.load().lookup(ReportCategory.VARIABLES, "Chardonay").isPresent());
    }
}
