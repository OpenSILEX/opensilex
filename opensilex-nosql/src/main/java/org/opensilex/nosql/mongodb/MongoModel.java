//******************************************************************************
//                          MongoModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;

/**
 *
 * @author Alice Boizet
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
