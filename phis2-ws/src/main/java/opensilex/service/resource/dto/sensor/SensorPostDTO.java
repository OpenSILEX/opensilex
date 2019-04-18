//******************************************************************************
//                                       SensorPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 11 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.sensor;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Email;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.Sensor;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;

/**
 * DTO for the POST sensor service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorPostDTO extends AbstractVerifiedClass {
    //type of the sensor. Uri of the concept (must be subclass of SensingDevice concept)
    private String rdfType;
    //label of the sensor
    private String label;
    //brand of the sensor
    private String brand;
    //model of the sensor
    private String model;
    //serial number of the sensor
    private String serialNumber;
    //in service date of the sensor
    private String inServiceDate;
    //purchase date of the sensor
    private String dateOfPurchase;
    //date of last calibration of the sensor
    private String dateOfLastCalibration;
    //email of the person in charge of the sensor
    private String personInCharge;

    public SensorPostDTO(Sensor sensor) {
        rdfType = sensor.getRdfType();
        label = sensor.getLabel();
        brand = sensor.getBrand();
        model = sensor.getModel();
        serialNumber = sensor.getSerialNumber();
        inServiceDate = sensor.getInServiceDate();
        dateOfPurchase = sensor.getDateOfPurchase();
        dateOfLastCalibration = sensor.getDateOfLastCalibration();
        personInCharge = sensor.getPersonInCharge();
    }
    
    @Override
    public Sensor createObjectFromDTO() {
        Sensor sensor = new Sensor();
        sensor.setRdfType(rdfType);
        sensor.setLabel(label);
        sensor.setBrand(brand);
        sensor.setModel(model);
        sensor.setSerialNumber(serialNumber);
        sensor.setInServiceDate(inServiceDate);
        sensor.setDateOfPurchase(dateOfPurchase);
        sensor.setDateOfLastCalibration(dateOfLastCalibration);
        sensor.setPersonInCharge(personInCharge);

        return sensor;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_RDF_TYPE)
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
    @Required
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