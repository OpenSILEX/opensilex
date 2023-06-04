package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.ResourceDTO;

import java.net.URI;
import java.util.ArrayList;

@JsonPropertyOrder({"prefLabel", "altLabels", "definition"})
public class LabelDTO{

    @JsonProperty("prefLabel")
    protected String prefLabel;

    @JsonProperty("altLabels")
    protected ArrayList<String> altLabels;

    @JsonProperty("definition")
    protected String definition;

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    public ArrayList<String> getAltLabels() {
        return altLabels;
    }

    public void setAltLabels(ArrayList<String> altLabels) {
        this.altLabels = altLabels;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }



}
