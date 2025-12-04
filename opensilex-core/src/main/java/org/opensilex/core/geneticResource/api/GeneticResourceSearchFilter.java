package org.opensilex.core.geneticResource.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.geneticResourceGroup.api.GeneticResourceGroupApi;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SearchFilter;

import java.net.URI;
import java.util.List;


/**
 * @author rcolin
 * Object which contains all filters for a GeneticResource search
 */
public class GeneticResourceSearchFilter extends SearchFilter {

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

    private URI group;

    private List<URI> parentGeneticResources;

    private List<URI> parentMGeneticResources;

    private List<URI> parentFGeneticResources;

    private Boolean isPublic;
    private AccountModel user;

    private List<URI> groups;

    @ApiModelProperty(value = "Regex pattern for filtering list by uri", example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_URI)
    public String getUri() {
        return uri;
    }

    public GeneticResourceSearchFilter setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @ApiModelProperty(value = "Search by type", example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_TYPE)
    public URI getType() {
        return type;
    }

    public GeneticResourceSearchFilter setType(URI type) {
        this.type = type;
        return this;
    }

    @ApiModelProperty(value = "Regex pattern for filtering list by name and synonyms", example = ".*")
    public String getName() {
        return name;
    }

    public GeneticResourceSearchFilter setName(String name) {
        this.name = name;
        return this;
    }

    @ApiModelProperty(value = "Regex pattern for filtering list by code", example = ".*")
    public String getCode() {
        return code;
    }

    public GeneticResourceSearchFilter setCode(String code) {
        this.code = code;
        return this;
    }

    @ApiModelProperty(value = "Search by production year", example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_PRODUCTION_YEAR)
    public Integer getProductionYear() {
        return productionYear;
    }

    public GeneticResourceSearchFilter setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
        return this;
    }

    @ApiModelProperty(value = "Search by species", example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_SPECIES)
    public URI getSpecies() {
        return species;
    }

    public GeneticResourceSearchFilter setSpecies(URI species) {
        this.species = species;
        return this;
    }

    @ApiModelProperty(value = "Search by variety", example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_VARIETY)
    public URI getVariety() {
        return variety;
    }

    public GeneticResourceSearchFilter setVariety(URI variety) {
        this.variety = variety;
        return this;
    }

    @ApiModelProperty(value = "Search by accession",  example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_ACCESSION)
    public URI getAccession() {
        return accession;
    }

    public GeneticResourceSearchFilter setAccession(URI accession) {
        this.accession = accession;
        return this;
    }

    @ApiModelProperty(value = "Search by institute", example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_INSTITUTE)
    public String getInstitute() {
        return institute;
    }

    public GeneticResourceSearchFilter setInstitute(String institute) {
        this.institute = institute;
        return this;
    }

    @ApiModelProperty(value = "Search by experiment", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI)
    public URI getExperiment() {
        return experiment;
    }

    public GeneticResourceSearchFilter setExperiment(URI experiment) {
        this.experiment = experiment;
        return this;
    }

    @ApiModelProperty(value = "Search by metadata", example = GeneticResourceAPI.GENETIC_RESOURCE_EXAMPLE_METADATA)
    public String getMetadata() {
        return metadata;
    }

    public GeneticResourceSearchFilter setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    @ApiModelProperty(value = "List of geneticResource URI")
    public List<URI> getUris() {
        return uris;
    }

    public GeneticResourceSearchFilter setUris(List<URI> uris) {
        this.uris = uris;
        return this;
    }

    @ApiModelProperty(value = "Search by geneticResource group", example = GeneticResourceGroupApi.GROUP_EXAMPLE_URI)
    public URI getGroup() {
        return group;
    }

    public GeneticResourceSearchFilter setGroup(URI group) {
        this.group = group;
        return this;
    }

    public List<URI> getParentGeneticResources() {
        return parentGeneticResources;
    }

    public GeneticResourceSearchFilter setParentGeneticResources(List<URI> parentGeneticResources) {
        this.parentGeneticResources = parentGeneticResources;
        return this;
    }

    public List<URI> getParentMGeneticResources() {
        return parentMGeneticResources;
    }

    public GeneticResourceSearchFilter setParentMGeneticResources(List<URI> parentMGeneticResources) {
        this.parentMGeneticResources = parentMGeneticResources;
        return this;
    }

    public List<URI> getParentFGeneticResources() {
        return parentFGeneticResources;
    }

    public GeneticResourceSearchFilter setParentFGeneticResources(List<URI> parentFGeneticResources) {
        this.parentFGeneticResources = parentFGeneticResources;
        return this;
    }



    public Boolean isPublic() {
        return isPublic;
    }
    public GeneticResourceSearchFilter setPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }
    public AccountModel getUser() {
        return user;
    }

    public GeneticResourceSearchFilter setUser(AccountModel user) {
        this.user = user;
        return this;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

}
