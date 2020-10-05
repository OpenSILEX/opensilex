//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;


@SPARQLResource(
        ontology = Oeso.class,
        resource = "Variable",
        graph = "set/variables",
        ignoreValidation = true
)
public class VariableModel extends BaseVariableModel<VariableModel> implements ClassURIGenerator<VariableModel> {

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "altLabel"
    )
    private String longName;
    public static final String LONG_NAME_FIELD_NAME = "longName";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasEntity",
            required = true
    )
    private EntityModel entity;
    public static final String ENTITY_FIELD_NAME = "entity";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasQuality",
            required = true
    )
    private QualityModel quality;
    public static final String QUALITY_FIELD_NAME = "quality";

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
            property = "hasMethod"
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
            property = "hasDataType"
    )
    private URI dataType;

    public String getLongName() { return longName; }

    public void setLongName(String longName) { this.longName = longName; }

    public EntityModel getEntity() {
        return entity;
    }

    public void setEntity(EntityModel entity) {
        this.entity = entity;
    }

    public QualityModel getQuality() {
        return quality;
    }

    public void setQuality(QualityModel quality) {
        this.quality = quality;
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

    @Override
    public String[] getUriSegments(VariableModel instance) {
        return new String[]{
            "variable",
            instance.getName()
        };
    }
}

