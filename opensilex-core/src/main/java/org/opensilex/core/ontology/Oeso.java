//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.ontology;

import org.apache.jena.rdf.model.*;
import org.opensilex.sparql.utils.*;

/**
 *
 * @author vincent
 */
public class Oeso {

    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/oeso";

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
     * The namespace of the vocabulary as a resource
     */
    public static final Resource NAMESPACE = Ontology.resource(NS);

    public static final Property hasShortname = Ontology.property(NS, "hasShortname");
    public static final Property hasRelatedProject = Ontology.property(NS, "hasShortname");
    public static final Property hasFinancialSupport = Ontology.property(NS, "hasShortname");
    public static final Property hasFinancialReference = Ontology.property(NS, "hasShortname");
    public static final Property hasStartDate = Ontology.property(NS, "hasShortname");
    public static final Property hasEndDate = Ontology.property(NS, "hasShortname");
    public static final Property hasKeyword = Ontology.property(NS, "hasShortname");
    public static final Property hasObjective = Ontology.property(NS, "hasObjective");

}
