//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.Instant;
import java.util.List;

/**
 * Activity over a period, plus the period wide failure percentage.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class ActivityReportDTO {

    @ApiModelProperty(value = "inclusive lower bound actually used")
    @JsonProperty("start_date")
    private Instant startDate;

    @ApiModelProperty(value = "exclusive upper bound actually used")
    @JsonProperty("end_date")
    private Instant endDate;

    @ApiModelProperty(value = "bucket size actually applied, which may be coarser than the one requested")
    @JsonProperty("granularity")
    private String granularity;

    @ApiModelProperty(value = "true when the requested granularity had to be coarsened to stay under the bucket cap")
    @JsonProperty("truncated")
    private boolean truncated;

    @ApiModelProperty(value = "the series, one entry per non empty bucket")
    @JsonProperty("buckets")
    private List<ActivityBucketDTO> buckets;

    @ApiModelProperty(value = "calls over the whole period")
    @JsonProperty("total_requests")
    private long totalRequests;

    @ApiModelProperty(value = "4xx over the whole period")
    @JsonProperty("total_client_errors")
    private long totalClientErrors;

    @ApiModelProperty(value = "5xx over the whole period")
    @JsonProperty("total_server_errors")
    private long totalServerErrors;

    @ApiModelProperty(value = "share of failed calls over the whole period, computed on the totals rather than by averaging the buckets so that a quiet bucket does not weigh as much as a busy one")
    @JsonProperty("failure_percentage")
    private double failurePercentage;

    @ApiModelProperty(value = "distinct accounts over the whole period, which is not the sum of the per bucket counts")
    @JsonProperty("distinct_users")
    private long distinctUsers;

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public List<ActivityBucketDTO> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<ActivityBucketDTO> buckets) {
        this.buckets = buckets;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getTotalClientErrors() {
        return totalClientErrors;
    }

    public void setTotalClientErrors(long totalClientErrors) {
        this.totalClientErrors = totalClientErrors;
    }

    public long getTotalServerErrors() {
        return totalServerErrors;
    }

    public void setTotalServerErrors(long totalServerErrors) {
        this.totalServerErrors = totalServerErrors;
    }

    public double getFailurePercentage() {
        return failurePercentage;
    }

    public void setFailurePercentage(double failurePercentage) {
        this.failurePercentage = failurePercentage;
    }

    public long getDistinctUsers() {
        return distinctUsers;
    }

    public void setDistinctUsers(long distinctUsers) {
        this.distinctUsers = distinctUsers;
    }

}
