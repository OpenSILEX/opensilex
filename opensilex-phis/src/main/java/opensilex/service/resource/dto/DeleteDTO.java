//******************************************************************************
//                                AnnotationDeleteDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 25 oct. 2019
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************


package opensilex.service.resource.dto;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;

/**
 * @author renaud.colin@inra.fr
 *
 */
@ApiModel
public class DeleteDTO extends AbstractVerifiedClass {
	
	/**
	 * Uri(s) of objects to delete 
	 * @example http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2
	 */
	private List<String> uris;

	@Override
	public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("No object could be create from a DeleteDTO"); 
	}
	
	@URL
	@NotNull
    @NotEmpty
    @ApiModelProperty(notes = "Need to be a list of URI")
    public List<String> getUris() {
        return uris;
    }
	
    public void setUris(List<String> uris) {
        this.uris = uris;
    }

}
