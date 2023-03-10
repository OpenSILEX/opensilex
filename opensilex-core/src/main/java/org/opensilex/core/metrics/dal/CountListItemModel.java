//******************************************************************************
//                          CountListItemModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.metrics.api.CountItemDTO;

/**
 * Represents a basic metric item list
 * @author Arnaud Charleroy
 */
public class CountListItemModel {
    
    private String name;
    
    private URI type;

    private Integer calculatedTotalCount = null;

    //private Integer countDifference = null; //difference between two CountItemModel counts

    private List<CountItemModel> items = new ArrayList<>();

    //private List<CountItemModel> diffItems = new ArrayList<>(); //items which are only present in one of two CountItemModels

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }


    public void setCalculatedTotalCount(Integer calculatedTotalCount) {
        this.calculatedTotalCount = calculatedTotalCount;
    }

   /* public Integer getCountDifference() {
        return countDifference;
    }

    public Integer setCountDifference(Integer countDifference) {
        this.countDifference = countDifference;
        return countDifference;
    }*/

    public Integer getTotalCount() {
        if(calculatedTotalCount != null){
            return  calculatedTotalCount;
        }
        int totalCount = 0;
        
        for (CountItemModel item : items) {
            totalCount  = totalCount + item.getCount();
        }
        return totalCount;
    } 

    public List<CountItemModel> getItems() {
        return items;
    }

    public void addItem(CountItemModel item) {
        this.items.add(item);
    }

    public void setItems(List<CountItemModel> items) {
        this.items = items;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*public List<CountItemModel> getDifferenceItems() {
        return diffItems;
    }

    public void setDifferenceItems(List<CountItemModel> items) {
        this.diffItems = items;
    }

    public void addDifferenceItem(CountItemModel item) {
        this.diffItems.add(item);
    }*/

    public static CountItemDTO getDTOFromModel(CountItemModel countItemModel) {
        CountItemDTO countItemDTO = new CountItemDTO();
        countItemDTO.setName(countItemModel.getName());
        countItemDTO.setUri(countItemModel.getUri());
        countItemDTO.setCount(countItemModel.getCount());
        countItemDTO.setType(countItemModel.getType());
        return countItemDTO;
    }

}
