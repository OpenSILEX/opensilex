//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.opensilex.core.ontology.*;
import org.opensilex.sparql.annotations.*;
import org.opensilex.sparql.utils.*;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Variable",
        graph = "variable"
)
public class VariableModel extends BaseVariableModel implements ClassURIGenerator<VariableModel> {

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

    @Override
    public String[] getUriSegments(VariableModel instance) {
        return new String[]{
            "variable",
            instance.getName()
        };
    }
}
