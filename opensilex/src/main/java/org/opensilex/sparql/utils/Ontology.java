//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import java.net.*;
import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.path.*;
import org.apache.jena.vocabulary.*;

/**
 *
 * @author vincent
 */
public abstract class Ontology {

    public static final Path subClassAny;
    static {
        subClassAny = new P_ZeroOrMore1(new P_Link(RDFS.subClassOf.asNode()));
    }
    
    public static final Resource resource(String uri) {
        return ResourceFactory.createResource(uri);
    }

    public static final Resource resource(String namespace, String local) {
        return ResourceFactory.createResource(namespace + local);
    }

    public static final Property property(String uri) {
        return ResourceFactory.createProperty(uri);
    }

    public static final Property property(String namespace, String local) {
        return ResourceFactory.createProperty(namespace, local);
    }

    public static final Node nodeURI(URI uri) {
        return nodeURI(uri.toString());
    }

    public static final Node nodeURI(String uri) {
        return NodeFactory.createURI(uri);
    }
}
