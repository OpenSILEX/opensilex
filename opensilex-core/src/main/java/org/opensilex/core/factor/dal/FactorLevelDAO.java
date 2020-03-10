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
import org.opensilex.core.factor.api.FactorLevelSearchDTO;
import org.opensilex.core.factor.api.FactorSearchDTO;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;
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

    public ListWithPagination<FactorLevelModel> search(FactorLevelSearchDTO factorLevelSearchDTO, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        return sparql.searchWithPagination(
                FactorLevelModel.class,
                null,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    appendFilters(factorLevelSearchDTO, select);
                },
                orderByList,
                page,
                pageSize
        );
    }
    
     /**
     * Append FILTER or VALUES clause on the given {@link SelectBuilder} for each non-empty simple attribute ( not a {@link List} from the {@link ExperimentSearchDTO}
     *
     * @param searchDTO a search DTO which contains all attributes about an {@link FactorLevelModel} search
     * @param select search query
     * @throws java.lang.Exception can throw an exception
     * @see SPARQLQueryHelper the utility class used to build Expr
     */
    protected void appendFilters(FactorLevelSearchDTO searchDTO, SelectBuilder select) throws Exception {

        List<Expr> exprList = new ArrayList<>();

        // build equality filters
        if (searchDTO.getHasFactor()!= null) {
            exprList.add(SPARQLQueryHelper.eq(FactorLevelModel.HAS_FACTOR_FIELD, searchDTO.getUri()));
        }
        
        // build regex filters
        if (searchDTO.getAlias()!= null) {
            exprList.add(SPARQLQueryHelper.regexFilter(FactorLevelModel.ALIAS_FIELD, searchDTO.getAlias()));
        }

        for (Expr filterExpr : exprList) {
            select.addFilter(filterExpr);
        }
    }
}
