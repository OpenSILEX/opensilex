package org.opensilex.core.metrics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"difference_count"})
public class CountItemPeriodDTO extends CountItemDTO {
    @JsonProperty("difference_count")
    private Integer diffCount;

    public Integer getDifferenceCount() {
        return diffCount;
    }

    public void setDifferenceCount(Integer diffCount) {
        this.diffCount = diffCount;
    }
}
