/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.EntityModel;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
public class DataCreationDTO{
    
    @ValidURI
    protected URI uri;
    
    @ValidURI
    private URI object;
    
    @NotNull
    @ValidURI
    private URI variable;
    
    @NotNull
    private DataProvenanceModel provenance;
    
    @NotNull
    @Date({DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ})
    private String date;
    
    @NotNull
    private Object value;
    
    @Min(0)
    @Max(1)
    private Float confidence = null;
    
    private Map metadata;
    
    @JsonProperty("prov:used")
    List<EntityModel> provUsed;
    
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public void setObject(URI object){
        this.object = object;
    }
    
    public void setVariable(URI variable){
        this.variable = variable;
    }
    
    public void setProvenance(DataProvenanceModel provenance){
        this.provenance = provenance;
    }
    
    public void setDate( String date){
        this.date = date;
    }
       
    public void setValue(Object value){
        this.value = value;
    }
    
    public void setConfidence(Float c){
        this.confidence = c;
    }
    
    public URI getUri() {
        return uri;
    }
    
    public URI getObject(){
        return object;
    }
    
    public URI getVariable(){
        return variable;
    }
    
    public DataProvenanceModel getProvenance(){
        return provenance;
    }
    
    public String getDate(){
        return date;
    }
    
    public Object getValue(){
        return value;
    }

    public Float getConfidence(){
        return confidence;
    }

    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata(Map metadata) {
        this.metadata = metadata;
    }

    public List<EntityModel> getProvUsed() {
        return provUsed;
    }
      
    public DataModel newModel() throws ParseException {
        DataModel model = new DataModel();

        model.setUri(getUri());        
        model.setObject(getObject());
        model.setVariable(getVariable());
        model.setProvenanceURI(getProvenance().getUri());
        model.setProvenanceSettings(getProvenance().getSettings());
        model.setProvUsed(getProvenance().getProvUsed());
        model.setDate(getDate());        
        model.setValue(getValue());        
        model.setConfidence(getConfidence());
        model.setMetadata(getMetadata());
        
        return model;
        
    }
}
