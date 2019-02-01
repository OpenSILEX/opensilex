//**********************************************************************************************
//                                       Uri.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 26 Feb 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  26 Feb, 2018
// Subject: Uri model for the service call
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;

/**
 * Model of the Uri object
 * @author Eloan LAGIER
 */
public class Uri {
    String uri;
    String rdfType;
    private Map<String, String> properties = new HashMap<>();
    private Map<String,String> annotations = new HashMap<>();
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @ApiModelProperty(example = "Document")
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }
    
    @ApiModelProperty(example = "{ Map<String, String> }")
    public Map<String, String> getProperties() {
        return properties;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }
    
   @ApiModelProperty(example = "{ Map<String, String> }")
    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(String key, String value) {
        annotations.put(key, value);
    }
}
