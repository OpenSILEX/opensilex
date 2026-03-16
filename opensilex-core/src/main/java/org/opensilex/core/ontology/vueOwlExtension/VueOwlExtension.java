/*
 * *****************************************************************************
 *                         VueOwlExtension.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 11/03/2026 16:06
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.vueOwlExtension;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author vince
 */
public class VueOwlExtension {

    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/owl-vue-extension";

    public static final String PREFIX = "oeso-owl";

    /**
     * The namespace of the vocabulary as a string
     */
    public static final String NS = DOMAIN + "#";

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
     * Vocabulary namespace
     */
    public static final Resource NAMESPACE = Ontology.resource(NS);

    public static final Resource ClassExtension = Ontology.resource(NS, "ClassExtension");

    public static final Property isAbstractClass = Ontology.property(NS, "isAbstractClass");
    public static final Property hasIcon = Ontology.property(NS, "hasIcon");
    
    public static final Resource ClassPropertyExtension = Ontology.resource(NS, "ClassPropertyExtension");
    public static final Property fromOwlClass = Ontology.property(NS, "fromOwlClass");
    public static final Property toOwlProperty = Ontology.property(NS, "toOwlProperty");
    public static final Property hasInputComponent = Ontology.property(NS, "hasInputComponent");
    public static final Property hasViewComponent = Ontology.property(NS, "hasViewComponent");
    public static final Property hasDisplayOrder = Ontology.property(NS, "hasDisplayOrder");

}
