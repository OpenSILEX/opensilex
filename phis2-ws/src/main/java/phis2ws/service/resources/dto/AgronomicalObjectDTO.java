//**********************************************************************************************
//                               AgronomicalObjectDTO.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  July 18, 2017 - modifications of the saved data and save in triplestore
// Subject: A class which contains methods to automatically check the attributes 
//          of a class, from rules defined by the user
//***********************************************************************************************

package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyPostDTO;
import phis2ws.service.view.model.phis.AgronomicalObject;
import phis2ws.service.resources.validation.interfaces.URL;

/**
 * Represents the submitted JSON for the agronomical objects
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class AgronomicalObjectDTO extends AbstractVerifiedClass {
    
    //the argonomical object type 
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#Plot)
    private String rdfType;
    //The WKT geometry (WGS84 EPSG4326) of the agronomical object
    //(e.g. POLYGON(0 1, 1 2, 2 3, 3 0, 0 1)
    private String geometry;
    //the concerned experiment (e.g. http://www.phenome-fppn.fr/diaphen/DIA2018-2)
    private String experiment;
    //the object which as part the agronomical object 
    //(e.g. http://www.phenome-fppn.fr/mtp/2017/o1032491)
    private String isPartOf;
    //the year used to generated the agronomical object. If it is not given, this is the actual year
    //(e.g. 2017)
    private String year;
    //the properties of the agronomical object
    private ArrayList<PropertyPostDTO> properties;

    @Override
    public AgronomicalObject createObjectFromDTO() {
        AgronomicalObject agronomicalObject = new AgronomicalObject();
        agronomicalObject.setRdfType(rdfType);
        agronomicalObject.setGeometry(geometry);
        agronomicalObject.setUriExperiment(experiment);
        agronomicalObject.setIsPartOf(isPartOf);
        
        if (properties != null) {
            properties.forEach((property) -> {
                agronomicalObject.addProperty(property.createObjectFromDTO());
            });
        }
        
        return agronomicalObject;
    }
    
    //SILEX:todo
    // Do the geometry validator (needs discussions about the 
    // allowed formats and geometry types)
    //\SILEX:todo
    
    @ApiModelProperty(example = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")
    public String getGeometry() {
        return geometry;
    }
    
    @Required
    @URL
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/vocabulary/2017#Plot")
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    @Pattern(regexp = DateFormats.YEAR_REGEX, message = "This is not a valid year. Excepted format : YYYY (e.g. 2017)")
    @ApiModelProperty(example = "2017")
    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
    
    @URL
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/DIA2017-1")
    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    @URL
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
}
