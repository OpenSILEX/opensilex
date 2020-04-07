//******************************************************************************
//                           EnvironmentMeasureDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 Nov. 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.environment;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import opensilex.service.configuration.DateFormat;
import opensilex.service.model.EnvironmentMeasure;

/**
 * Environmental measure DTO.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class EnvironmentMeasureDTO {

    /**
     * URI of the sensor which has provide the measured value.
     * @example http://www.phenome-fppn.fr/mtp/2018/s18003
     */
    protected String sensorUri;
    
    /**
     * URI of the variable corresponding to the value.
     * @example http://www.opensilex.org/demo/id/variables/v004
     */
    protected String variableUri;
    
    /**
     * Date corresponding to the given value. 
     * The format should be yyyy-MM-ddTHH:mm:ssZ.
     * @example 2018-06-25T15:13:59+0200
     */
    protected String date;
    
    /**
     * Measured value.
     * @example 1.2
     */
    protected BigDecimal value;
    
    public EnvironmentMeasureDTO(EnvironmentMeasure measure) {
        if (measure.getDate() != null) {
            SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
            setDate(df.format(measure.getDate()));
        }
        
        setSensorUri(measure.getSensorUri());
        setVariableUri(measure.getVariableUri());
        setValue(measure.getValue());
    }

    public String getSensorUri() {
        return sensorUri;
    }

    public void setSensorUri(String sensorUri) {
        this.sensorUri = sensorUri;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(String variableUri) {
        this.variableUri = variableUri;
    }
}
