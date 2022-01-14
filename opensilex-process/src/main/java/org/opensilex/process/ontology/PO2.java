//******************************************************************************
// LBE - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: emilie.fernandez@inrae.fr, virginie.rossard@inrae.fr, eric.latrille@inrae.fr
//******************************************************************************
package org.opensilex.process.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;
/**
 * @author Emilie Fernandez
 */
public class PO2 {
    public static final String DOMAIN = "http://opendata.inra.fr/PO2";
    public static final String PREFIX = "PO2";
    /**
     * The namespace of the vocabulary as a string
     */
    public static final String NS = DOMAIN + "/";
    /**
     * The namespace of the vocabulary as a string
     *
     * @return namespace as String
     * @see #NS
     */
    public static String getURI() {
        return NS;
    }
    /**
     * namespace
     */
    public static final Resource NAMESPACE = Ontology.resource(NS);
    public static final Property longString = Ontology.property(NS, "longString");

    // ---- PROCESS ----
    public static final Resource Transformation_Process = Ontology.resource(NS, "Transformation_Process");

    // ---- Step ----
    public static final Resource step = Ontology.resource(NS, "step");
    public static final Property hasForStep = Ontology.property(NS, "hasForStep");
    public static final Property hasInput = Ontology.property(NS, "hasInput");
    public static final Property hasOutput = Ontology.property(NS, "hasOutput");
    public static final Property isComposedOf = Ontology.property(NS, "isComposedOf");

}