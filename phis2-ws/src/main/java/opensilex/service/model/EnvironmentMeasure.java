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
    //The uri of the sensor which has provide the measured value.
    //e.g. http://www.phenome-fppn.fr/mtp/2018/s18003
    protected String sensorUri;
    //The uri of the measured variable
    //e.g. http://www.phenome-fppn.fr/mtp/id/variables/v002
    protected String variableUri;
    //The date corresponding to the given value. The format should be yyyy-MM-ddTHH:mm:ssZ
    //e.g. 2018-06-25T15:13:59+0200
    protected Date date;
    //The measured value.
    //e.g. 1.2
    //SILEX:info
    //We use BigDecimal here because this value represent scientific data, so we need to keep the exact precision
    //float or double type are subject to rounded errors: @see https://floating-point-gui.de/basic/
    //\SILEX:info
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
