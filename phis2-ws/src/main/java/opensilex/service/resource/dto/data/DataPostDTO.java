//******************************************************************************
//                                DataPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 1 March 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.text.ParseException;
import javax.validation.constraints.NotNull;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Data;
import org.apache.commons.validator.routines.BigDecimalValidator;

/**
 * Data POST DTO.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class DataPostDTO extends AbstractVerifiedClass {
    
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
     * The measured value.
     * @example 1.2
     */
    protected Object value;

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI)
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

    @Date({DateFormat.YMDTHMSZ, DateFormat.YMD})
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
    public Data createObjectFromDTO() throws ParseException {
        Data data = new Data();

        data.setObjectUri(objectUri);
        data.setVariableUri(variableUri);
        data.setProvenanceUri(provenanceUri);

        data.setDate(DateFormat.parseDateOrDateTime(date, false));

        String stringValue = value.toString();
        
        java.util.Date dateValue = getDateOrNullIfInvalid(stringValue);
        if (dateValue != null) {
             data.setValue(dateValue);
        } else {
            BigDecimal decimalValue = getBigDecimalOrNullIfInvalid(stringValue);
            if (decimalValue != null) {
                data.setValue(decimalValue);
            } else {
                data.setValue(value);
            }
        }

        return data;
    }
    
    /**
     * Return Date object from string value or null if it's not a date
     * @param value
     * @return Date or null
     */
    private java.util.Date getDateOrNullIfInvalid(String value) {
        try {
            return DateFormat.parseDateOrDateTime(value, false);
        } catch (ParseException ex) {
            return null;
        }
    }
    
    /**
     * Return BigDecimal object from string value or null if it's not a valid number
     * @param value
     * @return BigDecimal or null
     */
    private BigDecimal getBigDecimalOrNullIfInvalid(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
