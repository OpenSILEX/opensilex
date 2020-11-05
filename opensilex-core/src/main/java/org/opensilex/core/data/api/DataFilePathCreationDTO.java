//******************************************************************************
//                          DataFileCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.validation.constraints.NotNull;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author Alice Boizet
 */
public class DataFilePathCreationDTO extends DataFileCreationDTO {
    @NotNull
    private String relativePath;

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    
    
    @Override
    public DataFileModel newModel() throws ParseException {
        DataFileModel model = new DataFileModel();
        
        model.setMetadata(getMetadata());
        model.setProvenance(getProvenance());
        model.setRdfType(getRdfType());
        model.setScientificObjects(getScientificObjects());
        model.setUri(getUri());
        model.setPath(relativePath);
        
        if(getDate() != null){
            DateFormat[] formats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ};
            ZonedDateTime zdt = null;
            for (DateFormat dateCheckFormat : formats) {
                try { 
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                    zdt = ZonedDateTime.parse(getDate(), dtf);
                    break;
                } catch (DateTimeParseException e) {
                }                    
            }
            model.setDate(zdt);
            model.setTimezone(zdt.getZone().toString());
        }
        return model;
    }
    
        
}
