//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SOSA;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The ontology property each role of a column stands for, where it has one: the single table the
 * exchange formats read to say what a column means. A role with no entry says nothing about the
 * ontology, and the formats write nothing for it.
 * <p>
 * Properties are those of the platform's own vocabularies, never a string written here: a link that
 * does not exist is a compile error, not a template that cannot be understood.
 *
 * @author Arnaud Charleroy
 */
public final class RoleOntology {

    private static final Map<DescribedRole, Property> PROPERTIES = new EnumMap<>(DescribedRole.class);

    static {
        PROPERTIES.put(DescribedRole.OBJECT, RDFS.label);
        PROPERTIES.put(DescribedRole.GERMPLASM, Oeso.hasGermplasm);
        PROPERTIES.put(DescribedRole.FACTOR_LEVEL, Oeso.hasFactorLevel);
        PROPERTIES.put(DescribedRole.PARENT_OBJECT, Oeso.isPartOf);
        PROPERTIES.put(DescribedRole.LOCATION, Oeso.isHosted);
        PROPERTIES.put(DescribedRole.VARIABLE, SOSA.observedProperty);
    }

    private RoleOntology() {
    }

    /**
     * @return the URI of the property the role stands for, or empty when it has none
     */
    public static Optional<String> predicate(DescribedRole role) {
        return Optional.ofNullable(PROPERTIES.get(role)).map(Property::getURI);
    }

    /**
     * @return every link of the table, for the checks that each exists in a vocabulary
     */
    public static Map<DescribedRole, Property> all() {
        return Map.copyOf(PROPERTIES);
    }

    /**
     * @return the class an observation is an instance of
     */
    public static String observationClass() {
        return SOSA.Observation.getURI();
    }

    /**
     * @return the property that links an observation to the thing it concerns
     */
    public static String featureOfInterest() {
        return SOSA.hasFeatureOfInterest.getURI();
    }
}
