//**********************************************************************************************
//                                       Ask.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Janvier 30 2018
// Contact: eloan.lagire@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier 31, 2018
// Subject: Ask model for SPARQL Ask Response
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import io.swagger.annotations.ApiModelProperty;

/**
 * this object is here to help the ask-type query result to be shawn
 *
 * @author Eloan LAGIER
 */
public class Ask {

    Boolean exist;
    String rdfType;

    @ApiModelProperty(example = "true")
    public Boolean getExist() {
        return exist;
    }

    public void setExist(Boolean exist) {
        this.exist = exist;
    }

    @ApiModelProperty(example = "Class")
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }
}
