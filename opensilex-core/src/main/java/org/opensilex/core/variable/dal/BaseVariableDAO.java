//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author vidalmor
 */
public class BaseVariableDAO<T extends SPARQLNamedResourceModel<T>> {

    protected final SPARQLService sparql;
    protected final Class<T> objectClass;
    protected final Node defaultGraph;

    /**
     * Use a specific SPARQL fetcher in order to return results in an optimized way
     * for search
     */
    protected final SparqlNoProxyFetcher<T> fetcher;

    public BaseVariableDAO(Class<T> objectClass, SPARQLService sparql) {
        this.sparql = sparql;
        this.objectClass = objectClass;
        try{
            defaultGraph = sparql.getDefaultGraph(objectClass);
            fetcher = new SparqlNoProxyFetcher<>(objectClass, sparql);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    public T create(T instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public T update(T instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(objectClass, instanceURI);
    }

    public T get(URI instanceURI) throws Exception {
        return sparql.getByURI(objectClass, instanceURI, null);
    }

    public ListWithPagination<T> search(String labelPattern, List<OrderBy> orderByList, Integer page, Integer pageSize, String lang) throws Exception {
        Expr labelFilter = SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, labelPattern);

        return sparql.searchWithPagination(
                defaultGraph,
                objectClass,
                lang,
                (SelectBuilder select) -> {
                    if (labelFilter != null) {
                        select.addFilter(labelFilter);
                    }
                },
                Collections.emptyMap(),
                result -> fetcher.getInstance(result,lang),
                orderByList,
                page,
                pageSize
        );
    }

    public List<T> getList(List<URI> uris, String lang) throws Exception {
        return sparql.getListByURIs(
                defaultGraph,
                objectClass,
                uris,
                lang,
                result -> fetcher.getInstance(result,lang)
        );
    }

    /**
     *  @throws SPARQLInvalidUriListException if any URI from uris could not be loaded
     */
    public List<T> getList(List<URI> uris) throws Exception {
        return getList(uris,null);
    }
}
