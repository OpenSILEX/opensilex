/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.dal.project;

import java.net.URI;
import org.opensilex.module.core.service.sparql.SPARQLService;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectDAO {
    
    private final SPARQLService sparql;
    
    public ProjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    /**
     * Get the informations of a project by its URI.
     * @param uri
     * @return the project
     * @throws Exception 
     */
    public Project getProjectByUri(URI uri) throws Exception {
        return sparql.getByURI(Project.class, uri);
    }
}
