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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.unit.test.AbstractUnitTest;
import org.opensilex.sparql.model.TEST_ONTOLOGY;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vince
 */
public abstract class SHACLTest extends AbstractUnitTest {

    protected static SPARQLService sparql;

    protected static SPARQLModule sparqlModule;

    public static void initialize() throws Exception {
        sparqlModule = opensilex.getModuleByClass(SPARQLModule.class);

        InputStream ontology = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.FILE_PATH.toString());
        sparql.loadOntology(sparqlModule.getBaseURI(), ontology, TEST_ONTOLOGY.FILE_FORMAT);
    }

    @Test
    public void testSHACLGeneration() throws Exception {
        sparql.enableSHACL();

        InputStream ontologyData = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.DATA_FILE_PATH.toString());
        sparql.loadOntology(sparqlModule.getSuffixedURI("data"), ontologyData, TEST_ONTOLOGY.DATA_FILE_FORMAT);

        ontologyData = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.SHACL_FAIL_FILE_PATH.toString());
        try {
            sparql.loadOntology(sparqlModule.getSuffixedURI("data"), ontologyData, TEST_ONTOLOGY.SHACL_FAIL_FILE_FORMAT);
            throw new Exception("This ontology should fail to validate with SHACL");
        } catch (SPARQLValidationException ex) {
            Map<URI, Map<URI, Map<URI, String>>> errors = ex.getValidationErrors();
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

            Map<URI, Map<URI, String>> aErrors = errors.get(aURI);

            URI propertyURI = new URI(TEST_ONTOLOGY.hasString.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            Map<URI, String> brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(datatypeConstraintURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasChar.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(datatypeConstraintURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasBoolean.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(datatypeConstraintURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasDate.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(datatypeConstraintURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasDateTime.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(datatypeConstraintURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasRelationToB.getURI());
            assertTrue(aErrors.containsKey(propertyURI));
            brokenConstraints = aErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(classConstraintURI));

            Map<URI, Map<URI, String>> bErrors = errors.get(bURI);

            propertyURI = new URI(TEST_ONTOLOGY.hasChar.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(minCountURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasDouble.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(minCountURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasFloat.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(minCountURI));

            propertyURI = new URI(TEST_ONTOLOGY.hasShort.getURI());
            assertTrue(bErrors.containsKey(propertyURI));
            brokenConstraints = bErrors.get(propertyURI);
            assertEquals(2, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(maxCountURI));
            assertTrue(brokenConstraints.containsKey(datatypeConstraintURI));

            Map<URI, Map<URI, String>> cErrors = errors.get(cURI);
            propertyURI = new URI(TEST_ONTOLOGY.hasLabel.getURI());
            assertTrue(cErrors.containsKey(propertyURI));
            brokenConstraints = cErrors.get(propertyURI);
            assertEquals(1, brokenConstraints.size());
            assertTrue(brokenConstraints.containsKey(uniqueLangConstraintURI));

        }
    }

}
