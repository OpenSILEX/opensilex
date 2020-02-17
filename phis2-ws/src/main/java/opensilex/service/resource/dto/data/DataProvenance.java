/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.resource.dto.data;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;

/**
 *
 * @author boizetal
 */
public class DataProvenance {
    
    @URL
    @Required
    protected String uri;
    @SerializedName("prov:used")
    protected String provUsed;
    @SerializedName("prov:startedAt")
    protected Date provStartedAt;    

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

    public Date getProvStartedAt() {
        return provStartedAt;
    }

    public void setProvStartedAt(Date provStartedAt) {
        this.provStartedAt = provStartedAt;
    }
    
    
}
