//******************************************************************************
//                                   Ask.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 30 Jan. 2018
// Contact: eloan.lagire@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * Ask model.
 * @author Eloan LAGIER
 */
public class Ask {

    Boolean exist;

    @ApiModelProperty(example = "true")
    public Boolean getExist() {
        return exist;
    }

    public void setExist(Boolean exist) {
        this.exist = exist;
    }
}
