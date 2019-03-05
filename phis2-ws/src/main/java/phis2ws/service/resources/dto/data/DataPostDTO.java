//******************************************************************************
//                                       DataPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 1 March 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.data;

import io.swagger.annotations.ApiModelProperty;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.validation.constraints.NotNull;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.Data;

/**
 * Represents the exchange format used to insert data in post service.
 */
public class DataPostDTO extends AbstractVerifiedClass {
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
    //The measured value.
    //e.g. 1.2
    protected Object value;
    
    public Data createObjectFromDTOWithException() throws ParseException {
        Data data = new Data();
        data.setObjectUri(objectUri);
        data.setVariableUri(variableUri);
        data.setProvenanceUri(provenanceUri);
        
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        data.setDate(df.parse(date));
        
        try {
            data.setValue(df.parse(value.toString()));
        } catch (ParseException ex) {
            data.setValue(value);
        }
        
        return data;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_URI)
    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
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
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI)
    public String getProvenanceUri() {
        return provenanceUri;
    }

    public void setProvenanceUri(String provenanceUri) {
        this.provenanceUri = provenanceUri;
    }

    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
