/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.TimeZone;
import org.eclipse.rdf4j.query.algebra.evaluation.function.datetime.Timezone;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
public class DataDTO {
    
    @ValidURI
    protected URI uri;
    
    private URI provenance;
    
    private URI object;
    
    @NotNull
    private URI variable;
    
    private String date;
    private String timezone;
    
    @NotNull
    private Object value;
    
    private int confidence;
    
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public void setObject(URI object){
        this.object = object;
    }
    
    public void setVariable(URI variable){
        this.variable = variable;
    }
    
    public void setProvenance(URI provenance){
        this.provenance = provenance;
    }
    
    public void setDate( String date){
        this.date = date;
    }
    
    public void setDate( ZonedDateTime date){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXX");
        this.date = dtf.format(date);
    }
    
    public void setTimezone(String tz){
        this.timezone = tz;
    }
    
    public void setValue(Object value){
        this.value = value;
    }
    
    public void setConfidence(int c){
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
    
    public URI getProvenance(){
        return provenance;
    }
    
    public String getDate(){
        return date;
    }
    
    public String getTimezone(){
        return timezone;
    }
    
    public Object getValue(){
        return value;
    }

    public int getConfidence(){
        return confidence;
    }
}
