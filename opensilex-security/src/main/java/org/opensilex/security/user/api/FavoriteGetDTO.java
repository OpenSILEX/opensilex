package org.opensilex.security.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.opensilex.sparql.exceptions.SPARQLIllegalStateException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiModel
public class FavoriteGetDTO {

    @JsonProperty("uri")
    private URI uri;

    @JsonProperty("type")
    private URI type;

    @JsonProperty("defaultName")
    private String defaultName;

    @JsonProperty("graphNames")
    private List<FavoriteGetGraphNameDTO> graphNameDtoList;

    public static FavoriteGetDTO fromNamedResourceModelContextMap(URI uri, Map<URI, SPARQLNamedResourceModel<?>> map) throws SPARQLIllegalStateException {
        FavoriteGetDTO dto = new FavoriteGetDTO();
        dto.setUri(uri);
        dto.setGraphNameDtoList(new ArrayList<>());
        for (Map.Entry<URI, SPARQLNamedResourceModel<?>> graphModel : map.entrySet()) {
            dto.getGraphNameDtoList().add(new FavoriteGetGraphNameDTO(graphModel.getKey(), graphModel.getValue().getName()));
            if (dto.getType() == null) {
                dto.setType(graphModel.getValue().getType());
            } else if (!Objects.equals(dto.getType(), graphModel.getValue().getType())) {
                throw new SPARQLIllegalStateException("URI has multiple incompatible types : <" + uri + "> (types are <" + dto.getType() + "> and <" + graphModel.getValue().getType() + ">");
            }
        }
        return dto;
    }

    public void setDefaultLabelFromGraph(URI defaultGraph) {
        setDefaultName(getGraphNameDtoList().stream()
                .filter(graphName -> Objects.equals(graphName.getGraph(), defaultGraph))
                .map(FavoriteGetGraphNameDTO::getName)
                .findFirst()
                .orElse(null));
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public List<FavoriteGetGraphNameDTO> getGraphNameDtoList() {
        return graphNameDtoList;
    }

    public void setGraphNameDtoList(List<FavoriteGetGraphNameDTO> graphNameDtoList) {
        this.graphNameDtoList = graphNameDtoList;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }
}
