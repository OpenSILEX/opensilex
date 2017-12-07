//**********************************************************************************************
//                               AgronomicalObjectDTO.java 
//
// Author(s): Morgane VIDAL
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
import phis2ws.service.view.model.phis.Property;

public class AgronomicalObjectDTO extends AbstractVerifiedClass {
    
    /**
     * @param typeAgronomicalObject le type de l'objet agronomique (plot, fields, cultivated land...). On donne ici l'uri du concept 
     * @param geometry les coordonnées GPS (idéalement en WGS84) de l'objet agronomique
     * @param experiment l'uri de l'essai concerné s'il y en a un
     * @param year l'année utilisée dans l'uri de l'objet agronomique. Si le champ n'est pas renseigné, on prendra l'année actuelle.
     * @param properties les propriétés associées à l'objet agronomique
     */
    private String typeAgronomicalObject;
    private String geometry;
    private String experiment;
    private String year;
    private ArrayList<Property> properties;
            
    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(typeAgronomicalObject, Boolean.TRUE);
        rules.put(geometry, Boolean.TRUE);
        rules.put(experiment, Boolean.FALSE);
        rules.put(year, Boolean.FALSE);
        
        return rules;
    }

    @Override
    public AgronomicalObject createObjectFromDTO() {
        AgronomicalObject agronomicalObject = new AgronomicalObject();
        agronomicalObject.setTypeAgronomicalObject(typeAgronomicalObject);
        agronomicalObject.setGeometry(geometry);
        agronomicalObject.setUriExperiment(experiment);
        
        if (properties != null) {
            for (Property property : properties) {
                agronomicalObject.addProperty(property);
            }
        }
        
        return agronomicalObject;
    }

    @ApiModelProperty(example = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")
    public String getGeometry() {
        return geometry;
    }
    
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/vocabulary/2017#Plot")
    public String getTypeAgronomicalObject() {
        return typeAgronomicalObject;
    }

    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/DIA2017-1")
    public String getUriExperiment() {
        return experiment;
    }

    public void setUriExperiment(String uriConcernedItem) {
        this.experiment = uriConcernedItem;
    }

    public void setTypeAgronomicalObject(String typeAgronomicalObject) {
        this.typeAgronomicalObject = typeAgronomicalObject;
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
    
    public ArrayList<Property> getProperties() {
        return properties;
    }
    
    public void setProperties(ArrayList<Property> properties) {
        this.properties = properties;
    }
}
