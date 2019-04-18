//******************************************************************************
//                                VectorDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 16 May 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Email;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Vector;

/**
 * Vector DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VectorDTO extends AbstractVerifiedClass {

    /**
     * URI of the vector.
     */
    private String uri;
    
    /**
     * Type URI of the vector.
     * @example http://www.phenome-fppn.fr/vocabulary/2018#UAV)
     */
    private String rdfType;
    
    /**
     * Label.
     */
    private String label;
    
    /**
     * Brand.
     */
    private String brand;
    
    /**
     * Serial number.
     */
    private String serialNumber;
    
    /**
     * Commissioning date.
     */
    private String inServiceDate;
    
    /**
     * Purchase date.
     */
    private String dateOfPurchase;
    
    /**
     * Email of the person in charge.
     */
    private String personInCharge;

    @Override
    public Vector createObjectFromDTO() {
        Vector vector = new Vector();
        vector.setUri(uri);
        vector.setRdfType(rdfType);
        vector.setLabel(label);
        vector.setBrand(brand);
        vector.setSerialNumber(serialNumber);
        vector.setInServiceDate(inServiceDate);
        vector.setDateOfPurchase(dateOfPurchase);
        vector.setPersonInCharge(personInCharge);

        return vector;
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_RDF_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_LABEL)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_BRAND)
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_SERIAL_NUMBER)
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Date(DateFormat.YMD)
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_IN_SERVICE_DATE)
    public String getInServiceDate() {
        return inServiceDate;
    }

    public void setInServiceDate(String inServiceDate) {
        this.inServiceDate = inServiceDate;
    }

    @Date(DateFormat.YMD)
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_DATE_OF_PURCHASE)
    public String getDateOfPurchase() {
        return dateOfPurchase;
    }

    public void setDateOfPurchase(String dateOfPurchase) {
        this.dateOfPurchase = dateOfPurchase;
    }

    @Email
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_PERSON_IN_CHARGE)
    public String getPersonInCharge() {
        return personInCharge;
    }

    public void setPersonInCharge(String personInCharge) {
        this.personInCharge = personInCharge;
    }
}
