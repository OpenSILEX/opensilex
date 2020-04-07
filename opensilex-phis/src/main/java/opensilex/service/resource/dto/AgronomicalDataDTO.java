//******************************************************************************
//                           AgronomicalDataDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 Jan. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.model.AgronomicalData;
import opensilex.service.resource.validation.interfaces.URL;

/**
 * Agronomical data DTO. 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class AgronomicalDataDTO extends AbstractVerifiedClass {
    
    private String agronomicalObject;
    private String date;
    private String value;
    private String sensor;
    private String incertitude;

    @Override
    public AgronomicalData createObjectFromDTO() {
        AgronomicalData data = new AgronomicalData();
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
