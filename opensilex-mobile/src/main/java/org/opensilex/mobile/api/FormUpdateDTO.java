//******************************************************************************
//                          FormUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Maximilian Hart
 */
public class FormUpdateDTO extends FormCreationDTO{
    
    protected URI uri;

    protected String commitAddress;
    
    @NotNull
    @ValidURI
    @ApiModelProperty(value = "URI of the form being updated", required = true) 
    public URI getUri() {
        return uri;
    }

    @JsonProperty("commit_address")
    @NotNull
    @ApiModelProperty(value = "address of the commit", required = true)
    public String getCommitAddress(){
        return commitAddress;
    }
    
    @Override
    public FormModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        FormModel model = super.newModel();
        model.setUri(uri);
        model.setCommitAddress(commitAddress);
        return model;
    }
    
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public void setCommitAddress(String s){
        this.commitAddress = s;
    }
    
}
