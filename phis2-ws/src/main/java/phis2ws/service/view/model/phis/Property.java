//**********************************************************************************************
//                                       Property.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  May, 29 2018 - add the domain attribute 
// Subject: Represents the view of Properties (used in Agronomical Object, SensorProfile). 
//          Corresponds to data which will be saved in the triplestore
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 * Represents the view of the properties in the triplestore. 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Property {
    
    /**
     * @param rdfType property type if needed (e.g. http://www.phenome-fppn.fr/vocabulary/2017#Variety)
     *                     null si c'est non typé (String))
     * @param relation nom de la relation (ex. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
     * @param value valeur (ex. plot alias)
     */
    
    //type of the property (if needed)
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#Variety)
    private String rdfType;
    //relation name 
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
    private String relation;
    //value
    //(e.g. plot alias, or the uri of the variety)
    private String value;
    //the domain of the property. For the first version, it is only a single string.
    //then, the union and others will be added
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#MultispectralCamera)
    private String domain;
    
    public Property() {
    }
    
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
