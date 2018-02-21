//**********************************************************************************************
//                                       Concept.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre, 8 2017
// Contact: eloan.lagier@inra.fr morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date: January, 25 2018
// Subject: A class of concept for the service call
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Model of the Concept Object
 *
 * @author lagier
 */
public class Concept {

    String uri;
    //todo
    private Map<String, String> properties = new HashMap<>();

    public Concept() {
    }

    public Concept(String conceptUri, Map<String, String> infos) {
        this.uri = conceptUri;
        this.properties = infos;
    }

    @ApiModelProperty(example = "http://www.phenome-fppn.fr/vocabulary/2017#Document")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "{ Map<String, String> }")
    public Map<String, String> getProperties() {
        return properties;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }
}
