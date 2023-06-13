package org.opensilex.sparql.response;

        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;
        import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
        import org.opensilex.sparql.model.SPARQLNamedResourceModel;

        import java.net.URI;
        import java.util.List;


@JsonPropertyOrder({
        "uri", "prefLabels", "altLabels", "definitions"
})
public class ObjectMultiNamedResourceDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("prefLabels")
    protected List<String> prefLabels;

    @JsonProperty("altLabels")
    protected List<String> altLabels;

    @JsonProperty("definitions")
    protected List<String> definitions;

    public ObjectMultiNamedResourceDTO() {
    }

    public ObjectMultiNamedResourceDTO(SPARQLMultiNamedResourceModel<?> model) {
        this.uri = model.getUri();
        this.prefLabels = model.getPrefLabels();
        this.altLabels = model.getAltsLabels();
        this.definitions = model.getDefinitions();
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<String> getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(List<String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    public List<String> getAltLabels() {
        return altLabels;
    }

    public void setAltLabels(List<String> altLabels) {
        this.altLabels = altLabels;
    }

    public List<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<String> definitions) {
        this.definitions = definitions;
    }

    //    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }


}
