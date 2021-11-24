//******************************************************************************
//                          FormUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

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

    
    @NotNull
    @ValidURI
    @ApiModelProperty(value = "URI of the form being updated", required = true) 
    public URI getUri() {
        return uri;
    }
    
    @Override
    public FormModel newModel() throws TimezoneAmbiguityException, TimezoneException, UnableToParseDateException {
        FormModel model = super.newModel();
        model.setUri(uri);
        return model;
    }
    
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    
}
