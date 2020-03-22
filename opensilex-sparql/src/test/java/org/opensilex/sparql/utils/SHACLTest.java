/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.utils;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.jena.riot.Lang;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.unit.test.AbstractUnitTest;
import org.opensilex.sparql.model.A;
import org.opensilex.sparql.model.B;
import org.opensilex.sparql.model.C;
import org.opensilex.sparql.model.TEST_ONTOLOGY;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vince
 */
public abstract class SHACLTest extends AbstractUnitTest {

    protected static SPARQLService service;

    protected static URI shaclURI;

    public static void initialize(SPARQLService service) throws Exception {
        shaclURI = new URI(RDF4J.SHACL_SHAPE_GRAPH.toString());
        SHACLTest.service = service;

        service.clear();
        
        InputStream ontology = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.FILE_PATH.toString());
        service.loadOntology(SPARQLModule.getPlatformURI(), ontology, TEST_ONTOLOGY.FILE_FORMAT);
    }

    @AfterClass
    public static void destroy() throws Exception {
        service.clearGraph(SPARQLModule.getPlatformURI());
        service.shutdown();
    }

    @Test
    public void testSHACLGeneration() throws Exception {
        InputStream ontologyData = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.DATA_FILE_PATH.toString());
        service.loadOntology(SPARQLModule.getPlatformDomainGraphURI("data"), ontologyData, TEST_ONTOLOGY.DATA_FILE_FORMAT);

        ontologyData = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.SHACL_FAIL_FILE_PATH.toString());
        try {
            service.loadOntology(SPARQLModule.getPlatformDomainGraphURI("data"), ontologyData, TEST_ONTOLOGY.SHACL_FAIL_FILE_FORMAT);
            throw new Exception("This ontology should fail to validate with SHACL");
        } catch (SPARQLValidationException ex) {
            Map<URI, Map<URI, List<URI>>> errors = ex.getValidationErrors();
            URI aURI = new URI("http://test.opensilex.org/a/002");
            URI bURI = new URI("http://test.opensilex.org/b/002");
            URI cURI = new URI("http://test.opensilex.org/c/002");
            assertTrue(errors.containsKey(aURI));
            assertTrue(errors.containsKey(bURI));
            assertTrue(errors.containsKey(cURI));

            URI datatypeConstraintURI = new URI(SHACL.DatatypeConstraintComponent.getURI());
            URI minCountURI = new URI(SHACL.MinCountConstraintComponent.getURI());
            URI maxCountURI = new URI(SHACL.MaxCountConstraintComponent.getURI());
            URI classConstraintURI = new URI(SHACL.ClassConstraintComponent.getURI());
            URI uniqueLangConstraintURI = new URI(SHACL.UniqueLangConstraintComponent.getURI());

            Map<URI, List<URI>> aErrors = errors.get(aURI);

            URI propertyURI = new URI(TEST_ONTOLOGY.hasString.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            List<URI> brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(datatypeConstraintURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasChar.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(datatypeConstraintURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasBoolean.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(datatypeConstraintURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasDate.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(datatypeConstraintURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasDateTime.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(datatypeConstraintURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasRelationToB.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(classConstraintURI, brokenConstraints.get(0));

            Map<URI, List<URI>> bErrors = errors.get(bURI);

            propertyURI = new URI(TEST_ONTOLOGY.hasChar.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(minCountURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasDouble.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(minCountURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasFloat.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(minCountURI, brokenConstraints.get(0));

            propertyURI = new URI(TEST_ONTOLOGY.hasShort.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(2, brokenConstraints.size());
            assertTrue(brokenConstraints.contains(maxCountURI));
            assertTrue(brokenConstraints.contains(datatypeConstraintURI));

            Map<URI, List<URI>> cErrors = errors.get(cURI);
            propertyURI = new URI(TEST_ONTOLOGY.hasLabel.getURI());
            assertTrue(cErrors.containsKey(propertyURI));
            brokenConstraints = cErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertEquals(uniqueLangConstraintURI, brokenConstraints.get(0));

        }
    }

}
