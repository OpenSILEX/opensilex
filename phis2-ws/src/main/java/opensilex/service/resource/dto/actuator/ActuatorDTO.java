//******************************************************************************
//                                       ActuatorDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 17 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.actuator;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Email;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.Actuator;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;

/**
 * DTO for the GET actuator.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ActuatorDTO extends AbstractVerifiedClass {
    //The URI of the actuator.
    private String uri;
    //type of the actuator. Uri of the concept (must be subclass of Actuator concept)
    private String rdfType;
    //label of the actuator
    private String label;
    //brand of the actuator
    private String brand;
    //model of the actuator
    private String model;
    //serial number of the actuator
    private String serialNumber;
    //in service date of the actuator
    private String inServiceDate;
    //purchase date of the actuator
    private String dateOfPurchase;
    //date of last calibration of the actuator
    private String dateOfLastCalibration;
    //email of the person in charge of the actuator
    private String personInCharge;

    public ActuatorDTO(Actuator actuator) {
        uri = actuator.getUri();
        rdfType = actuator.getRdfType();
        label = actuator.getLabel();
        brand = actuator.getBrand();
        serialNumber = actuator.getSerialNumber();
        model = actuator.getModel();
        inServiceDate = actuator.getInServiceDate();
        dateOfPurchase = actuator.getDateOfPurchase();
        dateOfLastCalibration = actuator.getDateOfLastCalibration();
        personInCharge = actuator.getPersonInCharge();
    }

    @Override
    public Actuator createObjectFromDTO() {
        Actuator actuator = new Actuator();
        actuator.setUri(uri);
        actuator.setRdfType(rdfType);
        actuator.setLabel(label);
        actuator.setBrand(brand);
        actuator.setModel(model);
        actuator.setSerialNumber(serialNumber);
        actuator.setInServiceDate(inServiceDate);
        actuator.setDateOfPurchase(dateOfPurchase);
        actuator.setDateOfLastCalibration(dateOfLastCalibration);
        actuator.setPersonInCharge(personInCharge);

        return actuator;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ACTUATOR_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ACTUATOR_RDF_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_LABEL)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_BRAND)
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_SERIAL_NUMBER)
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Date(DateFormat.YMD)
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_IN_SERVICE_DATE)
    public String getInServiceDate() {
        return inServiceDate;
    }

    public void setInServiceDate(String inServiceDate) {
        this.inServiceDate = inServiceDate;
    }

    @Date(DateFormat.YMD)
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_PURCHASE)
    public String getDateOfPurchase() {
        return dateOfPurchase;
    }

    public void setDateOfPurchase(String dateOfPurchase) {
        this.dateOfPurchase = dateOfPurchase;
    }

    @Date(DateFormat.YMD)
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_DATE_OF_LAST_CALIBRATION)
    public String getDateOfLastCalibration() {
        return dateOfLastCalibration;
    }

    public void setDateOfLastCalibration(String dateOfLastCalibration) {
        this.dateOfLastCalibration = dateOfLastCalibration;
    }
    
    @Email
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_EMAIL)
    public String getPersonInCharge() {
        return personInCharge;
    }

    public void setPersonInCharge(String personInCharge) {
        this.personInCharge = personInCharge;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_MODEL)
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
