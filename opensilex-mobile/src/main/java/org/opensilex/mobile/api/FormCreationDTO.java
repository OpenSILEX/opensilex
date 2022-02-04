//******************************************************************************
//                          FormCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.bson.Document;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author Maximilian Hart
 */
public class FormCreationDTO {
 
  
    private String creationDate;

    private URI type;

    private List<Map> formData;

    private String timezone;

    private String commitAddress;

    private String name;

    @JsonProperty("form_data")
    public List<Map> getFormData() {
        return formData;
    }

    public void setFormData(List<Map> formData) {
        this.formData = formData;
    }
    
    @JsonProperty("modified_date")
    @ApiModelProperty(value = "timestamp", example = "YYYY-MM-DDTHH:MM:SSZ", required = true)
    @Date(DateFormat.YMDTHMSX)
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @JsonProperty("commit_address")
    @NotNull
    @ApiModelProperty(value = "address of the commit", required = true)
    public String getCommitAddress(){
        return commitAddress;
    }

    @JsonProperty("name")
    @NotNull
    @ApiModelProperty(value = "code lot of the form", required = true)
    public String getName(){
        return name;
    }

    public void setName(String s){
        this.name = s;
    }

    public void setCommitAddress(String s){
        this.commitAddress = s;
    }

    @ApiModelProperty(value = "to specify if the offset is not in the date and if the timezone is different from the default one")
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
        model.setCommitAddress(commitAddress);
        model.setName(name);
        model.setType(type);
        model.setFormData(formData);
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
