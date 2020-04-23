//******************************************************************************
//                                 DataQueryLog.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.logs.dal;
import java.net.URI;
import org.bson.Document;

import java.util.Date;
import java.util.Map;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;

/**
 * DataQueryLog model
 * @author Arnaud Chaleroy
 */
@PersistenceCapable
public class LogModel {
    
    /**
     * URI of the DataQueryLog
     * @example http://www.sunagri.fr/sunagri/id/agent/admin_sunagri 
     */
    @PrimaryKey
    protected URI uri;
    
    /**
     * URI of the userUri
     * @example http://www.sunagri.fr/sunagri/id/agent/admin_sunagri 
     */
    protected URI userUri;
    
    /**
     * Remote URI from which query comes.
     * @example 0:0:0:0:0:0:0:1
     */
    protected String remoteIPAddress;
    
    /**
     * Query
     * @example "{\"variable\": \"http://www.opensilex.org/demo/id/variable/v0000001\"}"
     */
    protected Map query;
    
    /**
     * Date of the value. The format should be yyyy-MM-ddTHH:mm:ssZ
     * @example 2018-06-25T15:13:59+0200
     */
    protected Date date;

    public URI getUserUri() {
        return userUri;
    }

    public void setUserUri(URI userUri) {
        this.userUri = userUri;
    }

    public String getRemoteAdress() {
        return remoteIPAddress;
    }

    public void setRemoteAdress(String remoteAdress) {
        this.remoteIPAddress = remoteAdress;
    }

    public Map getQuery() {
        return query;
    }

    public void setQuery(Map query) {
        this.query = query;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}