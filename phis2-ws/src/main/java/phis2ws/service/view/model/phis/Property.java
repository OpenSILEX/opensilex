//**********************************************************************************************
//                                       Property.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 5 2017
// Subject: Represents the view of Properties (used in Agronomical Object). 
//          Corresponds to data which will be saved in the triplestore
//***********************************************************************************************
package phis2ws.service.view.model.phis;

public class Property {
    
    /**
     * @param typeProperty type de la propriété (ex. http://www.phenome-fppn.fr/vocabulary/2017#Variety)
     *                     null si c'est non typé (String))
     * @param relation nom de la relation (ex. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
     * @param value valeur (ex. plot alias)
     */
    private String typeProperty;
    private String relation;
    private String value;

    public Property() {
    }
    
    public String getTypeProperty() {
        return typeProperty;
    }

    public void setTypeProperty(String typeProperty) {
        this.typeProperty = typeProperty;
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
}
