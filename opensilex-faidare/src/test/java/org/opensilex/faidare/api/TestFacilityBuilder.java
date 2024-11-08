package org.opensilex.faidare.api;

import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.api.facility.FacilityAddressDTO;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestFacilityBuilder  {

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    AccountModel currentUser;

    private static final String stringPrefix = "default facility ";

    private URI uri = new URI("test:default-facility-uri/");

    private String name = stringPrefix + "name";

    private URI type = URI.create(Oeso.Facility.getURI());

    private List<RDFObjectRelationDTO> relations;

    private String typeLabel;

    private FacilityAddressDTO address = new FacilityAddressDTO();

    private List<LocationObservationDTO> locations;

    private List<URI> organizations;

    private List<URI> sites;

    private List<URI> variableGroups;

    public TestFacilityBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestFacilityBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestFacilityBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public URI getType() {
        return type;
    }

    public TestFacilityBuilder setType(URI type) {
        this.type = type;
        return this;
    }

    public List<RDFObjectRelationDTO> getRelations() {
        return relations;
    }

    public TestFacilityBuilder setRelations(List<RDFObjectRelationDTO> relations) {
        this.relations = relations;
        return this;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public TestFacilityBuilder setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
        return this;
    }

    public FacilityAddressDTO getAddress() {
        return address;
    }

    public TestFacilityBuilder setAddress(FacilityAddressDTO address) {
        this.address = address;
        return this;
    }

    public List<LocationObservationDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationObservationDTO> locations) {
        this.locations = locations;
    }

    public List<URI> getOrganizations() {
        return organizations;
    }

    public TestFacilityBuilder setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
        return this;
    }

    public List<URI> getSites() {
        return sites;
    }

    public TestFacilityBuilder setSites(List<URI> sites) {
        this.sites = sites;
        return this;
    }

    public List<URI> getVariableGroups() {
        return variableGroups;
    }

    public TestFacilityBuilder setVariableGroups(List<URI> variableGroups) {
        this.variableGroups = variableGroups;
        return this;
    }

    private final List<FacilityCreationDTO> dtoList = new ArrayList<>();

    public List<FacilityCreationDTO> getDTOList() {
        return dtoList;
    }

    public FacilityCreationDTO createDTO() throws Exception {
        FacilityCreationDTO dto = new FacilityCreationDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setName(getName() + dtoList.size());
        dto.setType(getType());
        dto.setRelations(getRelations());
        dto.setTypeLabel(getTypeLabel());
        dto.setAddress(getAddress());
        dto.setLocations(getLocations());
        dto.setOrganizations(getOrganizations());
        dto.setSites(getSites());
        dto.setVariableGroups(getVariableGroups());

        dtoList.add(dto);
        return dto;
    }
}
