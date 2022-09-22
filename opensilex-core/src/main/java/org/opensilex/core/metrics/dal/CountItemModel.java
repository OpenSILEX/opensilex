//******************************************************************************
//                          CountItemModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.dal;

import java.net.URI; 
import org.opensilex.core.metrics.api.CountItemDTO;

/**
 * Represents a generic metric single item
 * @author Arnaud Charleroy
 */
public class CountItemModel {
    
    private URI uri;
    
    private String name;
    
    private Integer count; 
    
    private URI type;

    public CountItemModel() {
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }
    
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
    
    public static CountItemDTO getDTOFromModel(CountItemModel countItemModel){
        CountItemDTO countItemDTO = new CountItemDTO();
        countItemDTO.setName(countItemModel.getName());
        countItemDTO.setUri(countItemModel.getUri());
        countItemDTO.setCount(countItemModel.getCount());
         countItemDTO.setType(countItemModel.getType());
        return countItemDTO;
    }
}
