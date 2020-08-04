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
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.factor.api.FactorSearchDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
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

    public ListWithPagination<FactorModel> search(URI uri, String name, String category, URI experimentUri, List<OrderBy> orderByList, Integer page, Integer pageSize, String lang) throws Exception {
        return sparql.searchWithPagination(FactorModel.class,
                lang,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    appendFilters(uri, name, category, experimentUri, select);
                },
                orderByList,
                page,
                pageSize
        );
    }

    public List<FactorModel> getAll(String lang) throws Exception {
        return sparql.search(FactorModel.class, lang);
    }

    /**
     * Append FILTER or VALUES clause on the given {@link SelectBuilder} for
     * each non-empty simple attribute ( not a {@link List} from the
     * {@link FactorSearchDTO}
     *
     * @param uri uri factor uri attribute
     * @param name name search attribute
     * @param category category of the factor
     * @param experimentUri experiment uri
     * @param select search query
     * @throws java.lang.Exception can throw an exception
     * @see SPARQLQueryHelper the utility class used to build Expr
     */
    protected void appendFilters(URI uri, String name, String category, URI experimentUri, SelectBuilder select) throws Exception {
        // build regex filters
        if (!StringUtils.isEmpty(name) && uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            select.addFilter(
                    SPARQLQueryHelper.or(
                            SPARQLQueryHelper.regexFilter(FactorModel.NAME_FIELD, name),
                            SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null)
                    ));
        } else {
            if (uri != null) {
                Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
                Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
                select.addFilter(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null));
            }
            if (!StringUtils.isEmpty(name)) {
                select.addFilter(SPARQLQueryHelper.regexFilter(FactorModel.NAME_FIELD, name));
            }
        }

        if (!StringUtils.isEmpty(category)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(FactorModel.CATEGORY_FIELD, category));
        }

        if (experimentUri != null && !StringUtils.isEmpty(experimentUri.toString())) {
            addWhere(select, experimentUri, Oeso.studyEffectOf, FactorModel.URI_FIELD);
        }

    }

    private static void addWhere(SelectBuilder select, URI subjectVar, Property property, String objectVar) {
        select.getWhereHandler().getClause().addTriplePattern(new Triple(SPARQLDeserializers.nodeURI(subjectVar), property.asNode(), makeVar(objectVar)));
    }

    private void appendUriRegexFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            select.addFilter(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null));
        }
    }
    
    public List<FactorModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(FactorModel.class, uris, null);
    }
}
