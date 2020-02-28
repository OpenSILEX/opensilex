//******************************************************************************
//                                 DataQueryLog.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;
import org.bson.Document;

import java.util.Date;

/**
 * DataQueryLog model
 * @author Arnaud Chaleroy
 */
public class DataQueryLog {
    /**
     * URI of the userUri
     * @example http://www.sunagri.fr/sunagri/id/agent/admin_sunagri 
     */
    protected String userUri;
    
    /**
     * Remote URI from which query comes.
     * @example 0:0:0:0:0:0:0:1
     */
    protected String remoteIPAddress;
    
    
    /**
     * Query
     * @example "{\"variable\": \"http://www.opensilex.org/demo/id/variable/v0000001\"}"
     */
    protected Document query;
    
    /**
     * Date of the value. The format should be yyyy-MM-ddTHH:mm:ssZ
     * @example 2018-06-25T15:13:59+0200
     */
    protected Date date;

    public String getUserUri() {
        return userUri;
    }

    public void setUserUri(String user) {
        this.userUri = user;
    }

    public String getRemoteAdress() {
        return remoteIPAddress;
    }

    public void setRemoteAdress(String remoteAdress) {
        this.remoteIPAddress = remoteAdress;
    }

    public Document getQuery() {
        return query;
    }

    public void setQuery(Document query) {
        this.query = query;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}