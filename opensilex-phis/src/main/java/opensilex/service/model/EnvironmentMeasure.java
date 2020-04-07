//******************************************************************************
//                               EnvironmentMeasure.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 Oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Environmental measure model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class EnvironmentMeasure {
    /**
     * URI of the sensor which provided the measured value.
     * @example http://www.phenome-fppn.fr/mtp/2018/s18003
     */
    protected String sensorUri;
    
    /**
     * URI of the measured variable.
     * @example http://www.phenome-fppn.fr/mtp/id/variables/v002
     */
    protected String variableUri;
    
    /** 
     * Value date. The format should be yyyy-MM-ddTHH:mm:ssZ.
     *  @example 2018-06-25T15:13:59+0200
     */
    protected Date date;
    
    /** 
     * The measured value.
     * @example  1.2
     * //SILEX:info
     * We use BigDecimal here because this value represent scientific data, so 
     * we need to keep the exact precision float or double type are subject to 
     * errors of rounding 
     * @see https://floating-point-gui.de/basic/
     * //\SILEX:info
     */
    protected BigDecimal value;

    public String getSensorUri() {
        return sensorUri;
    }

    public void setSensorUri(String sensorUri) {
        this.sensorUri = sensorUri;
    }

    public String getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(String variableUri) {
        this.variableUri = variableUri;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
