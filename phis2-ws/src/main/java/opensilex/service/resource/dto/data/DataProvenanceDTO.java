/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.resource.dto.data;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import opensilex.service.configuration.DateFormat;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;

/**
 *
 * @author boizetal
 */
public class DataProvenanceDTO {
    
    @URL
    @Required
    protected String uri;
    @SerializedName("prov:used")
    protected String provUsed;
    @SerializedName("prov:startedAt")
    protected String provStartedAt;   
    
    public DataProvenanceDTO(DataProvenance provenance) {       
        
        setUri(provenance.getUri());
        setProvUsed(provenance.getProvUsed());
        
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        if (provenance.getProvStartedAt() != null) {
            setProvStartedAt(df.format(provenance.getProvStartedAt()));
        }
    }
    

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getProvUsed() {
        return provUsed;
    }

    public void setProvUsed(String provUsed) {
        this.provUsed = provUsed;
    }

    public String getProvStartedAt() {
        return provStartedAt;
    }

    public void setProvStartedAt(String provStartedAt) {
        this.provStartedAt = provStartedAt;
    }
    
    
}
