//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.Instant;

/**
 * One point of the activity series.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class ActivityBucketDTO {

    @ApiModelProperty(value = "start of the time bucket")
    @JsonProperty("bucket")
    private Instant bucket;

    @ApiModelProperty(value = "distinct accounts that called during the bucket")
    @JsonProperty("users")
    private long users;

    @ApiModelProperty(value = "calls recorded during the bucket")
    @JsonProperty("requests")
    private long requests;

    @ApiModelProperty(value = "calls answered 4xx")
    @JsonProperty("client_errors")
    private long clientErrors;

    @ApiModelProperty(value = "calls answered 5xx or ended in an unhandled exception")
    @JsonProperty("server_errors")
    private long serverErrors;

    @ApiModelProperty(value = "share of failed calls in the bucket")
    @JsonProperty("error_percentage")
    private double errorPercentage;

    public Instant getBucket() {
        return bucket;
    }

    public void setBucket(Instant bucket) {
        this.bucket = bucket;
    }

    public long getUsers() {
        return users;
    }

    public void setUsers(long users) {
        this.users = users;
    }

    public long getRequests() {
        return requests;
    }

    public void setRequests(long requests) {
        this.requests = requests;
    }

    public long getClientErrors() {
        return clientErrors;
    }

    public void setClientErrors(long clientErrors) {
        this.clientErrors = clientErrors;
    }

    public long getServerErrors() {
        return serverErrors;
    }

    public void setServerErrors(long serverErrors) {
        this.serverErrors = serverErrors;
    }

    public double getErrorPercentage() {
        return errorPercentage;
    }

    public void setErrorPercentage(double errorPercentage) {
        this.errorPercentage = errorPercentage;
    }

}
