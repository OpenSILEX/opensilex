/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.model;

import java.net.URI;
import javax.jdo.query.BooleanExpression;
import org.opensilex.nosql.utils.ClassURIGenerator;

/**
 *
 * @author sammy
 */
public interface NoSQLModel extends ClassURIGenerator<NoSQLModel> {
    
    /**
     * Method to obtain URI prefix
     * Needed to generate URI with DataNucleusService
     * 
     * @return Prefix defined in class implments this interface
     */
    public String getGraphPrefix(); 
    
    /**
     * Method to get model URI
     * Needed to:
     *          - check validity of URI
     *          - find and update data
     *          - delete data
     * @return URI of the data
     */
    public URI getUri();
    
    /**
     * Method to set model URI
     * Needed to generate and set uri with DataNucleusService
     * 
     * @param uri 
     */
    public void setUri(URI uri);
    
    /**
     * Method to get the typed query about URI
     * Needed to find data by their URI
     * 
     * @param uri
     * @return BooleanExpression
     */
    public BooleanExpression getURIExpr(URI uri); //Typed Query pour la recherche d'URI
    
    /**
     * Methode to update data
     * 
     * @param instance with new value
     * @return updated instance
     */
    public <T extends NoSQLModel> T update(T instance);
    
    @Override
    public String toString();
}
