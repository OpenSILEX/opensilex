package org.opensilex.core.variable.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Dimension",
        graph = VariableModel.GRAPH
)
public class DimensionModel extends BaseVariableModel<DimensionModel> {

    public static final String GRAPH = "dimension";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDataType",
            required = true
    )
    private URI dataType;
    public static final String DATA_TYPE_FIELD_NAME = "dataType";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasUnit"
    )
    private UnitModel unit;
    public static final String UNIT_FIELD_NAME = "unit";

    public DimensionModel() {
    }

    public DimensionModel(URI uri) {
        setUri(uri);
    }

    @Override
    public String[] getInstancePathSegments(DimensionModel instance) {
        return new String[]{
                "dimension",
                instance.getName()
        };
    }

    public URI getDataType() {
        return dataType;
    }

    public void setDataType(URI dataType) {
        this.dataType = dataType;
    }

    public UnitModel getUnit() {
        return unit;
    }

    public void setUnit(UnitModel unit) {
        this.unit = unit;
    }
}