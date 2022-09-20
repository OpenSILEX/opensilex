package org.opensilex.core.variable.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.sparql.response.ObjectNamedResourceDTO;

import java.net.URI;

public class EntityDuplicatesDTO extends ObjectNamedResourceDTO {

    @JsonProperty("levenshtein")
    private int levenshtein;

    @JsonProperty("containedIn")
    private boolean containedIn;

    public EntityDuplicatesDTO(EntityModel model) {
        super(model);
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Plant")
    public String getName() {
        return name;
    }

    public int getLevenshtein() {
        return levenshtein;
    }

    public void setLevenshtein(int levenshtein) {
        this.levenshtein = levenshtein;
    }

    public boolean getContainedIn() {
        return containedIn;
    }

    public void setContainedIn(boolean containedIn) {
        this.containedIn = containedIn;
    }
}
