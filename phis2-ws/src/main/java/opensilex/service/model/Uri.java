//******************************************************************************
//                                   Uri.java 
// SILEX-PHIS
// Copyright © - INRA - 2018
// Creation date: 26 Feb 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//******************************************************************************
package opensilex.service.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import opensilex.service.documentation.DocumentationAnnotation;

/**
 * URI model.
 * @author Eloan LAGIER
 */
public class Uri {
    String uri;
    String rdfType;
    String label;
    private Map<String, String> properties = new HashMap<>();
    private Map<String,String> annotations = new HashMap<>();

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
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
