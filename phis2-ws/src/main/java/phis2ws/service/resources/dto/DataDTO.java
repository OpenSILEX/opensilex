//******************************************************************************
//                                       DataDTO.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 12 janv. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  12 janv. 2018
// Subject: Represents the JSON submitted for the data
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Data;

/**
 * corresponds to the submitted JSON for the data 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DataDTO extends AbstractVerifiedClass {
    
    private String agronomicalObject;
    private String date;
    private String value;

    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("agronomicalObject", Boolean.TRUE);
        rules.put("data", Boolean.TRUE);
        rules.put("value", Boolean.TRUE);
        return rules;
    }

    @Override
    public Data createObjectFromDTO() {
        Data data = new Data();
        data.setAgronomicalObject(agronomicalObject);
        data.setDate(date);
        data.setValue(value);
        return data;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_AGRONOMICAL_OBJECT_URI)
    public String getAgronomicalObject() {
        return agronomicalObject;
    }

    public void setAgronomicalObject(String agronomicalObject) {
        this.agronomicalObject = agronomicalObject;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATETIME)
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATA_VALUE)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
