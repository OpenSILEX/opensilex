//******************************************************************************
//                          DeviceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.device.dal;

import java.time.LocalDate;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

/**
 * @author sammy
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Device",
        graph = "set/device",
        prefix = "device"
)
public class DeviceModel extends SPARQLTreeModel<DeviceModel> {
     @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf"
    )
    protected DeviceModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected List<DeviceModel> children;
    
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
    URI personInCharge;
    public static final String PERSONINCHARGE_FIELD = "personInCharge";
    
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
    public static final String DATEOFLASTUSE_FIELD = "removal";
    
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
    
    public URI getPersonInCharge() {
        return personInCharge;
    }

    public void setPersonInCharge(URI personInCharge) {
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
}
