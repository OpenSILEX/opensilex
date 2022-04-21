//******************************************************************************
//                          DataFileCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;

/**
 *
 * @author Alice Boizet
 */
public class DataFilePathCreationDTO extends DataFileCreationDTO {
    @NotNull
    @JsonProperty("relative_path")
    @ApiModelProperty(value = "path to the stored file", example = DataFilesAPI.DATAFILE_EXAMPLE_URI)
    private String relativePath;

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }    
    
    @Override
    public DataFileModel newModel() throws UnableToParseDateException, TimezoneAmbiguityException, TimezoneException {
        DataFileModel model = new DataFileModel();
        
        model.setMetadata(getMetadata());
        model.setProvenance(getProvenance());
        model.setRdfType(getRdfType());
        model.setTarget(getTarget());
        model.setUri(getUri());
        model.setArchive(getArchive());
        model.setPath(relativePath);        
        ParsedDateTimeMongo parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(getDate(), getTimezone());
        model.setDate(parsedDateTimeMongo.getInstant());
        model.setOffset(parsedDateTimeMongo.getOffset());
        model.setIsDateTime(parsedDateTimeMongo.getIsDateTime());
        
        return model;
    }
    
        
}
