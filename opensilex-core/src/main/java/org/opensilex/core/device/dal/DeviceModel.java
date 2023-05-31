//******************************************************************************
//                          DeviceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.device.dal;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

import java.time.LocalDate;
import java.util.Map;

/**
 * @author sammy
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Device",
        graph = DeviceModel.GRAPH,
        prefix = "device"
)
public class DeviceModel extends SPARQLTreeModel<DeviceModel> {

    public static final String GRAPH = "device";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasBrand"
    )
    String brand;
    public static final String BRAND_FIELD = "brand";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasModel"
    )
    String model;
    public static final String MODEL_FIELD = "model";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasSerialNumber"
    )
    String serialNumber;
    public static final String SERIALNUMBER_FIELD = "serialNumber";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "personInCharge"
    )
    PersonModel personInCharge;
    public static final String PERSON_IN_CHARGE_FIELD = "personInCharge";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "startUp"
    )
    LocalDate startUp;
    public static final String STARTUP_FIELD = "startUp";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "removal"
    )
    LocalDate removal;
    public static final String REMOVAL_FIELD = "removal";
    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String description;
    public static final String COMMENT_FIELD = "description";
    
    Map<String, String> attributes;
    
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public PersonModel getPersonInCharge() {
        return personInCharge;
    }

    public void setPersonInCharge(PersonModel personInCharge) {
        this.personInCharge = personInCharge;
    }
    
    public LocalDate getStartUp() {
        return startUp;
    }

    public void setStartUp(LocalDate startUp) {
        this.startUp = startUp;
    }
    
    public LocalDate getRemoval() {
        return removal;
    }

    public void setRemoval(LocalDate removal) {
        this.removal = removal;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getInstanceUriPath(SPARQLTreeModel<DeviceModel> instance) {

        if(! StringUtils.isEmpty(instance.getName())){
            return normalize(instance.getName());
        }
        return RandomStringUtils.randomAlphabetic(8);
    }
}
