/*
 * ******************************************************************************
 *                                     FactorDAO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.factor.api.FactorSearchDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorDAO {

     // 1. TODO list properties skos
    
    protected final SPARQLService sparql;

    public FactorDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public FactorModel create(FactorModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public FactorModel update(FactorModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(FactorModel.class, instanceURI);
    }

    public FactorModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(FactorModel.class, instanceURI, null);
    }
            
    public ListWithPagination<FactorModel> search(String alias, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        return sparql.searchWithPagination(
                FactorModel.class,
                null,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    appendFilters(alias, select);
                },
                orderByList,
                page,
                pageSize
        );
    }

    public List<FactorModel> getAll() throws Exception {
       return  sparql.search(FactorModel.class, null); 
    }

    /**
     * Append FILTER or VALUES clause on the given {@link SelectBuilder} for each non-empty simple attribute ( not a {@link List} from the {@link FactorSearchDTO}
     *
     * @param alias alias search attribute
     * @param select search query
     * @throws java.lang.Exception can throw an exception
     * @see SPARQLQueryHelper the utility class used to build Expr
     */
    protected void appendFilters(String alias, SelectBuilder select) throws Exception {

        List<Expr> exprList = new ArrayList<>();

        // build regex filters
        if (alias != null) {
            exprList.add(SPARQLQueryHelper.regexFilter(FactorModel.ALIAS_FIELD, alias));
        }

        for (Expr filterExpr : exprList) {
            select.addFilter(filterExpr);
        }
    }
}
