//******************************************************************************
//                          DataProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModelProperty;
import org.bson.Document;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.server.rest.serialization.uri.UriJsonDeserializer;
import org.opensilex.server.rest.serialization.uri.UriListJsonDeserializer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Provenance model used in DataModel
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "prov_used", "prov_was_associated_with", "settings"})
public class DataProvenanceModel {

    @NotNull
    @ApiModelProperty(value = "provenance uri", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI)
    @JsonDeserialize(using = UriJsonDeserializer.class)
    URI uri;
    
    @ApiModelProperty(value = "experiments uris on which the data has been produced")
    @JsonDeserialize(using = UriListJsonDeserializer.class)
    List<URI> experiments;

    public static final String EXPERIMENT_FIELD = "experiments";

    @Valid
    @JsonProperty("prov_used")
    @ApiModelProperty(value = "list of inputs of the process described in the provenance")
    List<ProvEntityModel> provUsed;

    public static final String PROV_USED_FIELD = "provUsed";

    @Valid
    @JsonProperty("prov_was_associated_with")
    @ApiModelProperty(value = "allow an activity to be linked to an agent")
    List<ProvEntityModel> provWasAssociatedWith;

    public static final String PROV_WAS_ASSOCIATED_WITH_FIELD = "provWasAssociatedWith";

    protected static final String PROVENANCE_URI_FIELD = DataModel.PROVENANCE_FIELD + "." + MongoModel.URI_FIELD;
    protected static final String PROVENANCE_EXPERIMENT_FIELD = DataModel.PROVENANCE_FIELD + "." + EXPERIMENT_FIELD;
    protected static final String PROVENANCE_AGENTS_URI_FIELD = DataModel.PROVENANCE_FIELD + "." + PROV_WAS_ASSOCIATED_WITH_FIELD + "." + ProvEntityModel.URI_FIELD;

    @ApiModelProperty(value = "a key-value system to store specific information")
    Document settings; 

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<URI> experiments) {
        this.experiments = experiments;
    }
        
    public List<ProvEntityModel> getProvUsed() {
        return provUsed;
    }

    public void setProvUsed(List<ProvEntityModel> provUsed) {
        this.provUsed = provUsed;
    }
    
    public List<ProvEntityModel> getProvWasAssociatedWith() {
        return provWasAssociatedWith;
    }

    public void setProvWasAssociatedWith(List<ProvEntityModel> provWasAssociatedWith) {
        this.provWasAssociatedWith = provWasAssociatedWith;
    }

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }


    /*
        Implementation of 'equals' and 'hashCode' methods
        Used for grouping data by provenance
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataProvenanceModel that = (DataProvenanceModel) o;
        return Objects.equals(uri, that.uri) && Objects.equals(experiments, that.experiments) && Objects.equals(provUsed, that.provUsed) && Objects.equals(provWasAssociatedWith, that.provWasAssociatedWith) && Objects.equals(settings, that.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, experiments, provUsed, provWasAssociatedWith, settings);
    }
}
