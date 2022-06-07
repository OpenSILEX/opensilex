//******************************************************************************
//                          CountListItemDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.opensilex.core.metrics.dal.CountItemModel;
import org.opensilex.core.metrics.dal.CountListItemModel;

/**
 *
 * @author Arnaud Charleroy
 */
@JsonPropertyOrder({"uri", "type", "name", "total_items_count"})
public class CountListItemDTO {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private URI type;
    
    @JsonProperty("total_items_count")
    private Integer totalItemsCount;
    
    @JsonProperty("items")
    List<CountItemDTO> items;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public URI getType() {
        return type;
    }
    
    public void setType(URI type) {
        this.type = type;
    }    
    
    public List<CountItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<CountItemDTO> items) {        
        
        this.items = items;
    }
    
    public void addItem(CountItemDTO item) {
        this.items.add(item);        
    }    
    
    public Integer getTotalItemsCount() {
        return totalItemsCount;
    }
    
    public void setTotalItemsCount(Integer totalItemsCount) {
        this.totalItemsCount = totalItemsCount;
    }
    
    public static CountListItemDTO getDTOFromModel(CountListItemModel itemsModels) {
        CountListItemDTO countListItemDTO = new CountListItemDTO();        
        countListItemDTO.setItems(itemsModels.getItems().stream().map(CountItemModel::getDTOFromModel).collect(Collectors.toList()));
        countListItemDTO.setName(itemsModels.getName());
        countListItemDTO.setType(itemsModels.getType()); 
        countListItemDTO.setTotalItemsCount(itemsModels.getTotalCount());
        return countListItemDTO;
    }
    
}
