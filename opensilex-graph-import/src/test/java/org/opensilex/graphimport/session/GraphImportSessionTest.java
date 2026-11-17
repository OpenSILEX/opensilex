//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session;

import org.junit.Test;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.workbook.SheetLayout;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The rules of an import's files, without a server.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportSessionTest {

    private static final URI ALICE = URI.create("test:id/account/alice");
    private static final URI BOB = URI.create("test:id/account/bob");

    private static SessionFile file(String name, String... sheets) {
        WorkbookStructure structure = new WorkbookStructure().setFileName(name);
        for (String sheet : sheets) {
            structure.getSheets().add(new SheetStructure().setName(sheet).setHeaders(List.of("id")));
        }
        return SessionFile.of(name, structure);
    }

    private static GraphImportSession anImport() {
        return new GraphImportSession("s-1", ALICE, Instant.now());
    }

    @Test
    public void anImportBelongsToWhoOpenedItAndIsCalledAfterItsFirstFile() {
        GraphImportSession session = anImport();
        assertEquals("", session.getTitle());

        session.add(file("plan.xlsx", "parcelles"));
        session.add(file("mesures.csv", "mesures"));

        assertEquals("plan.xlsx", session.getTitle());
        assertTrue(session.isOwnedBy(ALICE));
        assertFalse(session.isOwnedBy(BOB));
    }

    @Test
    public void theSheetsOfEveryFileAreReadTogetherAndTwoOfTheSameNameAreToldApart() {
        GraphImportSession session = anImport();
        session.add(file("plan.xlsx", "parcelles", "notes"));
        assertEquals(List.of("parcelles", "notes"), session.getWorkbook().getSheetNames());

        session.add(file("autre.xlsx", "notes"));

        List<String> names = session.getWorkbook().getSheetNames();
        assertEquals(3, names.size());
        assertEquals("notes", names.get(1));
        assertTrue(names.get(2), names.get(2).contains("autre") && names.get(2).contains("notes"));
    }

    @Test
    public void aFileOfTheSameNameIsRefusedWhateverItsCase() {
        GraphImportSession session = anImport();
        session.add(file("plan.csv", "plan"));

        assertTrue(session.whyNotAdd("PLAN.csv").isPresent());
        assertFalse(session.whyNotAdd("mesures.csv").isPresent());
    }

    @Test
    public void anImportHoldsALimitedNumberOfFiles() {
        GraphImportSession session = anImport();
        for (int i = 0; i < GraphImportSession.MAX_FILES; i++) {
            session.add(file("f" + i + ".csv", "f" + i));
        }

        assertTrue(session.whyNotAdd("one-more.csv").isPresent());
    }

    @Test
    public void theLastFileStaysAndARemovedFileTakesItsSheetsAway() {
        GraphImportSession session = anImport();
        SessionFile plan = file("plan.csv", "plan");
        session.add(plan);
        assertTrue(session.whyNotRemove().isPresent());

        SessionFile measures = file("mesures.csv", "mesures");
        session.add(measures);
        assertFalse(session.whyNotRemove().isPresent());
        assertEquals(2, session.getWorkbook().getSheets().size());

        assertTrue(session.remove(measures.getId()));
        assertFalse(session.remove("nobody"));
        assertEquals(List.of("plan"), session.getWorkbook().getSheetNames());
        assertTrue(session.getFile(plan.getId()).isPresent());
        assertFalse(session.getFile(measures.getId()).isPresent());
    }

    @Test
    public void theDecisionsAboutAFileLeaveWithItAndAChangedSheetLosesItsColumnDecisions() {
        GraphImportSession session = anImport();
        SessionFile plan = file("plan.csv", "plan");
        SessionFile notes = file("notes.csv", "notes");
        session.add(plan);
        session.add(notes);
        URI plot = URI.create("http://test.opensilex.org/graph-import#Plot");
        URI comment = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");
        session.decide("plan", new ImportDecisions.SheetDecisions(plot, 0, Map.of(1, comment)));
        session.decide("notes", new ImportDecisions.SheetDecisions(plot, 0, Map.of(1, comment)));

        session.remove(notes.getId());
        session.lay(plan.getId(), "plan", new SheetLayout(0, 1), plan.getStructure().getSheets().get(0));

        assertFalse(session.getDecisions().sheets().containsKey("notes"));
        ImportDecisions.SheetDecisions kept = session.getDecisions().of("plan");
        assertEquals(plot, kept.classUri());
        assertTrue(kept.properties().isEmpty());
        assertEquals(null, kept.identifierColumn());
    }
}
