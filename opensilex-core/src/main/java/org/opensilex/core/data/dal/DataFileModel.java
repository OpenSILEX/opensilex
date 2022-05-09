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
    
    private URI rdfType;
    private String filename;
    private String path;
    private URI archive;

    public static final String RDF_TYPE_FIELD = "rdfType";

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

    public URI getArchive() {
        return archive;
    }

    public void setArchive(URI archive) {
        this.archive = archive;
    }
    
    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
    ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        String provenanceString = "";
        try {
            provenanceString = mapper.writeValueAsString(getProvenance());
        } catch (JsonProcessingException ex) {            
        }
        
        String objectsString = "";
        if (getTarget() != null) {
            objectsString = getTarget().toString();
        }
        
        String md5Hash = DigestUtils.md5Hex(objectsString + provenanceString);
        
        return new String[]{            
            String.valueOf(getDate().getEpochSecond()),
            md5Hash
        };
    }

}
