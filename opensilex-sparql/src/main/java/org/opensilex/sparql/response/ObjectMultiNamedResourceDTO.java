package org.opensilex.sparql.response;

        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.fasterxml.jackson.annotation.JsonPropertyOrder;
        import org.opensilex.sparql.model.SPARQLMultiNamedResourceModel;
        import org.opensilex.sparql.model.SPARQLNamedResourceModel;

        import java.net.URI;
        import java.util.List;
        import java.util.Map;


@JsonPropertyOrder({
        "uri", "prefLabels", "altLabels", "definitions"
})
public class ObjectMultiNamedResourceDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("prefLabels")
    protected Map<String,String> prefLabels;

    @JsonProperty("altLabels")
    protected Map<String,List<String>> altLabels;

    @JsonProperty("definitions")
    protected Map<String,String> definitions;

    public ObjectMultiNamedResourceDTO() {
    }

    public ObjectMultiNamedResourceDTO(SPARQLMultiNamedResourceModel<?> model) {
        this.uri = model.getUri();
        this.prefLabels = model.getPrefLabels().getAllTranslations();
        this.altLabels = model.getAltsLabels().getTranslationsOfAltLabels();
        this.definitions = model.getDefinitions().getAllTranslations();
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Map<String,String> getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(Map<String,String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    public Map<String, List<String>> getAltLabels() {
        return altLabels;
    }

    public void setAltLabels(Map<String, List<String>> altLabels) {
        this.altLabels = altLabels;
    }

    public Map<String,String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String,String> definitions) {
        this.definitions = definitions;
    }



}
