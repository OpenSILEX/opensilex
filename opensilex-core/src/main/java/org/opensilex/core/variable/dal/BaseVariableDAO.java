//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;

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

    public List<T> createList(List<T> instanceList) throws Exception {
        sparql.create(instanceList);
        return instanceList;
    }

    public T update(T instance, AccountModel currentUser) throws Exception {
        sparql.update(instance);
        return instance;
    }

    /**
     * Delete a resource associated to a variable
     * @param uri URI of the resource to delete (required)
     * @param property The property which link the given uri to a {@link VariableModel} (required)
     * @param currentUser the current user, needed to count data
     * @throws DisplayableBadRequestException if the resource is linked to an existing variable
     * @throws Exception if some errors occurs during deletion operation
     *
     * @apiNote
     * Example : <br>
     * {@code delete(:entity_uri,Oeso.hasEntity)} will delete the given entity by checking
     * if some variable is linked to the entity, before deleting it
     */
    public void delete(URI uri, Property property, AccountModel currentUser) throws DisplayableBadRequestException, Exception {
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
                result -> fetcher.getInstance(result,lang),
                null
        );
    }

    /**
     *  @throws SPARQLInvalidUriListException if any URI from uris could not be loaded
     */
    public List<T> getList(List<URI> uris) throws Exception {
        return getList(uris,null);
    }

    /**
     * The function retrieves a list of objects for export by querying a SPARQL endpoint with specified
     * URIs and language, and fetching specific fields for each object.
     * 
     * @param uris A list of URIs that represent the resources to be fetched from the SPARQL endpoint.
     * @param lang The language parameter specifies the language in which the data should be returned.
     * It is used to retrieve the data in a specific language if it is available.
     * @return A List of objects of type T.
     */
    public List<T> getListForExport(List<URI> uris, String lang) throws Exception {
        Set<String> fieldsToFetch = new HashSet<>();
        fieldsToFetch.add(VariableModel.SPECIES_FIELD_NAME);
        fieldsToFetch.add(BaseVariableModel.EXACT_MATCH_FIELD);
        fieldsToFetch.add(BaseVariableModel.NARROW_MATCH_FIELD);
        fieldsToFetch.add(BaseVariableModel.BROAD_MATCH_FIELD);
        fieldsToFetch.add(BaseVariableModel.CLOSE_MATCH_FIELD);


        return sparql.getListByURIs(
                defaultGraph,
                objectClass,
                uris,
                lang,
                result -> fetcher.getInstance(result, lang),
                fieldsToFetch
        );
    }
}
