//******************************************************************************
//                          DocumentCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.document.api;

import org.opensilex.core.document.dal.DocumentModel;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

/**
 * @author Emilie Fernandez
 */
public class DocumentCreationDTO extends DocumentDTO {
  
    public DocumentModel newModel() {

        DocumentModel model = new DocumentModel();
        model.setUri(getUri());
        model.setIdentifier(getIdentifier());
        model.setType(getType());
        model.setTitle(getTitle());
        model.setDate(getDate());
        model.setTargets(getTargets());
        model.setAuthors(getAuthors());
        model.setLanguage(getLanguage());
        model.setFormat(getFormat());
        model.setDescription(getDescription());
        model.setSubject(getSubject());
        model.setDeprecated(Boolean.toString(getDeprecated()));
        model.setSource(getSource());
        return model;
    }

     /**
     * Method to unserialize DocumentCreationDTO.
     * Required because this data is received as @FormDataParam.
     * @param param
     * @return
     * @throws IOException 
     */
    public static DocumentCreationDTO fromString(String param) throws IOException {
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        return mapper.readValue(param, DocumentCreationDTO.class);
    }


}
