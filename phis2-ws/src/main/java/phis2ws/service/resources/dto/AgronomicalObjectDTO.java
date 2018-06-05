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
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.AgronomicalObject;

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
    private ArrayList<PropertyDTO> properties;
            
    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(rdfType, Boolean.TRUE);
        rules.put(geometry, Boolean.TRUE);
        rules.put(experiment, Boolean.FALSE);
        rules.put(isPartOf, Boolean.FALSE);
        rules.put(year, Boolean.FALSE);
        
        return rules;
    }

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

    @ApiModelProperty(example = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")
    public String getGeometry() {
        return geometry;
    }
    
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

    @ApiModelProperty(example = "2017")
    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/DIA2017-1")
    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(String isPartOf) {
        this.isPartOf = isPartOf;
    }

    public ArrayList<PropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyDTO> properties) {
        this.properties = properties;
    }
}
