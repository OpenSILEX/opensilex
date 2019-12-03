//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author Vincent Migot
 */
public class SecurityOntology {

    public static final String DOMAIN = "http://www.opensilex.org/users";

    public static final String NS = DOMAIN + "#";

    public static String getURI() {
        return NS;
    }

    public static final Resource NAMESPACE = Ontology.resource(NS);

    public static final Resource Group = Ontology.resource(NS, "Group");
    public static final Resource SecurityProfile = Ontology.resource(NS, "GroupProfile");

    public static final Property hasPasswordHash = Ontology.property(NS, "hasPasswordHash");
    public static final Property hasUser = Ontology.property(NS, "hasUser");
    public static final Property hasProfile = Ontology.property(NS, "hasProfile");
    public static final Property hasAccess = Ontology.property(NS, "hasAccess");
    public static final Property isAdmin = Ontology.property(NS, "isAdmin");

}
