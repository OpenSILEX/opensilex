//******************************************************************************
//                                       VectorDTO.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 16 mai 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  16 mai 2018
// Subject: definition of a vector, view by the web service client
//******************************************************************************
package opensilex.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Email;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resources.validation.interfaces.Required;
import opensilex.service.resources.dto.manager.AbstractVerifiedClass;
import opensilex.service.resources.validation.interfaces.Date;
import opensilex.service.resources.validation.interfaces.URL;
import opensilex.service.view.model.Vector;

/**
 * corresponds to the submitted JSON for a vector
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VectorDTO extends AbstractVerifiedClass {

    //uri of the vector
    private String uri;
    //type of the vector. Uri of the concept (must be subclass of Vector)
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2018#UAV)
    private String rdfType;
    //label of the vector
    private String label;
    //brand of the vector
    private String brand;
    //serial number of the vector
    private String serialNumber;
    //in service date of the vector
    private String inServiceDate;
    //date of purchase of the vector
    private String dateOfPurchase;
    //email of the person in charge of the vector
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
