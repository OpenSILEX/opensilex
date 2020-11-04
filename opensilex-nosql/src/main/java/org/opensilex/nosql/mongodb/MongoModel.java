/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.mongodb;

import java.net.URI;
import org.opensilex.nosql.utils.ClassURIGenerator;

/**
 *
 * @author boizetal
 */
public class MongoModel implements ClassURIGenerator<MongoModel> {
    protected URI baseURI; 
    protected URI uri;    

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    @Override
    public String[] getUriSegments(MongoModel instance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
