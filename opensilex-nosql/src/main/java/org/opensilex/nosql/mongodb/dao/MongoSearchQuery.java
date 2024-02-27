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

import java.util.Objects;
import java.util.function.Function;

/**
 * A parameter object used to group the different options when running a read/search query
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

    // Only used in order to store effective Bson without recomputing it from F filter (ex : for logging or count+find)
    private Bson filterBson;

    // Only used in order to store effective Bson without recomputing it from F filter (ex: when use it for logging
    private String filterBsonStr;

    private Bson projection;

    // The function to convert models to another type.
    private Function<T, T_RESULT> convertFunction;

    // Custom CountOptions to use during count before the search with pagination
    private CountOptions countOptions;

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

    public MongoSearchQuery<T, F, T_RESULT> setFilter(F filter) {
        this.filter = filter;
        return this;
    }

    public MongoSearchQuery<T, F, T_RESULT> setProjection(Bson projection) {
        this.projection = projection;
        return this;
    }

    public MongoSearchQuery<T, F, T_RESULT> setConvertFunction(Function<T, T_RESULT> convertFunction) {
        this.convertFunction = convertFunction;
        return this;
    }

    public void setCountOptions(CountOptions countOptions) {
        this.countOptions = countOptions;
    }

    public MongoSearchQuery<T, F, T_RESULT> setFilterBson(Bson filterBson) {
        Objects.requireNonNull(filterBson);
        this.filterBson = filterBson;
        return this;
    }

    public Bson getFilterBson() {
        return filterBson;
    }

    public String getFilterBsonStr() {
        if(filterBsonStr == null){
            filterBsonStr = filterBson.toString();
        }
        return filterBsonStr;
    }
}
