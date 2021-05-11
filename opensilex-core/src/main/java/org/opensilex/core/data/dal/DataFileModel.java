//******************************************************************************
//                          DataFileModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.apache.commons.codec.digest.DigestUtils;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

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
    ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        String provenanceString = "";
        try {
            provenanceString = mapper.writeValueAsString(getProvenance());
        } catch (JsonProcessingException ex) {            
        }
        
        String objectsString = "";
        if (getScientificObject() != null) {
            objectsString = getScientificObject().toString();
        }
        
        String md5Hash = DigestUtils.md5Hex(objectsString + provenanceString);
        
        return new String[]{            
            String.valueOf(getDate().getEpochSecond()),
            md5Hash
        };
    } 
        
}
