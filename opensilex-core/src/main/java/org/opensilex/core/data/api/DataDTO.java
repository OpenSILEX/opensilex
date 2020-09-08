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
public interface DataDTO {
    
    public void setUri(URI uri);
    
    public void setObject(URI object);
    
    public void setVariable(URI variable);
    
    public void setProvenance(URI provenance);
    
    public void setDate( String date);
    
    public void setDate( ZonedDateTime date);
    
    public void setTimezone(String tz);
    
    public void setValue(Object value);
    
    public void setConfidence(Integer c);
    
    public URI getUri();
    
    public URI getObject();
    
    public URI getVariable();
    
    public URI getProvenance();
    
    public String getDate();
    
    public String getTimezone();
    
    public Object getValue();

    public Integer getConfidence();
}
