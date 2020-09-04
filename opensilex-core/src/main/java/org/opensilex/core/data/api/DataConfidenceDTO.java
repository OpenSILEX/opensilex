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
    private URI uri;
    private int confidence;
    
    public void setUri(URI uri){
        this.uri = uri;
    }
    
    public void setConfidence(int c){
        this.confidence = c;
    }
    
    public URI getUri(){
        return uri;
    }
    
    public int getConfidence(){
        return confidence;
    }
    
    public DataModel newModel(){
        DataModel model = new DataModel();
        model.setUri(getUri());
        model.setConfidence(getConfidence());
        return model;
        
    }
}
