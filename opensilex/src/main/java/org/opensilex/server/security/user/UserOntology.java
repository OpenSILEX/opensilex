/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security.user;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author Vincent Migot
 */
public class UserOntology {
    
    public static final String DOMAIN = "http://www.opensilex.org/users";
            
    public static final String GRAPH_USER = DOMAIN + "/data/";
    
    public static final String NS = DOMAIN + "#";

    public static String getURI() {return NS;}

    public static final Resource NAMESPACE = Ontology.resource(NS);
    
    public static final Resource User = Ontology.resource(NS, "User");

    public static final Property hasGroup = Ontology.property(NS, "hasGroup");
    
    public static final Property hasPasswordHash = Ontology.property(NS, "hasPasswordHash");

}
