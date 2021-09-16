//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.dal;

import java.net.URI;

import java.util.List;


import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.expr.Expr;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;

import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author Hamza IKIOU
 */
public class VariablesGroupDAO {
    
    protected final SPARQLService sparql;

    public VariablesGroupDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public VariablesGroupModel create(VariablesGroupModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public VariablesGroupModel update(VariablesGroupModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(VariablesGroupModel.class, uri);
    }

    public VariablesGroupModel get(URI uri) throws Exception {
        return sparql.getByURI(VariablesGroupModel.class, uri, null);
    }
    
    public List<VariablesGroupModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(VariablesGroupModel.class, uris, null);
    }
    
    private static void addVariableFilter(SelectBuilder select, URI variableUri) throws Exception{       
        Triple triple = new Triple(makeVar(VariablesGroupModel.URI_FIELD),RDFS.member.asNode(),makeVar(VariablesGroupModel.VARIABLES_LIST_FIELD));
        select.addWhere(triple);
        Expr filterVariableUri = SPARQLQueryHelper.eq(VariablesGroupModel.VARIABLES_LIST_FIELD, variableUri);
        select.addFilter(filterVariableUri);
    }
        
    public ListWithPagination<VariablesGroupModel> search(String name, URI variableUri,List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        Expr filterName = SPARQLQueryHelper.regexFilter(VariablesGroupModel.NAME_FIELD, name);
        
        return sparql.searchWithPagination(
                VariablesGroupModel.class,
                null,
                (SelectBuilder select) -> {
                    if (filterName != null) {
                        select.addFilter(filterName);
                    }
                    if (variableUri != null) {                       
                        addVariableFilter(select, variableUri);                                       
                    }
                },
                orderByList,
                page,
                pageSize
        );
       
    }
}
