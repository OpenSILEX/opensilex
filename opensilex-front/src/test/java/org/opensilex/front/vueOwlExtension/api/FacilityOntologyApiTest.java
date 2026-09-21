package org.opensilex.front.vueOwlExtension.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.XSD;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.OWLClassPropertyRestrictionDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.api.RDFPropertyDTO;
import org.opensilex.core.organisation.api.FacilityApiTest;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.core.organisation.api.facility.FacilityGetDTO;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FacilityOntologyApiTest extends AbstractSecurityIntegrationTest {
    @Test
    public void testRetrieveFacilityRelation() throws Exception {
        var propertyDto = new RDFPropertyDTO();
        propertyDto.setUri(URI.create("http://example.org/test/prop/fac/1"));
        propertyDto.setType(URI.create(OWL2.DatatypeProperty.getURI()));
        propertyDto.setRange(URI.create(XSD.integer.getURI()));
        propertyDto.setDomain(URI.create(Oeso.Facility.getURI()));
        propertyDto.setLabelTranslations(Map.of("en", "Test Fac Prop 1 Label(en)"));
        propertyDto.setCommentTranslations(Map.of("en", "Test Fac Prop 1 Comment(en)"));
        var propertyUri = new UserCallBuilder(OntologyAPITest.CREATE_PROPERTY)
                .setBody(propertyDto)
                .buildAdmin().executeCallAndReturnURI();

        var classDto = new VueRDFTypeDTO();
        classDto.setUri(URI.create("http://example.org/test/type/fac/1"));
        classDto.setParent(URI.create(Oeso.Facility.getURI()));
        classDto.setLabelTranslations(Map.of("en", "Test Fac Type 1 Label(en)"));
        classDto.setCommentTranslations(Map.of("en", "Test Fac Type 1 Comment(en)"));
        var classUri = new UserCallBuilder(OntologyAPITest.CREATE_TYPE)
                .setBody(classDto)
                .buildAdmin().executeCallAndReturnURI();

        var restrictionDto = new OWLClassPropertyRestrictionDTO();
        restrictionDto.setClassURI(classUri);
        restrictionDto.setProperty(propertyUri);
        restrictionDto.setDomain(classUri);
        restrictionDto.setList(false);
        restrictionDto.setRequired(false);
        var restrictionUri = new UserCallBuilder(OntologyAPITest.ADD_RESTRICTION)
                .setBody(restrictionDto)
                .buildAdmin().executeCallAndReturnURI();

        var relationDto = new RDFObjectRelationDTO();
        relationDto.setProperty(propertyUri);
        relationDto.setValue("1");

        var facilityDto = new FacilityCreationDTO();
        facilityDto.setType(classUri);
        facilityDto.setName("Test Fac 1");
        facilityDto.setRelations(List.of(relationDto));
        var facilityUri = new UserCallBuilder(FacilityApiTest.create)
                .setBody(facilityDto)
                .buildAdmin().executeCallAndReturnURI();

        var responseDto = new UserCallBuilder(FacilityApiTest.GET_BY_URI)
                .setUriInPath(facilityUri)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<FacilityGetDTO>>() {})
                .getDeserializedResponse()
                .getResult();

        assertEquals(1, responseDto.getRelations().size());
        var relation = responseDto.getRelations().get(0);
        assertTrue(SPARQLDeserializers.compareURIs(relation.getProperty(), propertyUri));
        assertEquals("1", relation.getValue());
    }
}
