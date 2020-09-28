//******************************************************************************
//                          DataDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.net.URI;
import java.time.ZonedDateTime;

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
