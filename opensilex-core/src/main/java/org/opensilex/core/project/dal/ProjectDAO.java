//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.project.dal;

import java.net.*;
import java.util.*;
import org.apache.jena.arq.querybuilder.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.mapping.*;
import org.opensilex.sparql.utils.*;
import org.opensilex.utils.*;

/**
 *
 * @author vidalmor
 */
public class ProjectDAO  {
    
 protected final SPARQLService sparql;
    
    
    public ProjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public ProjectModel create(ProjectModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public ProjectModel update(ProjectModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(ProjectModel.class, instanceURI);
    }

    public ProjectModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(ProjectModel.class, instanceURI);
    }

    public ListWithPagination<ProjectModel> find(List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        SPARQLClassObjectMapper<ProjectModel> mapper = SPARQLClassObjectMapper.getForClass(ProjectModel.class);
        
        return sparql.searchWithPagination(
                ProjectModel.class,
                (SelectBuilder select) -> {
                    // TODO implements filters
                },
                orderByList,
                page,
                pageSize
        );
    }
    
}
