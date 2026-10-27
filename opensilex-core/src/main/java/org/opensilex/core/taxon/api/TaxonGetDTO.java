//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.taxon.dal.TaxonModel;

import java.net.URI;
import java.util.List;

@JsonPropertyOrder({"uri", "rdf_type", "name", "kingdom", "genus", "taxon_ids"})
public class TaxonGetDTO {

    @JsonProperty("uri")
    @ApiModelProperty(value = "Taxon URI")
    protected URI uri;

    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "Rank of the taxon (Species or Genus)", example = "http://www.opensilex.org/vocabulary/oeso#Species")
    protected URI type;

    @JsonProperty("name")
    @ApiModelProperty(value = "Scientific name", example = "Escherichia coli")
    protected String name;

    @JsonProperty("kingdom")
    @ApiModelProperty(value = "Kingdom URI", example = "http://www.opensilex.org/vocabulary/oeso#Bacteria")
    protected URI kingdom;

    @JsonProperty("genus")
    @ApiModelProperty(value = "Genus URI (species only)")
    protected URI genus;

    @JsonProperty("taxon_id")
    @ApiModelProperty(value = "External identifiers, prefixed by their source", example = "[\"NCBITaxon:562\"]")
    protected List<String> taxonId;

    public static TaxonGetDTO fromModel(TaxonModel model) {
        TaxonGetDTO dto = new TaxonGetDTO();
        dto.uri = model.getUri();
        dto.type = model.getType();
        dto.name = model.getName();
        dto.kingdom = model.getKingdom();
        dto.genus = model.getGenus();
        dto.taxonId = model.getTaxonId();
        return dto;
    }

    public URI getUri() { return uri; }
    public URI getType() { return type; }
    public String getName() { return name; }
    public URI getKingdom() { return kingdom; }
    public URI getGenus() { return genus; }
    public List<String> getTaxonId() { return taxonId; }
}
