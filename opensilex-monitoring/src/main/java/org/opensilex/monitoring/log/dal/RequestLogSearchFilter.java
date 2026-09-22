//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log.dal;

import java.net.URI;
import java.time.Instant;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

/**
 * Search criteria for the access log drill-down.
 *
 * @author Arnaud Charleroy
 */
public class RequestLogSearchFilter extends MongoSearchFilter {

    private Instant startDate;
    private Instant endDate;
    private URI account;
    private String pathPrefix;
    private String httpMethod;
    private Boolean success;

    public Instant getStartDate() {
        return startDate;
    }

    public RequestLogSearchFilter setStartDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public RequestLogSearchFilter setEndDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public URI getAccount() {
        return account;
    }

    public RequestLogSearchFilter setAccount(URI account) {
        this.account = account;
        return this;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public RequestLogSearchFilter setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
        return this;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public RequestLogSearchFilter setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public Boolean getSuccess() {
        return success;
    }

    public RequestLogSearchFilter setSuccess(Boolean success) {
        this.success = success;
        return this;
    }
}
