//******************************************************************************
//                          SectionUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;

import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.mobile.dal.SectionModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Maximilian Hart
 */
public class SectionUpdateDTO extends SectionCreationDTO {
    
    private URI uri;

    private String updateDate;

    @NotNull
    @ValidURI
    @ApiModelProperty(value = "URI of the form being updated", required = true) 
    public URI getUri() {
        return uri;
    }

    @JsonProperty("created_date")
    @ApiModelProperty(value = "timestamp", example = "YYYY-MM-DDTHH:MM:SSZ", required = true)
    @Date(DateFormat.YMDTHMSX)
    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }
    
    @Override
    public SectionModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        SectionModel model = super.newModel();
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(getUpdateDate(), getTimezone());

        if (parsedDateTimeMongo == null) {
            throw new UnableToParseDateException(getUpdateDate());
        } else {
            model.setLastUpdateDate(parsedDateTimeMongo.getInstant());
            model.setOffset(parsedDateTimeMongo.getOffset());
        }
        model.setUri(uri);
        return model;
    }
    
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    
}
