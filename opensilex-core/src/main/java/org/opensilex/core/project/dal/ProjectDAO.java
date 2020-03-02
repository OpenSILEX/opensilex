//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.project.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author vidalmor
 */
public class ProjectDAO {

    protected final SPARQLService sparql;
    protected final String lang;

    public ProjectDAO(SPARQLService sparql, String lang) {
        this.sparql = sparql;
        this.lang = lang;
    }

    public ProjectModel create(ProjectModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public ProjectModel update(ProjectModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }
    
    public void update(List<ProjectModel> instances) throws Exception {
        sparql.update(instances);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(ProjectModel.class, instanceURI);
    }

    public ProjectModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(ProjectModel.class, instanceURI, lang);
    }

    public ListWithPagination<ProjectModel> search(String uri,
            String name, String shortname, String description, String startDate, String endDate, String homePage, String objective,
            List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        SPARQLClassObjectMapper<ProjectModel> mapper = SPARQLClassObjectMapper.getForClass(ProjectModel.class);

        return sparql.searchWithPagination(
                ProjectModel.class,
                lang,
                (SelectBuilder select) -> {
                    // TODO implements filters
                },
                orderByList,
                page,
                pageSize
        );
    }

    public void create(List<ProjectModel> instances) throws Exception {
        sparql.create(instances);
    }

    public ListWithPagination<ProjectModel> search(List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        return sparql.searchWithPagination(
                ProjectModel.class,
                lang,
                (SelectBuilder select) -> {
                    // TODO implements filters
                },
                orderByList,
                page,
                pageSize
        );
    }
}
