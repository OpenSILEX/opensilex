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
            property = "obtained"
    )
    LocalDate obtained;
    public static final String OBTAINED_FIELD = "obtained";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "dateOfLastUse"
    )
    LocalDate dateOfLastUse;
    public static final String DATEOFLASTUSE_FIELD = "dateOfLastUse";
    
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
    
    public LocalDate getObtained() {
        return obtained;
    }

    public void setObtained(LocalDate obtained) {
        this.obtained = obtained;
    }
    
    public LocalDate getDateOfLastUse() {
        return dateOfLastUse;
    }

    public void setDateOfLastUse(LocalDate dateOfLastUse) {
        this.dateOfLastUse = dateOfLastUse;
    }
}
