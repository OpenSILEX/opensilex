/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.sparql.SPARQLQueryHelper;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author vidalmor
 */
public class ExperimentDAO {

    protected final SPARQLService sparql;

    public ExperimentDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ExperimentModel create(ExperimentModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public ExperimentModel update(ExperimentModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(ExperimentModel.class, instanceURI);
    }

    public ExperimentModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(ExperimentModel.class, instanceURI);
    }

    public ListWithPagination<ExperimentModel> search(String aliasPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        SPARQLClassObjectMapper<ExperimentModel> mapper = SPARQLClassObjectMapper.getForClass(ExperimentModel.class);

        Expr aliasFilter = SPARQLQueryHelper.regexFilter(ExperimentModel.ALIAS_FIELD, aliasPattern);
        return sparql.searchWithPagination(
                ExperimentModel.class,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    if (aliasFilter != null) {
                        select.addFilter(aliasFilter);
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }
}
