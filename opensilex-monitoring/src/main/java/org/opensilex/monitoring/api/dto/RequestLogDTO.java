//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.monitoring.log.dal.RequestLogModel;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * One recorded call.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class RequestLogDTO {

    @ApiModelProperty(value = "when the call started")
    @JsonProperty("start_time")
    private Instant startTime;

    @ApiModelProperty(value = "calling account, null when anonymous")
    @JsonProperty("account")
    private URI account;

    @ApiModelProperty(value = "email of the calling account")
    @JsonProperty("account_email")
    private String accountEmail;

    @ApiModelProperty(value = "HTTP method")
    @JsonProperty("http_method")
    private String httpMethod;

    @ApiModelProperty(value = "path without the query string")
    @JsonProperty("path")
    private String path;

    @ApiModelProperty(value = "matched resource method")
    @JsonProperty("service")
    private String service;

    @ApiModelProperty(value = "HTTP status of the response")
    @JsonProperty("status")
    private Integer status;

    @ApiModelProperty(value = "true when the call succeeded")
    @JsonProperty("success")
    private boolean success;

    @ApiModelProperty(value = "how long the call took, null when duration recording is off")
    @JsonProperty("duration_ms")
    private Long durationMs;

    @ApiModelProperty(value = "query parameters, only for GET calls and only when enabled")
    @JsonProperty("query_parameters")
    private Map<String, Object> queryParameters;

    @ApiModelProperty(value = "exception class name, when the call ended in one")
    @JsonProperty("error_type")
    private String errorType;

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

    public Map<String, Object> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(Map<String, Object> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public static RequestLogDTO fromModel(RequestLogModel model) {
        RequestLogDTO dto = new RequestLogDTO();

        dto.setStartTime(model.getStartTime());
        dto.setAccount(model.getAccount());
        dto.setAccountEmail(model.getAccountEmail());
        dto.setHttpMethod(model.getHttpMethod());
        dto.setPath(model.getPath());
        dto.setService(model.getService());
        dto.setStatus(model.getStatus());
        dto.setSuccess(model.isSuccess());
        dto.setDurationMs(model.getDurationMs());
        dto.setErrorType(model.getErrorType());
        dto.setQueryParameters(model.getQueryParameters());
        return dto;
    }

}
