package org.opensilex.core.variable.api.dimension;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.dal.DimensionModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.net.URISyntaxException;

@JsonPropertyOrder({
        "uri", "name", "description", "datatype",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public class DimensionDetailsDTO extends BaseVariableDetailsDTO<DimensionModel> {

    @JsonProperty("datatype")
    private URI dataType;

    public DimensionDetailsDTO() {

    }

    public DimensionDetailsDTO(DimensionModel model) {
        super(model);
        if (model.getDataType() != null) {
            try {
                this.dataType = new URI(SPARQLDeserializers.getExpandedURI(model.getDataType()));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public URI getDataType() {
        return dataType;
    }

    public void setDataType(URI dataType) {
        this.dataType = dataType;
    }
}