//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.junit.Test;
import org.opensilex.aiimport.profile.GenericTabularProfile;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookAssembler;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.workbook;

/**
 * What adding a file to a conversation, or taking one away, does to it.
 *
 * @author Arnaud Charleroy
 */
public class SessionFilesTest {

    @SuppressWarnings("unchecked")
    private static WorkbookStructure file(String name, String... sheets) {
        SheetStructure[] built = new SheetStructure[sheets.length];
        for (int i = 0; i < sheets.length; i++) {
            built[i] = sheet(sheets[i], List.of("plot", "x"), row("P01", "1"));
        }
        return workbook(built).setFileName(name);
    }

    private static AiImportSession session(String profile, String... fileNames) {
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setProfileId(profile);
        for (String name : fileNames) {
            session.getFiles().add(SessionFile.of(name, file(name, "plan")));
        }
        session.setFileName(fileNames[0]).assemble();
        return session;
    }

    @Test
    public void aFileCanBeAddedToAConversationTheUserDescribes() {
        assertFalse(SessionFiles.whyNotAdd(session(UserDefinedProfile.ID, "a.csv"), "b.csv").isPresent());
        assertFalse(SessionFiles.whyNotAdd(session(GenericTabularProfile.ID, "a.csv"), "b.csv").isPresent());
    }

    @Test
    public void aFileCannotBeAddedToAConversationATemplateReads() {
        assertTrue(SessionFiles.whyNotAdd(session("vitis-explorer", "a.xlsx"), "b.csv").isPresent());
        assertTrue(SessionFiles.whyNotRemove(session("vitis-explorer", "a.xlsx", "b.xlsx")).isPresent());
    }

    @Test
    public void aFileOfTheSameNameIsRefusedWhateverItsCase() {
        assertTrue(SessionFiles.whyNotAdd(session(UserDefinedProfile.ID, "plan.csv"), "PLAN.csv").isPresent());
    }

    @Test
    public void aConversationHoldsNoMoreThanTheLimit() {
        String[] names = new String[AiImportSession.MAX_FILES];
        for (int i = 0; i < names.length; i++) {
            names[i] = "f" + i + ".csv";
        }

        assertTrue(SessionFiles.whyNotAdd(session(UserDefinedProfile.ID, names), "one-more.csv").isPresent());
    }

    @Test
    public void anAddedFileBringsItsSheets() {
        AiImportSession session = session(UserDefinedProfile.ID, "a.csv");

        SessionFile added = SessionFiles.add(session, "b.csv", file("b.csv", "mesures"));

        assertEquals(2, session.getFiles().size());
        assertEquals(added, session.getFiles().get(1));
        assertEquals(List.of("plan", "mesures"), session.getWorkbook().getSheetNames());
        assertEquals("the conversation keeps the name of its first file", "a.csv", session.getFileName());
    }

    @Test
    public void aRemovedFileTakesItsSheetsAndWhatWasSaidOfThemAway() {
        AiImportSession session = session(UserDefinedProfile.ID, "a.csv");
        SessionFile added = SessionFiles.add(session, "b.csv", file("b.csv", "mesures"));
        session.getDescription().getSheets().put("mesures", new SheetDescription("mesures").setKind(SheetKind.DATA));
        session.getDescription().getSheets().put("plan", new SheetDescription("plan").setKind(SheetKind.OBJECTS));

        assertEquals(added, SessionFiles.remove(session, added.getId()).get());

        assertEquals(List.of("plan"), session.getWorkbook().getSheetNames());
        assertFalse(session.getDescription().find("mesures").isPresent());
        assertTrue("what was said of the other file's sheets stays", session.getDescription().find("plan").isPresent());
    }

    @Test
    public void theFirstFileCanGoAndTheNextNamesTheConversation() {
        AiImportSession session = session(UserDefinedProfile.ID, "a.csv");
        SessionFile second = SessionFiles.add(session, "b.csv", file("b.csv", "mesures"));

        SessionFiles.remove(session, session.getFiles().get(0).getId());

        assertEquals("b.csv", session.getFileName());
        assertEquals(second, session.getFiles().get(0));
    }

    @Test
    public void theOnlyFileCannotBeRemoved() {
        assertTrue(SessionFiles.whyNotRemove(session(UserDefinedProfile.ID, "a.csv")).isPresent());
        assertFalse(SessionFiles.whyNotRemove(session(UserDefinedProfile.ID, "a.csv", "b.csv")).isPresent());
    }

    @Test
    public void aFileThatIsNotThereIsNotRemoved() {
        assertFalse(SessionFiles.remove(session(UserDefinedProfile.ID, "a.csv"), "nobody").isPresent());
    }

    @Test
    public void aSheetTakenByAnotherFileIsNamedAfterItsFile() {
        AiImportSession session = session(UserDefinedProfile.ID, "a.csv");

        SessionFiles.add(session, "b.csv", file("b.csv", "plan"));

        assertEquals(List.of("plan", "b.csv" + WorkbookAssembler.QUALIFIER + "plan"),
                session.getWorkbook().getSheetNames());
    }
}
