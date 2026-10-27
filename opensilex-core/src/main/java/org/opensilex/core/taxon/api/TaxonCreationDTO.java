//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.taxon.dal.TaxonModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLLabel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"uri", "rdf_type", "name", "kingdom", "genus", "taxon_ids"})
public class TaxonCreationDTO {

    @JsonProperty("uri")
    @ApiModelProperty(value = "Taxon URI, generated if empty")
    @ValidURI
    protected URI uri;

    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "Rank of the taxon", example = "http://www.opensilex.org/vocabulary/oeso#Species", required = true)
    @NotNull
    @ValidURI
    protected URI type;

    @JsonProperty("name")
    @ApiModelProperty(value = "Scientific name (Latin)", example = "Bacillus velezensis", required = true)
    @NotBlank
    protected String name;

    @JsonProperty("kingdom")
    @ApiModelProperty(value = "Kingdom URI", example = "http://www.opensilex.org/vocabulary/oeso#Bacteria", required = true)
    @NotNull
    @ValidURI
    protected URI kingdom;

    @JsonProperty("genus")
    @ApiModelProperty(value = "Genus URI (species only)")
    @ValidURI
    protected URI genus;

    @JsonProperty("taxon_id")
    @ApiModelProperty(value = "External identifiers, each prefixed by its source", example = "[\"NCBITaxon:492670\", \"TAXREF:123456\"]")
    protected List<String> taxonId = new ArrayList<>();

    public TaxonModel newModel() {
        TaxonModel model = new TaxonModel(uri);
        model.setType(type);
        model.setLabel(new SPARQLLabel(name, ""));
        model.setKingdom(kingdom);
        model.setGenus(genus);
        model.setTaxonId(taxonId == null ? new ArrayList<>() : new ArrayList<>(taxonId));
        return model;
    }

    public URI getUri() { return uri; }
    public void setUri(URI uri) { this.uri = uri; }
    public URI getType() { return type; }
    public void setType(URI type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public URI getKingdom() { return kingdom; }
    public void setKingdom(URI kingdom) { this.kingdom = kingdom; }
    public URI getGenus() { return genus; }
    public void setGenus(URI genus) { this.genus = genus; }
    public List<String> getTaxonId() { return taxonId; }
    public void setTaxonId(List<String> taxonId) { this.taxonId = taxonId; }
}
