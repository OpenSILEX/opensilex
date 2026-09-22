//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * Database volumetry and instance presence. Admin only.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class DbStatsDTO {

    @ApiModelProperty(value = "RDF4J volumetry")
    @JsonProperty("triple_store")
    private TripleStoreStatsDTO tripleStore;

    @ApiModelProperty(value = "MongoDB volumetry")
    @JsonProperty("mongodb")
    private MongoStatsDTO mongodb;

    @ApiModelProperty(value = "who is connected")
    @JsonProperty("users")
    private UserPresenceDTO users;

    @ApiModelProperty(value = "state of the access log writer")
    @JsonProperty("request_log")
    private RequestLogStatusDTO requestLog;

    public TripleStoreStatsDTO getTripleStore() {
        return tripleStore;
    }

    public void setTripleStore(TripleStoreStatsDTO tripleStore) {
        this.tripleStore = tripleStore;
    }

    public MongoStatsDTO getMongodb() {
        return mongodb;
    }

    public void setMongodb(MongoStatsDTO mongodb) {
        this.mongodb = mongodb;
    }

    public UserPresenceDTO getUsers() {
        return users;
    }

    public void setUsers(UserPresenceDTO users) {
        this.users = users;
    }

    public RequestLogStatusDTO getRequestLog() {
        return requestLog;
    }

    public void setRequestLog(RequestLogStatusDTO requestLog) {
        this.requestLog = requestLog;
    }

}
