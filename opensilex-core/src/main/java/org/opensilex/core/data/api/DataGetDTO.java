//******************************************************************************
//                          DataGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.validation.constraints.NotNull;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
public class DataGetDTO extends DataCreationDTO {
    @NotNull
    @ValidURI
    @Override
    public URI getUri() {
        return uri;
    }    
        
    public void setDate(ZonedDateTime date){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXX");
        this.setDate(dtf.format(date));
    }
    
    public static DataGetDTO fromModel(DataModel model){
        DataGetDTO dto = new DataGetDTO();        
        dto.setUri(model.getUri());
        dto.setScientificObjects(model.getScientificObjects());
        dto.setVariable(model.getVariable());      
        dto.setDate(model.getDate());        
        dto.setConfidence(model.getConfidence());
        dto.setValue(model.getValue());
        dto.setMetadata(model.getMetadata());   
        dto.setProvenance(model.getProvenance());
        
        return dto;
    }
}
