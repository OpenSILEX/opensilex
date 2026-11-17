//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SOSA;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Every ontology link of the table of roles exists in a vocabulary of the platform, and a role with
 * none says so.
 *
 * @author Arnaud Charleroy
 */
public class RoleOntologyTest {

    private static Set<String> vocabulary(Class<?>... vocabularies) throws IllegalAccessException {
        Set<String> uris = new HashSet<>();
        for (Class<?> vocabulary : vocabularies) {
            for (Field field : vocabulary.getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = field.get(null);
                if (value instanceof Property) {
                    uris.add(((Property) value).getURI());
                } else if (value instanceof Resource) {
                    uris.add(((Resource) value).getURI());
                }
            }
        }
        return uris;
    }

    @Test
    public void everyLinkOfTheTableExistsInAVocabularyOfThePlatform() throws Exception {
        Set<String> known = vocabulary(Oeso.class, SOSA.class, RDFS.class);

        RoleOntology.all().forEach((role, property) ->
                assertTrue(role + " -> " + property.getURI(), known.contains(property.getURI())));
        assertTrue(known.contains(RoleOntology.observationClass()));
        assertTrue(known.contains(RoleOntology.featureOfInterest()));
    }

    @Test
    public void aRoleWithoutALinkSaysNothingAboutTheOntology() {
        assertFalse(RoleOntology.predicate(DescribedRole.COMMENT).isPresent());
        assertFalse(RoleOntology.predicate(DescribedRole.UNREAD).isPresent());
        assertEquals("http://www.opensilex.org/vocabulary/oeso#hasFactorLevel",
                RoleOntology.predicate(DescribedRole.FACTOR_LEVEL).orElseThrow());
    }
}
