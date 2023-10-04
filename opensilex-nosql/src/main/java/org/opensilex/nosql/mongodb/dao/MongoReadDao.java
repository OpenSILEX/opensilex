package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import org.bson.conversions.Bson;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Interface for MongoDB read operations related to a specific model.
 *
 * @param <T> The type of the MongoDB model.
 * @param <F> The kind of filter specific to this DAO.
 */
public interface MongoReadDao<T extends MongoModel, F extends MongoSearchFilter> {

    /**
     * Get a model by its URI.
     *
     * @param uri The URI of the model to retrieve.
     * @return The model corresponding to the given URI.
     * @throws NoSQLInvalidURIException If the provided URI is invalid.
     */
    T get(URI uri) throws NoSQLInvalidURIException;

    /**
     * Get a model by its URI within a client session.
     *
     * @param session The MongoDB client session.
     * @param uri The URI of the model to retrieve.
     * @return The model corresponding to the given URI.
     * @throws NoSQLInvalidURIException If the provided URI is invalid.
     */
    T get(ClientSession session, URI uri) throws NoSQLInvalidURIException;

    /**
     * Check if a model with the given URI exists.
     *
     * @param uri The URI of the model to check.
     * @return True if the model exists, false otherwise.
     * @throws MongoException If a MongoDB error occurs.
     */
    boolean exists(URI uri) throws MongoException;

    /**
     * Check if a model with the given URI exists within a client session.
     *
     * @param session The MongoDB client session.
     * @param uri The URI of the model to check.
     * @return True if the model exists, false otherwise.
     * @throws MongoException If a MongoDB error occurs.
     */
    boolean exists(ClientSession session, URI uri) throws MongoException;

    /**
     * Count models based on the provided filter.
     *
     * @param filter The filter to apply.
     * @return The number of models matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     */
    long count(F filter) throws MongoException;

    /**
     * Count models based on the provided filter within a client session.
     *
     * @param session The MongoDB client session.
     * @param filter The filter to apply.
     * @return The number of models matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     */
    long count(ClientSession session, F filter) throws MongoException;

    /**
     * Search for models based on the provided filter.
     *
     * @param filter The filter to apply.
     * @return List of models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     */
    ListWithPagination<T> search(F filter) throws MongoException;

    /**
     * Search for models based on the provided filter within a client session.
     *
     * @param session The MongoDB client session.
     * @param filter The filter to apply.
     * @param projection The projection to apply on search results.
     * @return List of models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     */
    ListWithPagination<T> search(ClientSession session, F filter, Bson projection) throws MongoException;

    /**
     * Search for models based on the provided filter and apply a conversion function.
     *
     * @param filter The filter to apply.
     * @param convertFunction The function to convert models to another type.
     * @param <T_RESULT> The result type after conversion.
     * @return List of converted models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     */
    <T_RESULT> ListWithPagination<T_RESULT> search(F filter, Function<T, T_RESULT> convertFunction) throws MongoException;

    /**
     * Search for models based on the provided filter within a client session and apply a conversion function.
     *
     * @param session The MongoDB client session.
     * @param filter The filter to apply.
     * @param projection The projection to apply on search results.
     * @param convertFunction The function to convert models to another type.
     * @param <T_RESULT> The result type after conversion.
     * @return List of converted models matching the filter with pagination information.
     * @throws MongoException If a MongoDB error occurs.
     */
    <T_RESULT> ListWithPagination<T_RESULT> search(ClientSession session, F filter, Bson projection, Function<T, T_RESULT> convertFunction) throws MongoException;

    /**
     * Get distinct URIs of models based on the provided filter within a client session.
     *
     * @param session The MongoDB client session.
     * @param filter The filter to apply.
     * @return Set of distinct URIs matching the filter.
     * @throws MongoException If a MongoDB error occurs.
     */
    Set<URI> distinctUris(ClientSession session, F filter) throws MongoException;

    /**
     * Get distinct values for a specific field based on the provided filter within a client session.
     *
     * @param field The field for which to get distinct values.
     * @param resultClass The class of the result type.
     * @param filter The filter to apply.
     * @param session The MongoDB client session.
     * @param <T_RESULT> The result type after conversion.
     * @return Set of distinct values for the specified field.
     */
    <T_RESULT> Set<T_RESULT> distinct(String field, Class<T_RESULT> resultClass, F filter, ClientSession session);

    /**
     * Get distinct values for a specific field based on the provided filter using aggregation within a client session.
     *
     * @param field The field for which to get distinct values.
     * @param resultClass The class of the result type.
     * @param filter The filter to apply.
     * @param session The MongoDB client session.
     * @param <T_RESULT> The result type after conversion.
     * @return Set of distinct values for the specified field using aggregation.
     */
    <T_RESULT> Set<T_RESULT> distinctAggregation(String field, Class<T_RESULT> resultClass, F filter, ClientSession session);

    /**
     * Perform a lookup aggregation to join collections and apply a conversion function.
     *
     * @param filter The filter to apply on the main collection.
     * @param lookupCollectionName The name of the collection to join with.
     * @param lookupField The field to match in the lookup collection.
     * @param lookupClass The class of the lookup collection.
     * @param convertFunction The function to convert joined models to another type.
     * @param session The MongoDB client session.
     * @param <T_RESULT> The result type after conversion.
     * @param <T_JOINED> The type of models in the lookup collection.
     * @return List of converted models after performing the lookup aggregation.
     */
    <T_RESULT, T_JOINED> List<T_RESULT> lookupAggregation(
            F filter,
            String lookupCollectionName,
            String lookupField,
            Class<T_JOINED> lookupClass,
            Function<T_JOINED, T_RESULT> convertFunction,
            ClientSession session
    );
}
