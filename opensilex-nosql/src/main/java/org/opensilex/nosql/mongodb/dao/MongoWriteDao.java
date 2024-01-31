package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoModel;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Interface for MongoDB write operations related to a specific model.
 *
 * @param <T> The type of the MongoDB model.
 * @param <F> The kind of filter specific to this DAO.
 * @author rcolin
 */
public interface MongoWriteDao<T extends MongoModel, F extends MongoSearchFilter> {

    /**
     * Create a new model instance in the database.
     *
     * @param instance The model instance to create.
     * @return The result of the insertion operation.
     * @throws MongoException If a MongoDB error occurs.
     * @throws URISyntaxException If the provided URI is invalid.
     */
    @NotNull InsertOneResult create(@NotNull T instance) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException;

    /**
     * Create a new model instance in the database within a client session.
     *
     * @param instance The model instance to create.
     * @param session The MongoDB client session.
     * @return The result of the insertion operation.
     * @throws MongoException If a MongoDB error occurs.
     * @throws URISyntaxException If the provided URI is invalid.
     */
    @NotNull InsertOneResult create(@NotNull T instance, ClientSession session) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException;

    /**
     * Create multiple model instances in the database.
     *
     * @param instances The list of model instances to create.
     * @return The result of the insertion operation.
     * @throws MongoException If a MongoDB error occurs.
     * @throws URISyntaxException If the provided URI is invalid.
     */
    @NotNull InsertManyResult create(@NotNull List<T> instances) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException;

    /**
     * Create multiple model instances in the database within a client session.
     *
     * @param instances The list of model instances to create.
     * @param session The MongoDB client session.
     * @return The result of the insertion operation.
     * @throws MongoException If a MongoDB error occurs.
     * @throws URISyntaxException If the provided URI is invalid.
     */
    @NotNull InsertManyResult create(@NotNull @NotEmpty List<T> instances, ClientSession session) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException;

    /**
     * Get the name of the ID field for the model.
     *
     * @return The name of the ID field.
     */
    @NotNull @NotEmpty String idField();

    /**
     * Update an existing model instance in the database.
     *
     * @param instance The model instance to update.
     * @throws MongoException If a MongoDB error occurs.
     */
    void update(@NotNull T instance) throws MongoException, NoSQLInvalidURIException;

    /**
     * Update an existing model instance in the database within a client session.
     *
     * @param instance The model instance to update.
     * @param session The MongoDB client session.
     * @throws MongoException If a MongoDB error occurs.
     */
    void update(@NotNull T instance, ClientSession session) throws MongoException, NoSQLInvalidURIException;

    /**
     * Delete a model by its URI from the database.
     *
     * @param uri The URI of the model to delete.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull DeleteResult delete(@NotNull URI uri) throws MongoException;

    /**
     * Delete a model by its URI from the database within a client session.
     *
     * @param uri The URI of the model to delete.
     * @param session The MongoDB client session.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull DeleteResult delete(@NotNull URI uri, ClientSession session) throws MongoException;

    /**
     * Delete multiple models by their URIs from the database within a client session.
     *
     * @param uris The list of URIs of the models to delete.
     * @param session The MongoDB client session.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull DeleteResult deleteMany(@NotNull @NotEmpty List<URI> uris, ClientSession session) throws MongoException;

    /**
     * Delete models based on the provided filter from the database.
     *
     * @param deleteFilter The filter to apply for deletion.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull DeleteResult deleteMany(@NotNull F deleteFilter) throws MongoException;

    /**
     * Delete models based on the provided filter from the database within a client session.
     *
     * @param deleteFilter The filter to apply for deletion.
     * @param session The MongoDB client session.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull DeleteResult deleteMany(@NotNull F deleteFilter, ClientSession session) throws MongoException;
}
