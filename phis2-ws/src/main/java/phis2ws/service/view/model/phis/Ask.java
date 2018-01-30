//**********************************************************************************************
//                                       Ask.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Janvier 30 2018
// Contact: eloan.lagire@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier 30, 2018
// Subject: Ask model for SPARQL Ask Response
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author Eloan LAGIER
 */
public class Ask {
    String response;
    
    @ApiModelProperty(example = "true")
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response){
        this.response = response;
    }
}
