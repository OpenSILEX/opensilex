//******************************************************************************
//                          DataConfidenceDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import org.opensilex.core.data.dal.DataModel;

/**
 *
 * @author sammy
 */
public class DataConfidenceDTO{
    private Float confidence;
    
    public void setConfidence(Float c){
        this.confidence = c;
    }
    
    public Float getConfidence(){
        return confidence;
    }
    
    public DataModel newModel(){
        DataModel model = new DataModel();
        model.setConfidence(getConfidence());
        return model;
        
    }
}
