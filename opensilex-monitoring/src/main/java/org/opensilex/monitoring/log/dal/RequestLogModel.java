//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log.dal;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * One recorded web service call.
 *
 * <p>{@code startTime} is an {@link Instant} and not a {@code LocalDateTime} on purpose: a MongoDB
 * TTL index only acts on a BSON date, and a {@code LocalDateTime} serialises to a subdocument. The
 * failure mode of getting this wrong is silent — the index exists and expires nothing, forever.</p>
 *
 * <p>{@code success} is denormalised rather than derived from {@code status} so the error-rate
 * aggregation can be answered from an index instead of a computed expression.</p>
 *
 * @author Arnaud Charleroy
 */
public class RequestLogModel extends MongoModel {

    public static final String START_TIME_FIELD = "startTime";
    public static final String ACCOUNT_FIELD = "account";
    public static final String SUCCESS_FIELD = "success";
    public static final String PATH_FIELD = "path";
    public static final String HTTP_METHOD_FIELD = "httpMethod";
    public static final String STATUS_FIELD = "status";

    private Instant startTime;

    /** Explicitly null for an anonymous call, never absent: the aggregation relies on it. */
    private URI account;

    private String accountEmail;

    private boolean anonymous;

    private String httpMethod;

    /** Path without the query string. */
    private String path;

    /** {@code ClassSimpleName.methodName} of the matched resource method, null on a 404. */
    private String service;

    private Integer status;

    private boolean success;

    /** Null when duration recording is switched off. */
    private Long durationMs;

    /** Null unless the call was a GET and parameter recording is enabled. */
    private Document queryParameters;

    /** Null unless client IP recording is explicitly enabled. */
    private String clientIp;

    /** Simple class name of the exception, when the call ended in one. */
    private String errorType;

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        // The write path inserts straight into the collection and never generates a URI; this
        // override only keeps a stray dao.create() from throwing.
        return new String[]{UUID.randomUUID().toString()};
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public URI getAccount() {
        return account;
    }

    public void setAccount(URI account) {
        this.account = account;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Document getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(Document queryParameters) {
        this.queryParameters = queryParameters;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
}
