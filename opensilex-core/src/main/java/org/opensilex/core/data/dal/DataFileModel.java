//******************************************************************************
//                          DataFileModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import java.net.URI;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * DataFileModel
 * @author Alice Boizet
 */

public class DataFileModel extends DataModel {
    
    URI rdfType;
    String filename;
    String path;

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    @Override
    public String[] getUriSegments(MongoModel instance) {
        return new String[]{            
            String.valueOf(getDate().getEpochSecond()),
            String.valueOf(getProvenance().getUri().getFragment()),
            String.valueOf(System.nanoTime())
        };
    } 
        
}
