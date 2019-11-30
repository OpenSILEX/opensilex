/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.model;

import java.net.*;
import java.util.*;
import org.opensilex.sparql.annotations.*;

/**
 *
 * @author vidalmor
 */
public abstract class SPARQLModel {
    
    @SPARQLResourceURI()
    private URI uri;
    
    protected List<SPARQLModelRelation> relations = new ArrayList<>();
        
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public List<SPARQLModelRelation> getRelations() {
        return relations;
    }

    public void setRelations(List<SPARQLModelRelation> relations) {
        this.relations = relations;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.uri);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SPARQLModel other = (SPARQLModel) obj;
        return Objects.equals(this.uri, other.uri);
    }
    
    
}
