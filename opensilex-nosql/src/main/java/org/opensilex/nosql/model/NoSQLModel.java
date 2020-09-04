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
        
    public URI getUri();
    public String getGraphPrefix();
    public void setUri(URI uri);
    //public void setMetaData(Object m);
    public <T extends NoSQLModel> T update(T instance);
    public BooleanExpression getURIExpr(URI uri);
    @Override
    public String toString();
}
