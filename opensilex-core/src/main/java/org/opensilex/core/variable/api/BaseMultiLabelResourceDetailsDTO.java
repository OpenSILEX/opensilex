package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.variable.dal.BaseMultiLabelsResourceModel;

import java.net.URI;
import java.time.OffsetDateTime;

@JsonPropertyOrder({
        "uri", "multiLabelsDTO",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public abstract class BaseMultiLabelResourceDetailsDTO<T extends BaseMultiLabelsResourceModel<T>> extends SKOSReferencesDTO {

    protected BaseMultiLabelResourceDetailsDTO(T model, SharedResourceInstanceDTO sharedResourceInstance) {
        this(model);
        setFromSharedResourceInstance(sharedResourceInstance);
    }

    protected BaseMultiLabelResourceDetailsDTO(T model){

        uri = model.getUri();
        this.multiLabelsDTO = new MultiLabelsDTO();

        multiLabelsDTO.setPrefLabels(model.getPrefLabels().getAllTranslations());
        multiLabelsDTO.setShortLabels(model.getShortLabels().getAllTranslations());
        multiLabelsDTO.setAltLabels(model.getAltsLabels().getTranslations());
        multiLabelsDTO.setDefinitions(model.getDefinitions().getAllTranslations());
        setLastUpdateTime(model.getLastUpdateTime());

        setSkosReferencesFromModel(model);
    }

    protected BaseMultiLabelResourceDetailsDTO(){

    }

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("multiLabelsDTO")
    protected MultiLabelsDTO multiLabelsDTO;

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

    public MultiLabelsDTO getMultiLabelsDTO() {
        return multiLabelsDTO;
    }

    public void setMultiLabelsDTO(MultiLabelsDTO multiLabelsDTO) {
        this.multiLabelsDTO = multiLabelsDTO;
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
        model.getPrefLabels().addAllTranslations(this.multiLabelsDTO.getPrefLabels());
        model.getAltsLabels().addAllTranslations(this.multiLabelsDTO.getAltLabels());
        model.getDefinitions().addAllTranslations(this.multiLabelsDTO.getDefinitions());

    }

    public abstract T toModel();
}
