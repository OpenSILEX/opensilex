/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.opensilex.module.core.service.sparql.model;

import java.net.URI;
import org.opensilex.module.core.service.sparql.annotations.SPARQLProperty;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResource;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResourceURI;

/**
 *
 * @author vincent
 */
@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "B"
)
public class B {
    
    @SPARQLResourceURI()
    private URI uri;
    
    @SPARQLProperty(
        ontology = TEST_ONTOLOGY.class,
        property = "hasALinkToB",
        inverse = true
    )
    private A a;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }
    
    
}
