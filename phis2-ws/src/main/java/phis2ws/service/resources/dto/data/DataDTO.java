//******************************************************************************
//                                       DataDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 1 March 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.view.model.phis.Data;

/**
 * Represents the exchange format used to get data from generic service.
 */
public class DataDTO {

    //The uri of the provenance from which data come.
    //e.g. http://www.phenome-fppn.fr/mtp/2018/s18003
    protected String provenanceUri;
    //The uri of the scientific object on which data is related.
    //e.g. http://www.phenome-fppn.fr/mtp/2018/s18003
    protected String objectUri;
    //The uri of the measured variable
    //e.g. http://www.phenome-fppn.fr/mtp/id/variables/v002
    protected String variableUri;
    //The date corresponding to the given value. The format should be yyyy-MM-ddTHH:mm:ssZ
    //e.g. 2018-06-25T15:13:59+0200
    protected String date;
    //The data value.
    //e.g. 1.2
    protected Object value;

    public DataDTO(Data data) {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        
        if (data.getDate() != null) {
            setDate(df.format(data.getDate()));
        }
        setProvenanceUri(data.getProvenanceUri());
        setVariableUri(data.getVariableUri());
        setObjectUri(data.getObjectUri());
        
        Object dataValue = data.getValue();
        if (dataValue != null && dataValue instanceof Date) {
            setValue(df.format(dataValue));
        } else {
            setValue(dataValue);
        }
    }

    public String getProvenanceUri() {
        return provenanceUri;
    }

    public void setProvenanceUri(String provenanceUri) {
        this.provenanceUri = provenanceUri;
    }

    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    public String getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(String variableUri) {
        this.variableUri = variableUri;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
