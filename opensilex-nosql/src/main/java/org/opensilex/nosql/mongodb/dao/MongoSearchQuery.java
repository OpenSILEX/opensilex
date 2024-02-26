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

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.function.Function;

public class MongoSearchQuery<T extends MongoModel, F extends MongoSearchFilter, T_RESULT> {
    private ClientSession session;
    @NotNull
    private F filter;
    private Bson filterBson;
    private String filterBsonStr;

    private Bson projection;
    @NotNull
    private Function<T, T_RESULT> convertFunction;
    private CountOptions countOptions;

    /**
     * @param session         The MongoDB client session.
     * @param filter          The filter to apply.
     * @param projection      The projection to apply on search results.
     * @param convertFunction The function to convert models to another type.
     * @param countOptions    Custom CountOptions to use for the search with pagination
     */
    public MongoSearchQuery(ClientSession session, @NotNull F filter, Bson projection, @NotNull Function<T, T_RESULT> convertFunction, CountOptions countOptions) {
        this.session = session;
        this.filter = filter;
        this.projection = projection;
        this.convertFunction = convertFunction;
        this.countOptions = countOptions;
    }

    public MongoSearchQuery() {
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

    public void setSession(ClientSession session) {
        this.session = session;
    }

    public void setFilter(F filter) {
        this.filter = filter;
    }

    public void setProjection(Bson projection) {
        this.projection = projection;
    }

    public void setConvertFunction(Function<T, T_RESULT> convertFunction) {
        this.convertFunction = convertFunction;
    }

    public void setCountOptions(CountOptions countOptions) {
        this.countOptions = countOptions;
    }

    public void setFilterBson(Bson filterBson) {
        Objects.requireNonNull(filterBson);
        this.filterBson = filterBson;
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
