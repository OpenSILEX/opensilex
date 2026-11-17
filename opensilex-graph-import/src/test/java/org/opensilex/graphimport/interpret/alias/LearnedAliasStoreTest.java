//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.alias;

import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * The aliases a user teaches, kept per user in MongoDB.
 *
 * @author Arnaud Charleroy
 */
public class LearnedAliasStoreTest extends AbstractMongoIntegrationTest {

    private static final URI ARNAUD = URI.create("http://test.opensilex.org/id/user/arnaud");
    private static final URI CAMILLE = URI.create("http://test.opensilex.org/id/user/camille");
    private static final URI PLOT = URI.create("http://test.opensilex.org/graph-import#Plot");
    private static final URI TREE = URI.create("http://test.opensilex.org/graph-import#Tree");
    private static final URI COMMENT = URI.create("http://www.w3.org/2000/01/rdf-schema#comment");
    private static final URI NOTE = URI.create("http://test.opensilex.org/graph-import#note");

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(LearnedAliasStore.COLLECTION);
    }

    @Test
    public void theMostChosenPropertyWinsForAHeaderAndAClass() {
        LearnedAliasStore store = new LearnedAliasStore(getMongoDBService());
        store.learn(ARNAUD, PLOT, "Remarque", NOTE);
        store.learn(ARNAUD, PLOT, "remarque ", COMMENT);
        store.learn(ARNAUD, PLOT, "REMARQUE", COMMENT);

        assertEquals(Optional.of(COMMENT), store.find(ARNAUD, PLOT, "remarque"));
        assertEquals(Optional.empty(), store.find(ARNAUD, TREE, "remarque"));
    }

    @Test
    public void oneUserCorrectionNeverReachesAnother() {
        LearnedAliasStore store = new LearnedAliasStore(getMongoDBService());
        store.learn(ARNAUD, PLOT, "Remarque", NOTE);

        assertEquals(Optional.empty(), store.find(CAMILLE, PLOT, "remarque"));
    }

    @Test
    public void anEmptyHeaderTeachesNothing() {
        LearnedAliasStore store = new LearnedAliasStore(getMongoDBService());
        store.learn(ARNAUD, PLOT, " - ", NOTE);

        assertEquals(Optional.empty(), store.find(ARNAUD, PLOT, ""));
    }
}
