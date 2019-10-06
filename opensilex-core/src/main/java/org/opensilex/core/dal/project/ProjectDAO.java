/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.dal.project;

import java.net.URI;
import org.opensilex.sparql.SPARQLService;

/**
 *
 * @author Morgane Vidal
 */
public class ProjectDAO {
    
    private final SPARQLService sparql;
    
    public ProjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public Project getProjectByUri(URI uri) throws Exception {
        return sparql.getByURI(Project.class, uri);
    }
}
