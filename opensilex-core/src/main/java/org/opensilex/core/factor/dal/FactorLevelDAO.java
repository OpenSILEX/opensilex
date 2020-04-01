/*
 * ******************************************************************************
 *                                     FactorLevelDAO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inra.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelDAO {

    protected final SPARQLService sparql;

    public FactorLevelDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public FactorLevelModel create(FactorLevelModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public FactorLevelModel update(FactorLevelModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(FactorLevelModel.class, instanceURI);
    }

    public FactorLevelModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(FactorLevelModel.class, instanceURI, null);
    }

    public ListWithPagination<FactorLevelModel> search(String alias, URI hasFactor, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        return sparql.searchWithPagination(
                FactorLevelModel.class,
                null,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    appendFilters(alias, hasFactor, select);
                },
                orderByList,
                page,
                pageSize
        );
    }
    
     /**
     * Append FILTER or VALUES clause on the given {@link SelectBuilder} for each non-empty simple attribute ( not a {@link List} from the {@link ExperimentSearchDTO}
     *
     * @param alias search factor levels by alias
     * @param hasFactor search factor levels by factors
     * @param select search query
     * @throws java.lang.Exception can throw an exception
     * @see SPARQLQueryHelper the utility class used to build Expr
     */
    protected void appendFilters(String alias, URI hasFactor, SelectBuilder select) throws Exception {

        List<Expr> exprList = new ArrayList<>();

       
        // build regex filters
        if (alias != null) {
            exprList.add(SPARQLQueryHelper.regexFilter(FactorLevelModel.ALIAS_FIELD, alias));
        }
        
        // build equality filters
        if (hasFactor!= null) {
            exprList.add(SPARQLQueryHelper.eq(FactorLevelModel.HAS_FACTOR_FIELD, hasFactor));
        }

        for (Expr filterExpr : exprList) {
            select.addFilter(filterExpr);
        }
    }
}
