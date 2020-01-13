/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import org.eclipse.rdf4j.model.Statement;
import org.opensilex.sparql.service.SPARQLStatement;

/**
 *
 * @author vidalmor
 */
public class RDF4JStatement implements SPARQLStatement {

    private Statement statement;
    
    public RDF4JStatement(Statement statement) {
        this.statement = statement;
    }

    @Override
    public String getSubject() {
        return statement.getSubject().stringValue();
    }

    @Override
    public String getPredicate() {
        return statement.getPredicate().stringValue();
    }

    @Override
    public String getObject() {
        return statement.getObject().stringValue();
    }

    @Override
    public String getContext() {
        if (statement.getContext() == null) {
            return null;
        }
        
        return statement.getContext().stringValue();
    }
    
}
