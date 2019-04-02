//******************************************************************************
//                                       ScientificObjectDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 28 mars 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.scientificObject;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyPostDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.ScientificObject;

/**
 * Represents the submitted JSON for the scientific objects
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ScientificObjectPutDTO  extends AbstractVerifiedClass {
    //The scientific object type.
    //@example http://www.opensilex.org/vocabulary/oeso#Plot
    private String rdfType;
    //The WKT geometry (WGS84 EPSG4326) of the scientific object.
    //@see https://fr.wikipedia.org/wiki/Well-known_text
    //@example POLYGON(0 1, 1 2, 2 3, 3 0, 0 1)
    private String geometry;
    //The object which as part the scientific object.
    //@example http://www.opensilex.org/demo/2018/o18000074
    private String isPartOf;
    //The label of the scientific object.
    //@example P039_A
    private String label;
    //The properties of the scientific object.
    private ArrayList<PropertyPostDTO> properties;

    @Override
    public ScientificObject createObjectFromDTO() {
        ScientificObject scientificObject = new ScientificObject();
        scientificObject.setRdfType(rdfType);
        scientificObject.setGeometry(geometry);
        scientificObject.setIsPartOf(isPartOf);
        scientificObject.setLabel(label);
        
        if (properties != null) {
            properties.forEach((property) -> {
                scientificObject.addProperty(property.createObjectFromDTO());
            });
        }
        
        
        return scientificObject;
    }
    
    //SILEX:todo
    // Do the geometry validator (needs discussions about the 
    // allowed formats and geometry types)
    //\SILEX:todo
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_POLYGON)
    public String getGeometry() {
        return geometry;
    }
    
    @Required
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI)
    public String getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(String isPartOf) {
        this.isPartOf = isPartOf;
    }
    @Valid
    public ArrayList<PropertyPostDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyPostDTO> properties) {
        this.properties = properties;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_LABEL)
    public String getLabel() {
        return label;
    }
}
