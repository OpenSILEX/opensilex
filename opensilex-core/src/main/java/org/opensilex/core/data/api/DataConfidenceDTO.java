/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import java.net.URI;
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
