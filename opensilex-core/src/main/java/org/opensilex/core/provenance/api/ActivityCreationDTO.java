//******************************************************************************
//                          ActivityCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.provenance.dal.ActivityModel;
import org.opensilex.server.rest.validation.Required;

import java.net.URI;

/**
 * Activity ActivityCreationDTO
 * @author Alice Boizet
 */
@JsonPropertyOrder({"rdf_type", "uri", "start_date","end_date", "timezone", "settings"})
public class ActivityCreationDTO extends ActivityGetDTO {   
    @ApiModelProperty(value = "to specify if the offset is not in the dates and if the timezone is different from the default one")
    String timezone;
    
    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    @Required
    public URI getRdfType() {
        return rdfType;
    }

    public ActivityModel newModel() throws UnableToParseDateException, TimezoneAmbiguityException, TimezoneException {
        ActivityModel model = new ActivityModel();
        
        model.setRdfType(rdfType);
        model.setUri(uri);
        model.setSettings(settings);
        
        
        if (startDate != null) {
            ParsedDateTimeMongo parsedDateTime = DataValidateUtils.setDataDateInfo(startDate, timezone);
            model.setStartDate(parsedDateTime.getInstant());
            model.setOffset(parsedDateTime.getOffset());
        }
        
        if (endDate != null) {
            ParsedDateTimeMongo parsedDateTime = DataValidateUtils.setDataDateInfo(endDate, timezone);
            model.setEndDate(parsedDateTime.getInstant());
            model.setOffset(parsedDateTime.getOffset());
        }
        
        return model;
    }
    
}
