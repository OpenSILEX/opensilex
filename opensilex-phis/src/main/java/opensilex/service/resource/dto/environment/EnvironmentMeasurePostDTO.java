//******************************************************************************
//                          EnvironmentMeasurePostDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 29 Oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.environment;

import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.constraints.NotNull;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.EnvironmentMeasure;

/**
 * Environmental measure POST DTO.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class EnvironmentMeasurePostDTO extends AbstractVerifiedClass {
    
    /**
     * URI of the sensor which has provide the measured value.
     * @example http://www.phenome-fppn.fr/mtp/2018/s18003
     */
    protected String sensorUri;
    
    /**
     * URI of the measured variable.
     * @example http://www.phenome-fppn.fr/mtp/id/variables/v002
     */
    protected String variableUri;
    
    /**
     * Date corresponding to the given value. The format should be yyyy-MM-ddTHH:mm:ssZ.
     * @example 2018-06-25T15:13:59+0200
     */
    protected String date;
    
    /**
     * Measured value.
     * @example 1.2
     */
    protected BigDecimal value;
    
    @Override
    public EnvironmentMeasure createObjectFromDTO() {
        EnvironmentMeasure environment = new EnvironmentMeasure();
        environment.setSensorUri(sensorUri);
        environment.setVariableUri(variableUri);
        environment.setValue(value);
        
        try {
            SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
            environment.setDate(df.parse(date));
        } catch (ParseException ex) {
            Logger.getLogger(EnvironmentMeasurePostDTO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return environment;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_URI)
    public String getSensorUri() {
        return sensorUri;
    }

    public void setSensorUri(String sensorUri) {
        this.sensorUri = sensorUri;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI)
    public String getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(String variableUri) {
        this.variableUri = variableUri;
    }

    @Date(DateFormat.YMDTHMSZ)
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_XSDDATETIME)
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @NotNull
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ENVIRONMENT_VALUE)
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
