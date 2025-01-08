package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.CountOptions;
import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.StreamWithPagination;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Interface for MongoDB read operations related to a specific model.
 *
 * @param <T> The type of the MongoDB model.
 * @param <F> The kind of filter specific to this DAO.
 * @author rcolin
 */
public interface MongoReadDao<T extends MongoModel, F extends MongoSearchFilter> {

    /**
     * Get a model by its URI.
     *
     * @param uri The URI of the model to retrieve.
     * @return The model corresponding to the given URI.
     * @throws NoSQLInvalidURIException If no Model with the given URI is found from database
     */
    @NotNull T get(@NotNull URI uri) throws NoSQLInvalidURIException;

    /**
     * Get a model by its URI within a client session.
     *
     * @param session The MongoDB client session.
     * @param uri     The URI of the model to retrieve.
     * @return The model corresponding to the given URI.
     * @throws NoSQLInvalidURIException If no Model with the given URI is found from database
     */
    @NotNull T get(ClientSession session, @NotNull URI uri) throws NoSQLInvalidURIException;

    /**
     * Get a model by its URI within a client session. And apply projection.
     *
     * @param session The MongoDB client session. Can be null.
     * @param uri     The URI of the model to retrieve.
     * @param projection The projection to apply to result.
     * @return The model corresponding to the given URI.
     * @throws NoSQLInvalidURIException If no Model with the given URI is found from database
     */
    @NotNull T get(ClientSession session, @NotNull URI uri, Bson projection) throws NoSQLInvalidURIException;

    /**
     * Finds multiple documents in the specified MongoCollection based on a collection of URIs.
     *
     * @param uris       The collection of URIs to search for documents
     * @param size       The maximum number of URIs to read from uris
     * @return List<T>     The list of documents found (if any)
     */
    @NotNull List<T> findByUris(Stream<URI> uris, int size);

    /**
     * Check if a model with the given URI exists.
     *
     * @param uri The URI of the model to check.
     * @return True if the model exists, false otherwise.
     * @throws MongoException If a MongoDB error occurs.
     */
    boolean exists(@NotNull URI uri) throws MongoException;

    /**
     * Check if a model with the given URI exists within a client session.
     *
     * @param session The MongoDB client session.
     * @param uri     The URI of the model to check.
     * @return True if the model exists, false otherwise.
     * @throws MongoException If a MongoDB error occurs.
     */
    boolean exists(ClientSession session, @NotNull URI uri) throws MongoException;

    /**
     * Count models based on the provided filter.
     *
     * @param filter The filter to apply.
     * @return The number of models matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     */
    long count(@NotNull F filter) throws MongoException;

    /**
     * Count models based on the provided filter within a client session.
     *
     * @param session The MongoDB client session.
     * @param filter  The filter to apply.
     * @param countOptions The custom {@link CountOptions} to use
     * @return The number of models matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     *
     */
    long count(ClientSession session, @NotNull F filter, CountOptions countOptions) throws MongoException;

    /**
     * Search for models based on the provided filter.
     *
     * @param filter The filter to apply.
     * @return List of models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull ListWithPagination<T> searchWithPagination(@NotNull F filter) throws MongoException;

    /**
     * Search for models based on the provided filter within a client session and apply a conversion function.
     *
     * @param <T_RESULT>       The result type after conversion.
     * @param query The search query
     * @return List of converted models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     * @apiNote
     * <ul>
     * <li>
     * For performance and memory usage optimization,
     * it's strongly advised to avoid the materialization of the full list of T result and then to create a new List of T_RESULT.
     * Instead it's preferable to iterate over database results and perform T -> T_RESULT convert on-the-fly.
     * </li>
     * </ul>
     *
    * Use the following fields from {@code mongoSearchQuery} :
     * <ul>
     *     <li>{@link MongoSearchQuery#getSession()} : The MongoDB client session.</li>
     *     <li>{@link MongoSearchQuery#getFilter()} : The filter to apply.</li>
     *     <li>{@link MongoSearchQuery#getProjection()}} : The projection to apply on search results..</li>
     *     <li>{@link MongoSearchQuery#getConvertFunction()} : The document convert function.</li>
     * </ul>
     */
    <T_RESULT> @NotNull ListWithPagination<T_RESULT> searchWithPagination(MongoSearchQuery<T, F, T_RESULT> query) throws MongoException;


    /**
     * Search for models based on the provided filter.
     *
     * @param filter The filter to apply.
     * @return Stream of models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull StreamWithPagination<T> searchAsStreamWithPagination(F filter) throws MongoException;

    /**
     * Search for models based on the provided query
     *
     * @param query The Mongo search query object parameter
     * @return Stream of models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     *
     * @apiNote Use the following fields from {@code mongoSearchQuery} :
     * <ul>
     *     <li>{@link MongoSearchQuery#getSession()} : The MongoDB client session.</li>
     *     <li>{@link MongoSearchQuery#getFilter()} : The filter to apply.</li>
     *     <li>{@link MongoSearchQuery#getProjection()}} : The projection to apply on search results..</li>
     * </ul>
     */
    <T_RESULT> @NotNull StreamWithPagination<T_RESULT> searchAsStreamWithPagination(MongoSearchQuery<T, F, T_RESULT> query) throws MongoException;

    /**
     * Get distinct URIs of models based on the provided filter within a client session.
     *
     * @param session The MongoDB client session.
     * @param filter  The filter to apply.
     * @return List of distinct URIs matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     */
    List<URI> distinctUris(ClientSession session, @NotNull F filter) throws MongoException;

    /**
     * Get distinct URIs of models based on the provided filter
     *
     * @param filter The filter to apply.
     * @return List of distinct URIs matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     */
    List<URI> distinctUris(@NotNull F filter) throws MongoException;

    /**
     * Get distinct values for a specific field based on the provided filter within a client session.
     *
     * @param <T_RESULT>  The result type after conversion.
     * @param session     The MongoDB client session.
     * @param field       The field for which to get distinct values.
     * @param resultClass The class of the result type.
     * @param filter      The filter to apply.
     * @return List of distinct values for the specified field.
     *
     * @apiNote With this method, the page and pageSize specified in {@link MongoSearchFilter#getPage()} and  {@link MongoSearchFilter#getPageSize()}
     * are not used. Use the {@link #distinctWithPagination(ClientSession, String, Class, MongoSearchFilter)} method in case you want to apply pagination
      */
    <T_RESULT> List<T_RESULT> distinct(ClientSession session, @NotNull String field, @NotNull Class<T_RESULT> resultClass, @NotNull F filter);

    /**
     * Get distinct values for a specific field based on the provided filter using aggregation within a client session.
     *
     * @param <T_RESULT>    The result type after conversion.
     * @param session       The MongoDB client session.
     * @param distinctField The field for which to get distinct values.
     * @param resultClass   The class of the result type.
     * @param filter        The filter to apply.
     * @return List of distinct values for the specified field using aggregation, with pagination information
     *
     * @apiNote
     * <ul>
     *     <li>This method return a list instead of a set since collection items unicity must be guaranteed by the database aggregation/li>
     * </ul>
     *
     */
    <T_RESULT> ListWithPagination<T_RESULT> distinctWithPagination(ClientSession session, @NotNull String distinctField, @NotNull Class<T_RESULT> resultClass, @NotNull F filter);

    /**
     * Performs an aggregation operation on the specified collection using the provided aggregation arguments.
     *
     * @param aggregationPipeline The list of aggregation arguments
     * @return List<T>         The set of results after aggregation
     */
    List<T> aggregate(List<Bson> aggregationPipeline);

    /**
     * Performs an aggregation operation on the specified collection using the provided aggregation arguments.
     *
     * @param aggregationPipeline The list of aggregation arguments
     * @return Stream<T>         The Stream of results after aggregation
     */
    Stream<T> aggregateAsStream(List<Bson> aggregationPipeline);

    /**
     * Performs an aggregation operation on the specified collection using the provided aggregation arguments.
     *
     * @param aggregationPipeline The list of aggregation arguments
     * @param <T_RESULT>          The type of elements returned
     * @return List<T_RESULT>           The set of results after aggregation
     */
    <T_RESULT> List<T_RESULT> aggregate(List<Bson> aggregationPipeline, @NotNull Class<T_RESULT> resultClass);

    /**
     * Performs an aggregation operation on the specified collection using the provided aggregation arguments.
     *
     * @param aggregationPipeline The list of aggregation arguments
     * @param <T_RESULT>          The type of elements returned
     * @return Stream<T_RESULT>         The Stream of results after aggregation
     */
    <T_RESULT> Stream<T_RESULT> aggregateAsStream(List<Bson> aggregationPipeline, @NotNull Class<T_RESULT> resultClass);

    /**
     * Perform a lookup aggregation to join collections and apply a conversion function.
     *
     * @param <T_RESULT>           The result type after conversion.
     * @param <T_JOINED>           The type of models in the lookup collection.
     * @param session              The MongoDB client session.
     * @param filter               The filter to apply on the main collection.
     * @param lookupCollectionName The name of the collection to join with.
     * @param lookupField          The field to match in the lookup collection.
     * @param lookupClass          The class of the lookup collection.
     * @param convertFunction      The function to convert joined models to another type.
     * @return List of converted models after performing the lookup aggregation.
     */
    <T_RESULT, T_JOINED> List<T_RESULT> lookupAggregation(
            ClientSession session, F filter,
            @NotNull String lookupCollectionName,
            @NotNull String lookupField,
            @NotNull Class<T_JOINED> lookupClass,
            @NotNull Function<T_JOINED, T_RESULT> convertFunction
    );

}
