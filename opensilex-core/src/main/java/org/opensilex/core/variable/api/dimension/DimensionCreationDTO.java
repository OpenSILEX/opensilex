package org.opensilex.core.variable.api.dimension;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.dal.DimensionModel;

import java.net.URI;

@JsonPropertyOrder({
        "uri", "name", "description", "datatype",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public class DimensionCreationDTO extends BaseVariableCreationDTO<DimensionModel> {

    @JsonProperty("datatype")
    private URI datatype;

    @Override
    protected DimensionModel newModelInstance() {
        DimensionModel model = new DimensionModel();
        model.setDataType(this.datatype);
        return model;
    }

    public URI getDatatype() {
        return datatype;
    }

    public void setDatatype(URI datatype) {
        this.datatype = datatype;
    }
}