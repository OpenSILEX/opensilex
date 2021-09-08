//******************************************************************************
//                          ObservationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.bson.Document;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.Required;

/**
 */
public class FormDTO {
 
    @JsonProperty("creation_date")
    @ApiModelProperty(value = "timestamp", example = "", required = true)
    private String creationDate;

    private URI type;

    private Map formData;

    @ApiModelProperty(value = "to specify if the offset is not in the date and if the timezone is different from the default one")
    protected String timezone;

    public Map getFormData() {
        return formData;
    }

    public void setFormData(Map formData) {
        this.formData = formData;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public FormModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        FormModel model = new FormModel();
        model.setType(type);
        model.setFormData(new Document(formData));
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(getCreationDate(), getTimezone());

        if (parsedDateTimeMongo == null) {
            throw new UnableToParseDateException(getCreationDate());
        } else {
            model.setCreationDate(parsedDateTimeMongo.getInstant());
            model.setLastUpdateDate(parsedDateTimeMongo.getInstant());
            model.setOffset(parsedDateTimeMongo.getOffset());
        }
        return model;

    }
}
