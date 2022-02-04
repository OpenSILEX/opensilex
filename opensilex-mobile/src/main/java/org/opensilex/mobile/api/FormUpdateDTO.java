package org.opensilex.mobile.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class FormUpdateDTO extends FormCreationDTO{
    private URI uri;

    private String updateDate;


    @NotNull
    @ValidURI
    @ApiModelProperty(value = "URI of the form being updated", required = true)
    public URI getUri() {
        return uri;
    }

    @JsonProperty("updated_date")
    @ApiModelProperty(value = "timestamp", example = "YYYY-MM-DDTHH:MM:SSZ", required = true)
    @Date(DateFormat.YMDTHMSX)
    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    @Override
    public FormModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        FormModel model = super.newModel();
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(getUpdateDate(), getOffset());

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
