//**********************************************************************************************
//                                       InstanceT.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre, 8 2017
// Contact: eloan.lagier@inra.fr morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date: Decembre, 8 2017
// Subject: A class of InstanceT for the service call
//***********************************************************************************************
package phis2ws.service.view.model.phis;
import io.swagger.annotations.ApiModelProperty;


public class Instance  {
        /**
     * @attribute uri ex. http://www.phenome-fppn.fr/phenovia/documents/document90fb96ace2894cdb9f4575173d8ed4c9
     * @attribute type ex. http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument
     * @param uri  ex. http://www.phenome-fppn.fr/phenovia/documents/document90fb96ace2894cdb9f4575173d8ed4c9
     * @param type ex. http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument
     */
    String uri;
    String type;
    
    public Instance() {
        
    }
    
    public Instance(String uri,String type) {
        this.uri = uri;
        this.type = type;
        
    }
    
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/phenovia/documents/document90fb96ace2894cdb9f4575173d8ed4c9")
    public String getUri() {
        return uri;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument")
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}
