//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.expr.Expr;

import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;


public class BaseMultiLabelsResourceDAO<T extends SPARQLMultiNamedResourceModel<T>> {

    protected final SPARQLService sparql;
    protected final Class<T> objectClass;
    protected final Node defaultGraph;

    /**
     * Use a specific SPARQL fetcher in order to return results in an optimized way
     * for search
     */
    protected final SparqlNoProxyFetcher<T> fetcher;

    public BaseMultiLabelsResourceDAO(Class<T> objectClass, SPARQLService sparql) {
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

    public List<T> createList(List<T> instanceList) throws Exception {
        sparql.create(instanceList);
        return instanceList;
    }

    public T update(T instance) throws Exception {
        sparql.update(instance);
        return instance;
    }


    public void delete(URI uri, Property property) throws DisplayableBadRequestException, Exception {

        Objects.requireNonNull(uri);
        Objects.requireNonNull(property);

        // check that a variable is not linked to the uri, with the given property
        List<URI> linkedVariables = sparql.searchURIs(VariableModel.class,null,select -> {

            // Ex with entity : append GRAPH <VAR_GRAPH> { ?uri oeso:hasEntity <ENTITY_URI>
            select.addGraph(
                    sparql.getDefaultGraph(VariableModel.class),
                    NodeFactory.createVariable(SPARQLResourceModel.URI_FIELD),
                    property.asNode(),
                    SPARQLDeserializers.nodeURI(uri)
            );
        });

        if(! linkedVariables.isEmpty()){
            Map<String,String> translationValues = new HashMap<>();
            translationValues.put("uri", uri.toString());
            translationValues.put("variable", linkedVariables.toString());

            throw new DisplayableBadRequestException(
                    uri + " can't be deleted : used by variables : " + linkedVariables,
                    "component.variable.linked-object-error",
                    translationValues
            );
        }
        sparql.delete(objectClass,uri);
    }

    public T get(URI instanceURI) throws Exception {
        return sparql.getByURI(objectClass, instanceURI, null);
    }


    public ListWithPagination<T> search(String labelPattern,
                                        List<OrderBy> orderByList, Integer page, Integer pageSize, String lang) throws Exception {

        Expr prefLabelFilter = SPARQLQueryHelper.regexFilter(SPARQLMultiNamedResourceModel.PREF_LABELS_FIELD,
                labelPattern);
        Expr altLabelFilter = SPARQLQueryHelper.regexFilter(SPARQLMultiNamedResourceModel.ALT_LABELS_FIELD,
                labelPattern);
        Expr shortLabelFilter = SPARQLQueryHelper.regexFilter(SPARQLMultiNamedResourceModel.SHORT_LABEL_FIELD,
                labelPattern);

        Expr  label;
        if (prefLabelFilter != null && altLabelFilter != null && shortLabelFilter != null) {
            label = SPARQLQueryHelper.getExprFactory().or(prefLabelFilter, altLabelFilter);
        } else if (prefLabelFilter != null && altLabelFilter != null) {
            label = prefLabelFilter;
        } else {
            label = null;
        }

        Map<String, WhereHandler> customQueryHandler = new HashMap<>();
        customQueryHandler.put(SPARQLMultiNamedResourceModel.PREF_LABELS_FIELD,new WhereHandler());
        customQueryHandler.put(SPARQLMultiNamedResourceModel.ALT_LABELS_FIELD,new WhereHandler());
        customQueryHandler.put(SPARQLMultiNamedResourceModel.SHORT_LABEL_FIELD,new WhereHandler());

        ListWithPagination<T> listWithPagination = sparql.searchWithPagination(
                defaultGraph,
                objectClass,
                lang,
                (SelectBuilder select) -> {
                    if (label != null) {
                        select.addFilter(label);
                    }
                },
                customQueryHandler,
                null,
                orderByList,
                page,
                pageSize
        );

        List<T> models = listWithPagination.getList();
        List<T> uniqueModels = new ArrayList<>();
        Set<URI> uniqueUris = new HashSet<>();

        for (T model : models) {
            if (uniqueUris.add(model.getUri())) {
                uniqueModels.add(model);
            }
        }

        listWithPagination.setList(uniqueModels);


        return listWithPagination;
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
