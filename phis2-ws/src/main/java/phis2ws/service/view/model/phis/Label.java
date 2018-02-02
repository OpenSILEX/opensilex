//**********************************************************************************************
//                                       Label.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: Feb 1 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Feb, 1 2018
// Subject: represent the Label view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author Eloan LAGIER
 */
public class Label {
    String name;
    
    
    public void setName (String name) {
        this.name = name;
    }
    
    
    @ApiModelProperty("'document'@en")
    public String getName () {
        return name;
    }
    
}
