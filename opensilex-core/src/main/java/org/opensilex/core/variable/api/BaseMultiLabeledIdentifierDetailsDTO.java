package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.variable.dal.BaseMultiLabeledIdentifierModel;
import org.opensilex.core.variable.dal.BaseVariableModel;

import java.net.URI;
import java.time.OffsetDateTime;

@JsonPropertyOrder({
        "uri", "multiLabelDTO",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public abstract class BaseMultiLabeledIdentifierDetailsDTO<T extends BaseMultiLabeledIdentifierModel<T>> extends SKOSReferencesDTO {

    protected BaseMultiLabeledIdentifierDetailsDTO(T model, SharedResourceInstanceDTO sharedResourceInstance) {
        this(model);
        setFromSharedResourceInstance(sharedResourceInstance);
    }

    protected BaseMultiLabeledIdentifierDetailsDTO(T model){
        uri = model.getUri();
        this.multiLabelDTO = new MultiLabelDTO();
        System.out.println(this.multiLabelDTO);
        multiLabelDTO.setPrefLabels(model.getPrefLabels());
        multiLabelDTO.setAltLabels(model.getAltsLabels());
        multiLabelDTO.setDefinitions(model.getDefinitions());
        setLastUpdateTime(model.getLastUpdateTime());

        setSkosReferencesFromModel(model);
    }

    protected BaseMultiLabeledIdentifierDetailsDTO(){

    }

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("MultiLabelDTO")
    protected MultiLabelDTO multiLabelDTO;


    @JsonProperty("from_shared_resource_instance")
    protected SharedResourceInstanceDTO fromSharedResourceInstance;

    @JsonProperty("last_update_date")
    protected OffsetDateTime lastUpdateTime;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public MultiLabelDTO getMultiLabelDTO() {
        return multiLabelDTO;
    }

    public void setMultiLabelDTO(MultiLabelDTO multiLabelDTO) {
        this.multiLabelDTO = multiLabelDTO;
    }

    public SharedResourceInstanceDTO getFromSharedResourceInstance() {
        return fromSharedResourceInstance;
    }

    public void setFromSharedResourceInstance(SharedResourceInstanceDTO fromSharedResourceInstance) {
        this.fromSharedResourceInstance = fromSharedResourceInstance;
    }

    public OffsetDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(OffsetDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    protected void setBasePropertiesToModel(T model) {
        setSkosReferencesToModel(model);
        model.setUri(this.getUri());
        model.setPrefLabels(this.multiLabelDTO.getPrefLabels());
        model.setAltsLabels(this.multiLabelDTO.getAltLabels());
        model.setDefinitions(this.multiLabelDTO.getDefinitions());


    }

    public abstract T toModel();
}
