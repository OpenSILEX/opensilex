//******************************************************************************
//                      SecurityOntology.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.utils.Ontology;

/**
 * <pre>
 * Security ontology Resource and Property definitions for concepts:
 * - Users
 * - Groups
 * - Profiles
 * </pre>
 *
 * @author Vincent Migot
 */
public class SecurityOntology {

    public static final String NAMESPACE = SPARQLModule.ONTOLOGY_BASE_DOMAIN + "security#";

    public static final String PREFIX = "os-sec";

    public static final Resource Group = Ontology.resource(NAMESPACE, "Group");
    public static final Resource Profile = Ontology.resource(NAMESPACE, "Profile");
    public static final Resource GroupUserProfile = Ontology.resource(NAMESPACE, "GroupUserProfile");

    public static final Property hasPasswordHash = Ontology.property(NAMESPACE, "hasPasswordHash");
    public static final Property hasLanguage = Ontology.property(NAMESPACE, "hasLanguage");
    public static final Property hasUser = Ontology.property(NAMESPACE, "hasUser");
    public static final Property hasGroup = Ontology.property(NAMESPACE, "hasGroup");
    public static final Property hasProfile = Ontology.property(NAMESPACE, "hasProfile");
    public static final Property hasUserProfile = Ontology.property(NAMESPACE, "hasUserProfile");
    public static final Property hasAccess = Ontology.property(NAMESPACE, "hasAccess");
    public static final Property isAdmin = Ontology.property(NAMESPACE, "isAdmin");
    public static final Property hasCredential = Ontology.property(NAMESPACE, "hasCredential");

    public static final Property isEnabled = Ontology.property(NAMESPACE, "isEnabled");

}
