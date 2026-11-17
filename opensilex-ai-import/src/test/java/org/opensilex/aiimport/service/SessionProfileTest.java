//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.junit.Test;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.custom.CustomWorkbooks;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;
import org.opensilex.aiimport.profile.star.StarProfile;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The profile of a conversation: the same for everyone when it knows a template, built from the
 * conversation when it knows only what the user said.
 *
 * @author Arnaud Charleroy
 */
public class SessionProfileTest {

    private AiImportSession session(String profileId) {
        AiImportSession session = new AiImportSession("s", URI.create("http://opensilex.test/id/account/a"));
        session.setWorkbook(CustomWorkbooks.planAndMaterial()).setProfileId(profileId);
        return session;
    }

    @Test
    public void aProfileThatKnowsATemplateIsTheRegistrysOwn() {
        Optional<ImportProfile> profile = SessionProfile.of(session(StarProfile.ID));

        assertTrue(profile.isPresent());
        assertEquals(StarProfile.ID, profile.get().getId());
    }

    @Test
    public void aProfileThatKnowsOnlyWhatTheUserSaidCarriesTheSessionsDescription() {
        AiImportSession session = session(UserDefinedProfile.ID);
        session.getDescription().sheet("plan").setKind(SheetKind.IGNORED);
        session.getDescription().sheet("materiel").setKind(SheetKind.IGNORED);

        ImportProfile profile = SessionProfile.of(session).get();
        List<ObjectRow> rows = profile.extractObjectRows(session.getWorkbook());

        assertTrue("both sheets were declared ignored by this session, so no object", rows.isEmpty());
        AiImportSession other = session(UserDefinedProfile.ID);
        assertEquals("another session describes nothing, so the defaults read the sheets", 3,
                SessionProfile.of(other).get().extractObjectRows(other.getWorkbook()).size());
    }

    @Test
    public void twoSessionsDoNotShareADescription() {
        AiImportSession first = session(UserDefinedProfile.ID);
        AiImportSession second = session(UserDefinedProfile.ID);
        first.getDescription().sheet("plan").setKind(SheetKind.IGNORED);

        assertFalse(first.getDescription() == second.getDescription());
        assertTrue(second.getDescription().find("plan").isEmpty());
    }

    @Test
    public void anIdentifierThatNamesNoProfileGivesNone() {
        assertFalse(SessionProfile.of(session("no-such-profile")).isPresent());
        assertFalse(SessionProfile.bind(session("x"), null).isPresent());
    }

    @Test
    public void bindingAProfileOfATemplateReturnsItUnchanged() {
        StarProfile star = new StarProfile();

        assertSame(star, SessionProfile.bind(session(StarProfile.ID), star).get());
    }
}
