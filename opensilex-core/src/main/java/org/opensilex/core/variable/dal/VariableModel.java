//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;


@SPARQLResource(
        ontology = Oeso.class,
        resource = "Variable",
        graph = VariableModel.GRAPH,
        ignoreValidation = true
)
public class VariableModel extends BaseVariableModel<VariableModel> implements ClassURIGenerator<VariableModel> {

    public static final String GRAPH = "variable";

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    private String alternativeName;
    public static final String ALTERNATIVE_NAME_FIELD_NAME = "alternativeName";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasEntity",
            required = true
    )
    private EntityModel entity;
    public static final String ENTITY_FIELD_NAME = "entity";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasEntityOfInterest"
    )
    private InterestEntityModel entityOfInterest;
    public static final String ENTITY_OF_INTEREST_FIELD_NAME = "entityOfInterest";
            
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCharacteristic",
            required = true
    )
    private CharacteristicModel characteristic;
    public static final String CHARACTERISTIC_FIELD_NAME = "characteristic";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTraitUri"
    )
    private URI traitUri;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTraitName"
    )
    private String traitName;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasMethod",
            required = true
    )
    private MethodModel method;
    public static final String METHOD_FIELD_NAME = "method";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasUnit",
            required = true
    )
    private UnitModel unit;
    public static final String UNIT_FIELD_NAME = "unit";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTimeInterval"
    )
    private String timeInterval;

    @SPARQLProperty(
        ontology = Oeso.class,
        property = "hasSamplingInterval"
    )
    private String samplingInterval;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDataType",
            required = true
    )
    private URI dataType;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasSpecies"
    )
    private SpeciesModel species;

    public String getAlternativeName() { return alternativeName; }

    public void setAlternativeName(String alternativeName) { this.alternativeName = alternativeName; }

    public EntityModel getEntity() {
        return entity;
    }

    public void setEntity(EntityModel entity) {
        this.entity = entity;
    }
    
    public InterestEntityModel getEntityOfInterest(){
        return entityOfInterest;
    }
    
    public void setEntityOfInterest(InterestEntityModel entityOfInterest){
        this.entityOfInterest = entityOfInterest;
    }
    
    public CharacteristicModel getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(CharacteristicModel characteristic) {
        this.characteristic = characteristic;
    }

    public URI getTraitUri() { return traitUri; }

    public void setTraitUri(URI traitUri) { this.traitUri = traitUri; }

    public String getTraitName() { return traitName; }

    public void setTraitName(String traitName) { this.traitName = traitName; }

    public MethodModel getMethod() {
        return method;
    }

    public void setMethod(MethodModel method) {
        this.method = method;
    }

    public UnitModel getUnit() {
        return unit;
    }

    public void setUnit(UnitModel unit) {
        this.unit = unit;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    public String getSamplingInterval() { return samplingInterval; }

    public void setSamplingInterval(String samplingInterval) { this.samplingInterval = samplingInterval; }

    public URI getDataType() { return dataType; }

    public void setDataType(URI dataType) { this.dataType = dataType; }

    public SpeciesModel getSpecies() {
        return species;
    }

    public void setSpecies(SpeciesModel species) {
        this.species = species;
    }

}

