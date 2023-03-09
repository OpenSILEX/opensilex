package org.opensilex.core.metrics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bernhard Gschloessl
 */


@JsonPropertyOrder({"total_difference_item_count", "difference_items"})
public class CountListItemPeriodDTO extends CountListItemDTO {


    private Integer totalDiffItemCount;

    private List<CountItemPeriodDTO> diffItems; //items which are only present in one of two CountItemModels
    @JsonProperty("difference_items")
    public List<CountItemPeriodDTO> getDifferenceItems() {
        return diffItems;
    }

    public void setDifferenceItems(List<CountItemPeriodDTO> diffItems) {
        this.diffItems = diffItems;
    }

    public void addDifferenceItem(CountItemPeriodDTO diffItem) {
        if (this.diffItems == null){
            this.diffItems = new ArrayList<>();
        }
        this.diffItems.add(diffItem);
    }

    @JsonProperty("total_difference_item_count")
    public Integer getTotalDifferenceItemsCount() {
        return totalDiffItemCount;
    }

    public void setTotalDifferenceItemsCount(Integer totalDiffItemCount) {
        this.totalDiffItemCount = totalDiffItemCount;
    }
}
