/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.dal;

import java.util.Date;
import java.util.Map;
import javax.jdo.annotations.PersistenceCapable;

/**
 *
 * @author charlero
 */
@PersistenceCapable(table = "logs")
public class LogModel {

    String userUri;

    String request;

    String remoteAdress;

    Map<String, Object> queryParmeters;
    
    Date date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
    

    public Map<String, Object> getQueryParmeters() {
        return queryParmeters;
    }

    public void setQueryParmeters(Map<String, Object> queryParmeters) {
        this.queryParmeters = queryParmeters;
    }

    public String getUserUri() {
        return userUri;
    }

    public void setUserUri(String userUri) {
        this.userUri = userUri;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRemoteAdress() {
        return remoteAdress;
    }

    public void setRemoteAdress(String remoteAdress) {
        this.remoteAdress = remoteAdress;
    }

}
