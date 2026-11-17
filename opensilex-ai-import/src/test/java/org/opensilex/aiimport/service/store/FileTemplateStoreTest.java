//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import org.junit.Test;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateFixtures;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Where the templates are kept: what a user keeps is theirs, what they share is everyone's, and only
 * the author deletes.
 *
 * @author Arnaud Charleroy
 */
public class FileTemplateStoreTest extends AbstractMongoIntegrationTest {

    private static final URI ALICE = TemplateFixtures.AUTHOR;
    private static final URI BOB = URI.create("http://opensilex.test/id/account/bob");

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(FileTemplateStore.COLLECTION);
    }

    private FileTemplateStore store() {
        return new FileTemplateStore(getMongoDBService(), ObjectMapperContextResolver.getObjectMapper());
    }

    /**
     * Dates are kept to the millisecond: two templates made in the same one have no order.
     */
    private static void pause() throws InterruptedException {
        Thread.sleep(5);
    }

    @Test
    public void aTemplateIsKeptAndReadBackWithItsSheets() throws Exception {
        FileTemplate template = TemplateFixtures.planAndMeasures();
        store().save(template);

        FileTemplate back = store().find(template.getId(), ALICE).orElseThrow(AssertionError::new);

        assertEquals("plan et mesures", back.getName());
        assertEquals("Alice", back.getAuthorName());
        assertEquals(2, back.getSheets().size());
        assertEquals(template.getSheets().get(0).roles(), back.getSheets().get(0).roles());
        assertEquals(TemplateFixtures.PLOT, back.getSheets().get(0).getType());
        assertEquals(template.getSheets().get(0).getProperties(), back.getSheets().get(0).getProperties());
    }

    @Test
    public void aPrivateTemplateIsInvisibleToAnotherUserUntilItIsShared() throws Exception {
        FileTemplate template = TemplateFixtures.planAndMeasures();
        store().save(template);

        assertFalse(store().find(template.getId(), BOB).isPresent());
        assertTrue(store().list(BOB).isEmpty());

        store().save(template.share(true));

        assertTrue(store().find(template.getId(), BOB).isPresent());
        assertEquals(1, store().list(BOB).size());
        assertTrue(store().list(BOB).get(0).isShared());
    }

    @Test
    public void theListHoldsTheUsersOwnAndTheSharedOnesMostRecentFirst() throws Exception {
        FileTemplate mine = TemplateFixtures.planAndMeasures().rename("mine");
        pause();
        FileTemplate theirs = TemplateFixtures.planAndMeasures().rename("theirs").ownedBy(BOB, "Bob").share(true);
        pause();
        FileTemplate hidden = TemplateFixtures.planAndMeasures().rename("hidden").ownedBy(BOB, "Bob");
        store().save(mine);
        store().save(hidden);
        store().save(theirs);

        assertEquals(List.of("theirs", "mine"),
                store().list(ALICE).stream().map(FileTemplate::getName).collect(Collectors.toList()));
    }

    @Test
    public void onlyTheAuthorDeletesATemplate() throws Exception {
        FileTemplate template = TemplateFixtures.planAndMeasures().share(true);
        store().save(template);

        assertFalse("shared is not owned", store().delete(template.getId(), BOB));
        assertTrue(store().find(template.getId(), BOB).isPresent());

        assertTrue(store().delete(template.getId(), ALICE));
        assertFalse(store().find(template.getId(), ALICE).isPresent());
        assertFalse(store().delete(template.getId(), ALICE));
    }

    @Test
    public void savingTwiceReplacesTheTemplate() throws Exception {
        FileTemplate template = TemplateFixtures.planAndMeasures();
        store().save(template);
        store().save(template.rename("renamed"));

        assertEquals(1, store().list(ALICE).size());
        assertEquals("renamed", store().list(ALICE).get(0).getName());
    }
}
