/*
 *  *************************************************************************************
 *  MongoSearchQuery.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2024
 * Contact :  user@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.nosql.mongodb.dao;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.CountOptions;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.utils.pagination.PaginatedSearchStrategy;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.function.Function;

import static org.opensilex.utils.pagination.PaginatedSearchStrategy.COUNT_QUERY_BEFORE_SEARCH;

/**
 * A parameter object used to group the different options when running a read/search query
 * The following settings are available
 * <ul>
 *  <li>{@link #setFilter(MongoSearchFilter)} : The search filter to use</li>
 *  <li>{@link #setConvertFunction(Function)} : Use a function for mapping result from database cursor into another object. (Required) Use {@link Function#identity()} if no conversion is needed</li>
 *  <li>{@link #setProjection(Bson)} : Select the fields to include/exclude from database</li>
 *  <li>{@link #setSession(ClientSession)} : Use a given {@link ClientSession}. Useful when you want to read/search on documents which are not yet committed</li>
 *  <li> {@link #setPaginationStrategy(PaginatedSearchStrategy)} : Determine how to compute the pagination information associated with the search </li>
 *  <li> {@link #setCountOptions(CountOptions)} : Use a custom {@link CountOptions} when performing the count. Only relevant if {@code getCountStrategy() == COUNT_QUERY_BEFORE_SEARCH}  </li>
 * </ul>
 *
 * @param <T> The type of the MongoDB model.
 * @param <F> The kind of filter specific to this DAO.
 * @param <T_RESULT> The Type of object after model conversion (optional)
 *
 * @author rcolin
 */
public class MongoSearchQuery<T extends MongoModel, F extends MongoSearchFilter, T_RESULT> {


    // The MongoDB client session
    private ClientSession session;

    // The filter to apply
    private F filter;

    private Bson projection;

    // The function to convert models to another type.
    private Function<T, T_RESULT> convertFunction;

    // The count strategy for paginated search
    private PaginatedSearchStrategy paginationStrategy;


    // Custom CountOptions to use during count before the search with pagination
    private CountOptions countOptions;

    public MongoSearchQuery() {
        paginationStrategy = COUNT_QUERY_BEFORE_SEARCH;
    }

    public ClientSession getSession() {
        return session;
    }

    public F getFilter() {
        return filter;
    }

    public Bson getProjection() {
        return projection;
    }

    public Function<T, T_RESULT> getConvertFunction() {
        return convertFunction;
    }

    public CountOptions getCountOptions() {
        return countOptions;
    }

    public MongoSearchQuery<T, F, T_RESULT> setSession(ClientSession session) {
        this.session = session;
        return this;
    }

    public MongoSearchQuery<T, F, T_RESULT> setFilter(@NotNull F filter) {
        Objects.requireNonNull(filter);
        this.filter = filter;
        return this;
    }

    public MongoSearchQuery<T, F, T_RESULT> setProjection(Bson projection) {
        Objects.requireNonNull(projection);
        this.projection = projection;
        return this;
    }

    public MongoSearchQuery<T, F, T_RESULT> setConvertFunction(@NotNull Function<T, T_RESULT> convertFunction) {
        Objects.requireNonNull(convertFunction);
        this.convertFunction = convertFunction;
        return this;
    }

    public void setCountOptions(CountOptions countOptions) {
        this.countOptions = countOptions;
    }

    public PaginatedSearchStrategy getPaginationStrategy() {
        return paginationStrategy;
    }

    public MongoSearchQuery<T, F, T_RESULT> setPaginationStrategy(PaginatedSearchStrategy countStrategy) {
        this.paginationStrategy = countStrategy;
        return this;
    }
}