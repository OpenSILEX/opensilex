/*
 * ******************************************************************************
 *                                     FactorGetDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import java.net.URI;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorSearchDTO {
    
    private URI uri;

    private String name;

    private String comment;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        if(name != null && name.trim().isEmpty()){
            return null;
        } 
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
   
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
 
}
