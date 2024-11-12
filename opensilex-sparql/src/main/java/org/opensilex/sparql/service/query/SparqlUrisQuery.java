package org.opensilex.sparql.service.query;

import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A generic interface for executing SPARQL query which take a List of URIs as input
 * The interface define methods corresponding to several use cases when searching/dealing with a query with multiple URI
 *
 * <ul>
 *     <li>Get results/models associated with n URIS : {@link #resultsAsStream(Function)}</li>
 *     <li>Get unknown URIs fromm the database (useful when only validation is required) {@link #getUnknownStream()}</li>
 *     <li>Get existing URIs fromm the database {@link #getExistingStream()} ()}</li>
 * </ul>
 *
 * @param <T> The kind of {@link SPARQLResourceModel} returned by the {@link #resultsAsStream(Function)} query
 * @author rcolin
 */
public interface SparqlUrisQuery<T extends SPARQLResourceModel> {

    /**
     * @return the {@link Stream} of URI which are used as input of the query
     */
    Stream<String> getUniqueUriStringStream();

    /**
     * @return the number of URI which are used as input of the query
     */
    int getQuerySize();

    /**
     * @return a {@link Stream} of {@link T} which match the query input. Each T model returned by the database must have
     * a URI included in the input URIs
     * @throws SPARQLException if the query execution fails
     * @see SPARQLResourceModel#URI_FIELD
     */
    Stream<T> resultsAsStream(Function<SPARQLResult, T> resultHandler) throws SPARQLException;

    /**
     * Consume each result from {@link #resultsAsStream(Function)} and check if some URI were not found
     *
     * @throws SPARQLInvalidUriListException if some URI was not found
     */
    default List<T> getResults(Function<SPARQLResult, T> resultHandler) throws SPARQLException{
        return getResults(resultHandler, "The following URIs don't exists");
    }


    /**
     * Consume each result from {@link #resultsAsStream(Function)} and check if some URI were not found
     *
     * @throws SPARQLInvalidUriListException if some URI was not found
     */
    List<T> getResults(Function<SPARQLResult, T> resultHandler, String errorMsg) throws SPARQLException;

    /**
     * @return a {@link Stream} of {@link URI} which match the query input/exists in the database
     * @throws SPARQLException if the query execution fails
     */
    Stream<URI> getExistingStream() throws SPARQLException;

    /**
     * @return a {@link Stream} of {@link URI} which don't match the query input/don't exist in the database
     * @throws SPARQLException if the query execution fails
     */
    Stream<URI> getUnknownStream() throws SPARQLException;

    /**
     * Check if all URI are known, throws a {@link SPARQLInvalidUriListException} containing unknown URIs else
     */
    void checkUnknowns(String errorMsg) throws SPARQLException;

}
