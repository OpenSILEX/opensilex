package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.sparql.service.SparqlSearchFilter;

import java.net.URI;
import java.util.List;


/**
 * @author rcolin
 * Object which contains all filters for a Germplasm search
 */
public class GermplasmSearchFilter extends SparqlSearchFilter {

    private String uri;

    @JsonProperty("rdf_type")
    private URI type;
    private String name;
    private String code;

    @JsonProperty("production_year")
    private Integer productionYear;

    private URI species;
    private URI variety;
    private URI accession;
    private String institute;
    private URI experiment;

    private String metadata;

    private List<URI> uris;

    @ApiModelProperty(value = "Regex pattern for filtering list by uri", example = GermplasmAPI.GERMPLASM_EXAMPLE_URI)
    public String getUri() {
        return uri;
    }

    public GermplasmSearchFilter setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @ApiModelProperty(value = "Search by type", example = GermplasmAPI.GERMPLASM_EXAMPLE_TYPE)
    public URI getType() {
        return type;
    }

    public GermplasmSearchFilter setType(URI type) {
        this.type = type;
        return this;
    }

    @ApiModelProperty(value = "Regex pattern for filtering list by name and synonyms", example = ".*")
    public String getName() {
        return name;
    }

    public GermplasmSearchFilter setName(String name) {
        this.name = name;
        return this;
    }

    @ApiModelProperty(value = "Regex pattern for filtering list by code", example = ".*")
    public String getCode() {
        return code;
    }

    public GermplasmSearchFilter setCode(String code) {
        this.code = code;
        return this;
    }

    @ApiModelProperty(value = "Search by production year", example = GermplasmAPI.GERMPLASM_EXAMPLE_PRODUCTION_YEAR)
    public Integer getProductionYear() {
        return productionYear;
    }

    public GermplasmSearchFilter setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
        return this;
    }

    @ApiModelProperty(value = "Search by species", example = GermplasmAPI.GERMPLASM_EXAMPLE_SPECIES)
    public URI getSpecies() {
        return species;
    }

    public GermplasmSearchFilter setSpecies(URI species) {
        this.species = species;
        return this;
    }

    @ApiModelProperty(value = "Search by variety", example = GermplasmAPI.GERMPLASM_EXAMPLE_VARIETY)
    public URI getVariety() {
        return variety;
    }

    public GermplasmSearchFilter setVariety(URI variety) {
        this.variety = variety;
        return this;
    }

    @ApiModelProperty(value = "Search by accession",  example = GermplasmAPI.GERMPLASM_EXAMPLE_ACCESSION)
    public URI getAccession() {
        return accession;
    }

    public GermplasmSearchFilter setAccession(URI accession) {
        this.accession = accession;
        return this;
    }

    @ApiModelProperty(value = "Search by institute", example = GermplasmAPI.GERMPLASM_EXAMPLE_INSTITUTE)
    public String getInstitute() {
        return institute;
    }

    public GermplasmSearchFilter setInstitute(String institute) {
        this.institute = institute;
        return this;
    }

    @ApiModelProperty(value = "Search by experiment", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI)
    public URI getExperiment() {
        return experiment;
    }

    public GermplasmSearchFilter setExperiment(URI experiment) {
        this.experiment = experiment;
        return this;
    }

    @ApiModelProperty(value = "Search by metadata", example = GermplasmAPI.GERMPLASM_EXAMPLE_METADATA)
    public String getMetadata() {
        return metadata;
    }

    public GermplasmSearchFilter setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    @ApiModelProperty(value = "List of germplasm URI")
    public List<URI> getUris() {
        return uris;
    }

    public GermplasmSearchFilter setUris(List<URI> uris) {
        this.uris = uris;
        return this;
    }


}
