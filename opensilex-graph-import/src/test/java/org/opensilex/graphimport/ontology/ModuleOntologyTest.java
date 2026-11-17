//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.ontology.store.DefaultOntologyStore;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The module's ontology is loaded once into its own graph, loaded again when the graph is emptied, and
 * gives the class views the factor restrictions oeso-core does not declare.
 *
 * @author Arnaud Charleroy
 */
public class ModuleOntologyTest extends AbstractMongoIntegrationTest {

    private static final URI FACTOR = URI.create(Oeso.Factor.getURI());
    private static final URI FACTOR_LEVEL = URI.create(Oeso.FactorLevel.getURI());
    private static final URI FACTOR_CATEGORY = URI.create(Oeso.FactorCategory.getURI());
    private static final URI HAS_FACTOR = URI.create(Oeso.hasFactor.getURI());
    private static final URI HAS_CATEGORY = URI.create(Oeso.hasCategory.getURI());

    @Before
    public void emptyTheGraph() throws Exception {
        getSparqlService().clearGraph(ModuleOntology.GRAPH);
    }

    @Test
    public void loadingTwiceWritesTheFileOnce() throws Exception {
        assertFalse(ModuleOntology.isLoaded(getSparqlService()));

        ModuleOntology.ensureLoaded(getSparqlService());
        long once = triples();
        ModuleOntology.ensureLoaded(getSparqlService());

        assertTrue(ModuleOntology.isLoaded(getSparqlService()));
        assertTrue(once > 0);
        assertEquals(once, triples());
    }

    /**
     * The platform's ontology store reads the restrictions of every graph when the platform starts, and
     * refuses one on a property no graph declares: the module's file must not stop the platform starting.
     */
    @Test
    public void thePlatformStillStartsWithTheModuleOntology() throws Exception {
        ModuleOntology.ensureLoaded(getSparqlService());

        new DefaultOntologyStore(getSparqlService(), getOpensilex()).load();
    }

    @Test
    public void aLevelBelongsToExactlyOneFactor() throws Exception {
        ModuleOntology.ensureLoaded(getSparqlService());

        PropertyView hasFactor = new ClassViewReader(getSparqlService()).read(FACTOR_LEVEL).getProperty(HAS_FACTOR);

        assertNotNull(hasFactor);
        assertEquals(PropertyKind.OBJECT, hasFactor.kind());
        assertEquals(FACTOR, hasFactor.range());
        assertTrue(hasFactor.cardinality().isRequired());
        assertFalse(hasFactor.cardinality().isMultiValued());
        assertEquals("a pour facteur", hasFactor.labels().in("fr"));
        assertEquals("has Factor", hasFactor.labels().in("en"));
    }

    @Test
    public void aFactorHasAtMostOneCategory() throws Exception {
        ModuleOntology.ensureLoaded(getSparqlService());

        PropertyView hasCategory = new ClassViewReader(getSparqlService()).read(FACTOR).getProperty(HAS_CATEGORY);

        assertNotNull(hasCategory);
        assertEquals(FACTOR_CATEGORY, hasCategory.range());
        assertFalse(hasCategory.cardinality().isRequired());
        assertFalse(hasCategory.cardinality().isMultiValued());
    }

    private long triples() throws Exception {
        SelectBuilder select = new SelectBuilder().addVar("?s")
                .addGraph(NodeFactory.createURI(ModuleOntology.GRAPH.toString()), "?s", "?p", "?o");
        return getSparqlService().executeSelectQueryAsStream(select).count();
    }
}
