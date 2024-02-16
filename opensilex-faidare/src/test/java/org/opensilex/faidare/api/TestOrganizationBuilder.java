package org.opensilex.faidare.api;

import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.organisation.api.OrganizationCreationDTO;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestOrganizationBuilder extends AbstractSecurityIntegrationTest {

    private static final String stringPrefix = "default organization ";

    private URI uri = new URI("test:default-organization-uri/");

    private URI rdf_type;

    private String rdf_type_name;

    private String name = stringPrefix + "name";

    private List<URI> parents;

    private List<URI> children;

    private List<URI> groups;

    private List<URI> facilities;

    public TestOrganizationBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestOrganizationBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public URI getRdf_type() {
        return rdf_type;
    }

    public TestOrganizationBuilder setRdf_type(URI rdf_type) {
        this.rdf_type = rdf_type;
        return this;
    }

    public String getRdf_type_name() {
        return rdf_type_name;
    }

    public TestOrganizationBuilder setRdf_type_name(String rdf_type_name) {
        this.rdf_type_name = rdf_type_name;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestOrganizationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public List<URI> getParents() {
        return parents;
    }

    public TestOrganizationBuilder setParents(List<URI> parents) {
        this.parents = parents;
        return this;
    }

    public List<URI> getChildren() {
        return children;
    }

    public TestOrganizationBuilder setChildren(List<URI> children) {
        this.children = children;
        return this;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public TestOrganizationBuilder setGroups(List<URI> groups) {
        this.groups = groups;
        return this;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public TestOrganizationBuilder setFacilities(List<URI> facilities) {
        this.facilities = facilities;
        return this;
    }

    private static List<OrganizationCreationDTO> dtoList = new ArrayList<>();

    public static List<OrganizationCreationDTO> getDTOList() {
        return dtoList;
    }

    public URI create() throws Exception {
        OrganizationCreationDTO dto = new OrganizationCreationDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setName(getName() + dtoList.size());
        dto.setType(getRdf_type());
        dto.setTypeLabel(getRdf_type_name());
        dto.setParents(getParents());
        dto.setChildren(getChildren());
        dto.setGroups(getGroups());
        dto.setFacilities(getFacilities());


        URI createdURI = new UserCallBuilder(ExperimentAPITest.create)
                .setBody(dto)
                .buildAdmin()
                .executeCallAndReturnURI();
        dtoList.add(dto);
        return createdURI;
    }
}
