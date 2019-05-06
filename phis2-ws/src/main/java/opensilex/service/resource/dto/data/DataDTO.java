//******************************************************************************
//                                       DataDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 1 March 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import opensilex.service.configuration.DateFormat;
import opensilex.service.model.Data;
import org.bson.types.Decimal128;

/**
 * Data DTO.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class DataDTO {
    
    /**
     * URI of the data.
     * @example http://www.opensilex.org/1e9eb2fbacc7222d3868ae96149a8a16b32b2a1870c67d753376381ebcbb5937/e78da502-ee3f-42d3-828e-aa8cab237f93
     */
    protected String uri;
    
    /**
     * URI of the provenance from which data come.
     * @example http://www.phenome-fppn.fr/mtp/2018/s18003
     */
    protected String provenanceUri;
    
    /**
     * URI of the scientific object on which data is related.
     * @example http://www.phenome-fppn.fr/mtp/2018/s18003
     */
    protected String objectUri;
    
    /**
     * URI of the measured variable
     * @example http://www.phenome-fppn.fr/mtp/id/variables/v002
     */
    protected String variableUri;
    
    /**
     * Date corresponding to the given value. The format should be yyyy-MM-ddTHH:mm:ssZ
     * @example 2018-06-25T15:13:59+0200
     */
    protected String date;
    
    /**
     * The data value.
     * @example 1.2
     */
    protected Object value;

    public DataDTO(Data data) {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        
        if (data.getDate() != null) {
            setDate(df.format(data.getDate()));
        }
        
        setUri(data.getUri());
        setProvenanceUri(data.getProvenanceUri());
        setVariableUri(data.getVariableUri());
        setObjectUri(data.getObjectUri());
        
        Object dataValue = data.getValue();
        if (dataValue != null && dataValue instanceof Date) {
            setValue(df.format(dataValue));
        } else if (dataValue != null && dataValue instanceof Decimal128) {
            setValue(new BigDecimal(dataValue.toString()));
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
    
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
