//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.utils;

import java.net.URI;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author vincent
 */
public abstract class Ontology {

    public static final Path subClassAny;
    
    public static final Path subClassStrict;
    
    public static final Path subPropertyAny;

    public static final Resource SPARQLResourceModel = resource("https://www.opensilex.org/abstract#SPARQLResourceModel");

    
    static {
        subClassAny = new P_ZeroOrMore1(new P_Link(RDFS.subClassOf.asNode()));
        subClassStrict = new P_OneOrMore1(new P_Link(RDFS.subClassOf.asNode()));
        subPropertyAny = new P_ZeroOrMore1(new P_Link(RDFS.subPropertyOf.asNode()));
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

    public static final Property property(URI uri) {
        return ResourceFactory.createProperty(uri.toString());
    }

    public static final Property property(String namespace, String local) {
        return ResourceFactory.createProperty(namespace, local);
    }
}
