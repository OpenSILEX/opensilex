//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.apache.jena.riot.Lang;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.DefaultOntologyStore;
import org.opensilex.sparql.ontology.store.OntologyStore;

import java.io.InputStream;
import java.net.URI;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The view of a class read from a real triplestore: a small ontology loaded for the test, and the
 * OpenSILEX vocabulary itself.
 *
 * @author Arnaud Charleroy
 */
public class ClassViewReaderTest extends AbstractMongoIntegrationTest {

    private static final String NS = "http://test.opensilex.org/graph-import#";
    private static final URI FIXTURE_GRAPH = URI.create("http://test.opensilex.org/graph-import/ontology");
    private static final URI PARENT = URI.create(NS + "Parent");
    private static final URI CHILD = URI.create(NS + "Child");
    private static final URI HEIGHT = URI.create(NS + "height");
    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";
    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

    private ClassViewReader reader;

    @Before
    public void loadFixture() throws Exception {
        try (InputStream fixture = getClass().getResourceAsStream("/ontology/class-view-fixture.ttl")) {
            getSparqlService().loadOntology(FIXTURE_GRAPH, fixture, Lang.TURTLE);
        }
        reader = new ClassViewReader(getSparqlService());
    }

    @After
    public void dropFixture() throws Exception {
        getSparqlService().clearGraph(FIXTURE_GRAPH);
    }

    @Test
    public void theClassOwnRestrictionWinsOverItsParents() throws Exception {
        ClassView child = reader.read(CHILD);

        PropertyView height = child.getProperty(HEIGHT);
        assertEquals(new Cardinality(0, 3), height.cardinality());
        assertEquals(Set.of(CHILD), height.declaredOn());
        assertEquals(PropertyKind.DATA, height.kind());
        assertEquals(URI.create(XSD + "decimal"), height.range());
        assertEquals("hauteur", height.labels().in("fr"));
        assertTrue(child.isA(PARENT));
        assertEquals("Enfant", child.getLabels().in("fr"));
    }

    @Test
    public void exactCardinalitiesSetBothBounds() throws Exception {
        ClassView child = reader.read(CHILD);

        PropertyView code = child.getProperty(URI.create(NS + "code"));
        assertEquals(new Cardinality(1, 1), code.cardinality());
        assertEquals(Set.of(PARENT), code.declaredOn());
        assertEquals(new Cardinality(2, 2), child.getProperty(URI.create(NS + "pair")).cardinality());
    }

    @Test
    public void twoRestrictionsOfOneClassOnOnePropertyAddUp() throws Exception {
        assertEquals(new Cardinality(1, 2), reader.read(CHILD).getProperty(URI.create(NS + "note")).cardinality());
    }

    @Test
    public void anUnqualifiedCardinalityTakesItsKindFromThePropertyType() throws Exception {
        PropertyView neighbour = reader.read(CHILD).getProperty(URI.create(NS + "neighbour"));

        assertEquals(PropertyKind.OBJECT, neighbour.kind());
        assertEquals(new Cardinality(1, null), neighbour.cardinality());
        assertNull(neighbour.range());
    }

    @Test
    public void theParentDoesNotSeeItsChildRestrictions() throws Exception {
        ClassView parent = reader.read(PARENT);

        assertEquals(new Cardinality(0, 1), parent.getProperty(HEIGHT).cardinality());
        assertNull(parent.getProperty(URI.create(NS + "pair")));
    }

    @Test(expected = SPARQLInvalidURIException.class)
    public void aUriThatNamesNoClassIsRefused() throws Exception {
        reader.read(URI.create(NS + "Nothing"));
    }

    @Test
    public void theScientificObjectOfTheVocabulary() throws Exception {
        ClassView scientificObject = reader.read(URI.create("vocabulary:ScientificObject"));

        assertEquals(URI.create(Oeso.NS + "ScientificObject"), scientificObject.getUri());
        PropertyView name = scientificObject.getProperty(URI.create(RDFS_LABEL));
        assertEquals(new Cardinality(1, 1), name.cardinality());
        assertTrue(name.cardinality().isRequired());
        PropertyView isPartOf = scientificObject.getProperty(URI.create(Oeso.NS + "isPartOf"));
        assertEquals(PropertyKind.OBJECT, isPartOf.kind());
        assertEquals(URI.create(Oeso.NS + "ScientificObject"), isPartOf.range());
        assertFalse(isPartOf.cardinality().isMultiValued());
        PropertyView factorLevel = scientificObject.getProperty(URI.create(Oeso.NS + "hasFactorLevel"));
        assertTrue(factorLevel.cardinality().isMultiValued());
        assertEquals(URI.create(XSD + "date"),
                scientificObject.getProperty(URI.create(Oeso.NS + "hasCreationDate")).range());
    }

    /**
     * Why the view does not come from the ontology store: on the production store, one request with
     * an ancestor rewrites the class's restriction in the shared cache, and an exact cardinality of 1
     * loses its minimum. If the platform fixes either, this test says so and the reader may go back to
     * the store.
     */
    @Test
    public void theProductionStoreGivesAnotherAnswerAfterARequestWithAnAncestor() throws Exception {
        OntologyStore store = new DefaultOntologyStore(getSparqlService(), getOpensilex());
        store.load();

        assertEquals(Integer.valueOf(3), cachedRestriction(store, HEIGHT).getMaxQualifiedCardinality());
        store.getClassModel(CHILD, PARENT, null);
        assertEquals("the ancestor's restriction now stands for the class's own",
                Integer.valueOf(1), cachedRestriction(store, HEIGHT).getMaxQualifiedCardinality());

        OwlRestrictionModel code = store.getClassModel(PARENT, null, null).getRestrictionsByProperties()
                .get(URI.create(NS + "code"));
        assertFalse("an exact cardinality of 1 is read as optional", code.isRequired());

        assertEquals(new Cardinality(0, 3), reader.read(CHILD).getProperty(HEIGHT).cardinality());
        assertTrue(reader.read(PARENT).getProperty(URI.create(NS + "code")).cardinality().isRequired());
    }

    private static OwlRestrictionModel cachedRestriction(OntologyStore store, URI property) throws Exception {
        ClassModel child = store.getClassModel(CHILD, null, null);
        return child.getRestrictionsByProperties().get(property);
    }
}
