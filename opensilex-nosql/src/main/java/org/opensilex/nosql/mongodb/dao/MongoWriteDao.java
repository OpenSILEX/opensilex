package org.opensilex.nosql.mongodb.dao;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.UpdateResult.*;
import org.opensilex.nosql.exceptions.MongoDbUniqueIndexConstraintViolation;
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
    @NotNull InsertOneResult create(@NotNull T instance) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException, MongoDbUniqueIndexConstraintViolation;

    /**
     * Create a new model instance in the database within a client session.
     *
     * @param session  The MongoDB client session.
     * @param instance The model instance to create.
     * @return The result of the insertion operation.
     * @throws MongoException                   If a MongoDB error occurs.
     * @throws URISyntaxException               If there's an issue with URI syntax
     * @throws NoSQLAlreadyExistingUriException If the URI already exists in the collection
     */
    @NotNull InsertOneResult create(ClientSession session, @NotNull T instance) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException, MongoDbUniqueIndexConstraintViolation;

    /**
     * Create multiple model instances in the database.
     *
     * @param instances The list of model instances to create.
     * @return The result of the insertion operation.
     * @throws MongoException If a MongoDB error occurs.
     * @throws URISyntaxException If the provided URI is invalid.
     * @throws org.opensilex.nosql.exceptions.MongoDbUniqueIndexConstraintViolation if an unique index constraint violation occurs during creation
     *
     */
    @NotNull InsertManyResult create(@NotNull List<T> instances) throws MongoException, MongoDbUniqueIndexConstraintViolation,  URISyntaxException, NoSQLAlreadyExistingUriException;

    /**
     * Create multiple model instances in the database within a client session.
     *
     * @param session   The MongoDB client session.
     * @param instances The list of model instances to create.
     * @return The result of the insertion operation.
     * @throws MongoException     If a MongoDB error occurs.
     * @throws URISyntaxException If the provided URI is invalid.
     */
    @NotNull InsertManyResult create(ClientSession session, @NotNull @NotEmpty List<T> instances) throws MongoException, URISyntaxException, NoSQLAlreadyExistingUriException, MongoDbUniqueIndexConstraintViolation;

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
     * Within a client session : Update an existing model instance in the database if it exists,
     * otherwise insert it.
     *
     * @param session  The MongoDB client session.
     * @param instance The model instance to update.
     * @throws MongoException If a MongoDB error occurs.
     */
    void upsert(ClientSession session, @NotNull T instance) throws MongoException;

    /**
     * Update an existing model instance in the database if it exists,
     * otherwise insert it.
     *
     * @param instance The model instance to update.
     * @throws MongoException If a MongoDB error occurs.
     */
    void upsert(@NotNull T instance) throws MongoException;

    /**
     * Update an existing model instance in the database within a client session.
     *
     * @param session  The MongoDB client session.
     * @param instance The model instance to update.
     * @throws MongoException If a MongoDB error occurs.
     */
    void update(ClientSession session, @NotNull T instance) throws MongoException, NoSQLInvalidURIException;

    /**
     * Delete a model by its URI from the database.
     *
     * @param uri The URI of the model to delete.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     * @throws NoSQLInvalidURIException if no instance is found
     */
    @NotNull DeleteResult delete(@NotNull URI uri) throws MongoException, NoSQLInvalidURIException;

    /**
     * Delete a model by its URI from the database within a client session.
     *
     * @param session The MongoDB client session.
     * @param uri     The URI of the model to delete.
     * @return The result of the deletion operation.
     * @throws MongoException           If a MongoDB error occurs.
     * @throws NoSQLInvalidURIException if no instance is found
     */
    @NotNull DeleteResult delete(ClientSession session, @NotNull URI uri) throws MongoException, NoSQLInvalidURIException;

    /**
     * Delete multiple models by their URIs from the database within a client session.
     *
     * @param session The MongoDB client session.
     * @param uris    The list of URIs of the models to delete.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull DeleteResult deleteMany(ClientSession session, @NotNull @NotEmpty List<URI> uris) throws MongoException;

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
     * @param session      The MongoDB client session.
     * @param deleteFilter The filter to apply for deletion.
     * @return The result of the deletion operation.
     * @throws MongoException If a MongoDB error occurs.
     */
    @NotNull DeleteResult deleteMany(ClientSession session, @NotNull F deleteFilter) throws MongoException;
}
