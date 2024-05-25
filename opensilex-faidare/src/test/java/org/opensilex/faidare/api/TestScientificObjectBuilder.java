package org.opensilex.faidare.api;

import org.geojson.GeoJsonObject;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectAPITest;
import org.opensilex.core.scientificObject.api.ScientificObjectCreationDTO;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestScientificObjectBuilder extends AbstractSecurityIntegrationTest {

    private static final String stringPrefix = "default scientific object ";

    private URI uri = new URI("test:default-scientific-object-uri/");

    private URI rdf_type;

    private String name = stringPrefix + "name";

    private URI experiment;

    private GeoJsonObject geometry;

    private List<RDFObjectRelationDTO> relations;

    public TestScientificObjectBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestScientificObjectBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public URI getRdf_type() {
        return rdf_type;
    }

    public TestScientificObjectBuilder setRdf_type(URI rdf_type) {
        this.rdf_type = rdf_type;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestScientificObjectBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public URI getExperiment() {
        return experiment;
    }

    public TestScientificObjectBuilder setExperiment(URI experiment) {
        this.experiment = experiment;
        return this;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public TestScientificObjectBuilder setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
        return this;
    }

    public List<RDFObjectRelationDTO> getRelations() {
        return relations;
    }

    public TestScientificObjectBuilder setRelations(List<RDFObjectRelationDTO> relations) {
        this.relations = relations;
        return this;
    }

    private final List<ScientificObjectCreationDTO> dtoList = new ArrayList<>();

    public List<ScientificObjectCreationDTO> getDTOList() {
        return dtoList;
    }

    public URI create() throws Exception {
        ScientificObjectCreationDTO dto = new ScientificObjectCreationDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setName(getName() + dtoList.size());
        dto.setType(getRdf_type());
        dto.setExperiment(getExperiment());
        dto.setGeometry(getGeometry());
        dto.setRelations(getRelations());

        URI createdURI = new UserCallBuilder(ScientificObjectAPITest.create)
                .setBody(dto)
                .buildAdmin()
                .executeCallAndReturnURI();
        dtoList.add(dto);
        return createdURI;
    }
}
