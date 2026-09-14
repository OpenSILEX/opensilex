//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Arnaud Charleroy
 */
public class AiImportSessionCacheTest {

    private static final URI ALICE = URI.create("http://opensilex.test/id/account/alice");
    private static final URI BOB = URI.create("http://opensilex.test/id/account/bob");

    private final AiImportSessionCache cache = new AiImportSessionCache(60);

    @Test
    public void aSessionIsReadableByItsOwner() {
        AiImportSession session = cache.create(ALICE);

        Optional<AiImportSession> found = cache.get(session.getId(), ALICE);
        assertTrue(found.isPresent());
        assertEquals(session.getId(), found.get().getId());
    }

    @Test
    public void aSessionIsInvisibleToAnotherAccount() {
        AiImportSession session = cache.create(ALICE);

        assertFalse("another account must not reach a conversation about someone else's file",
                cache.get(session.getId(), BOB).isPresent());
    }

    @Test
    public void anUnknownIdentifierReturnsNothing() {
        assertFalse(cache.get("not-a-session", ALICE).isPresent());
        assertFalse(cache.get(null, ALICE).isPresent());
    }

    @Test
    public void aRemovedSessionIsGone() {
        AiImportSession session = cache.create(ALICE);
        cache.remove(session.getId());

        assertFalse(cache.get(session.getId(), ALICE).isPresent());
    }

    @Test
    public void eachSessionGetsItsOwnIdentifier() {
        assertNotEquals(cache.create(ALICE).getId(), cache.create(ALICE).getId());
    }

    @Test
    public void theSystemPromptIsReplacedWithoutLosingTheConversation() {
        AiImportSession session = cache.create(ALICE);
        session.replaceSystemPrompt("first state of the instance");
        session.getHistory().add(
                org.opensilex.aiimport.service.dto.ChatMessage.user("what is missing?"));
        session.replaceSystemPrompt("state after the user created the variables");

        assertEquals("the prompt must not accumulate", 2, session.getHistory().size());
        assertEquals("state after the user created the variables",
                session.getHistory().get(0).getContent());
        assertEquals("what is missing?", session.getHistory().get(1).getContent());
    }
}
