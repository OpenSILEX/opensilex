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
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.view.model.phis.Data;
import phis2ws.service.resources.validation.interfaces.URL;

/**
 * corresponds to the submitted JSON for the data 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DataDTO extends AbstractVerifiedClass {
    
    private String agronomicalObject;
    private String date;
    private String value;
    private String sensor;
    private String incertitude;

    @Override
    public Data createObjectFromDTO() {
        Data data = new Data();
        data.setAgronomicalObject(agronomicalObject);
        data.setDate(date);
        data.setValue(value);
        data.setSensor(sensor);
        data.setIncertitude(incertitude);
        
        return data;
    }
    
    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI)
    public String getAgronomicalObject() {
        return agronomicalObject;
    }

    public void setAgronomicalObject(String agronomicalObject) {
        this.agronomicalObject = agronomicalObject;
    }

    @Date(DateFormat.YMD)
    @Required
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

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_URI)
    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATA_INCERTITUDE)
    public String getIncertitude() {
        return incertitude;
    }

    public void setIncertitude(String incertitude) {
        this.incertitude = incertitude;
    }
}
