//******************************************************************************
//                          OntologyAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.front.vueOwlExtension.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.XSD;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.OntologyAPI;
import org.opensilex.core.ontology.api.RDFPropertyDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.ResourceTreeDTO;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;

/**
 * @author rcolin
 */
public class OntologyAPITest extends AbstractSecurityIntegrationTest {

    String rdfTypePath = VueOwlExtensionAPI.PATH + "/" + VueOwlExtensionAPI.RDF_TYPE_PATH;
    String createPropertyPath = OntologyAPI.PATH + "/" + OntologyAPI.PROPERTY_PATH;
    String deleteRdfTypePath = rdfTypePath + "/{uri}";
    String searchSubClassOfPath = OntologyAPI.PATH + "/" + OntologyAPI.SEARCH_SUB_CLASS_OF_PATH;

    String propertyPath = OntologyAPI.PATH + "/" + OntologyAPI.PROPERTY_PATH;

    @BeforeClass
    public static void setup() throws Exception {

    }

    VueRDFTypeDTO getTypeDto(String name, URI parent) {

        VueRDFTypeDTO dto = new VueRDFTypeDTO();
        dto.setUri(URI.create("test:class_" + name));

        Map<String, String> labels = new HashMap<>();
        labels.put("en", "class_label_en" + name);
        labels.put("fr", "class_label_fr" + name);
        labels.put("", "class_label_no_lang" + name);
        dto.setLabelTranslations(labels);

        Map<String, String> comments = new HashMap<>();
        comments.put("en", "class_comment_en" + name);
        comments.put("fr", "class_comment_fr" + name);
        comments.put("", "class_comment_no_lang" + name);
        dto.setCommentTranslations(comments);

        dto.setParent(parent);
        return dto;
    }

    @Test
    public void testCreateClass() throws Exception {
        Response postResult = getJsonPostResponseAsAdmin(target(rdfTypePath), getTypeDto("0", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        postResult = getJsonPostResponseAsAdmin(target(rdfTypePath), getTypeDto("1", null));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetClass() throws Exception {

        // create types
        getJsonPostResponseAsAdmin(target(rdfTypePath), getTypeDto("0", null));
        getJsonPostResponseAsAdmin(target(rdfTypePath), getTypeDto("1", null));

        String getTypePath = OntologyAPI.PATH + "/" + OntologyAPI.RDF_TYPE;

        // get types
        URI classURI = URI.create("test:class_0");
        Response getResult = getJsonGetResponseAsAdmin(target(getTypePath).queryParam("rdf_type", classURI.toString()));
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        classURI = URI.create("test:class_1");
        getResult = getJsonGetResponseAsAdmin(target(getTypePath).queryParam("rdf_type", classURI.toString()));
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

    }

    @Test
    public void testSearchSubClasses() throws Exception {

        VueRDFTypeDTO parent1 = getTypeDto("parent1", URI.create(Oeso.ScientificObject.toString()));
        getJsonPostResponseAsAdmin(target(rdfTypePath), parent1);

        VueRDFTypeDTO child11 = getTypeDto("child11", parent1.getUri());
        VueRDFTypeDTO child12 = getTypeDto("child12", parent1.getUri());
        getJsonPostResponseAsAdmin(target(rdfTypePath), child11);
        getJsonPostResponseAsAdmin(target(rdfTypePath), child12);

        VueRDFTypeDTO parent2 = getTypeDto("parent2", URI.create(Oeso.ScientificObject.toString()));
        getJsonPostResponseAsAdmin(target(rdfTypePath), parent2);

        VueRDFTypeDTO child21 = getTypeDto("child21", parent2.getUri());
        VueRDFTypeDTO child211 = getTypeDto("child211", child21.getUri());
        getJsonPostResponseAsAdmin(target(rdfTypePath), child21);
        getJsonPostResponseAsAdmin(target(rdfTypePath), child211);

        Map<String, Object> params = new HashMap<>();
        params.put("parent_type", Oeso.ScientificObject.toString());
        List<ResourceTreeDTO> tree = getTreeResults(searchSubClassOfPath, params);
        assertFalse(tree.isEmpty());

        ResourceTreeDTO root = tree.get(0);

        // try to find all parent and children from tree
        ResourceTreeDTO parent1FromDb = root.getChildren().stream().filter(resourceTreeDTO -> SPARQLDeserializers.compareURIs(resourceTreeDTO.getUri(),parent1.getUri()))
                .findFirst().get();

        ResourceTreeDTO child11FromDb =  parent1FromDb.getChildren().stream()
                .filter(resourceTreeDTO -> SPARQLDeserializers.compareURIs(resourceTreeDTO.getUri(),child11.getUri()))
                .findFirst().get();

        ResourceTreeDTO child12FromDb =  parent1FromDb.getChildren().stream()
                .filter(resourceTreeDTO -> SPARQLDeserializers.compareURIs(resourceTreeDTO.getUri(),child12.getUri()))
                .findFirst().get();


        ResourceTreeDTO parent2FromDb = root.getChildren().stream().filter(resourceTreeDTO -> SPARQLDeserializers.compareURIs(resourceTreeDTO.getUri(),parent2.getUri()))
                .findFirst().get();

        ResourceTreeDTO child21FromDb =  parent2FromDb.getChildren().stream()
                .filter(resourceTreeDTO -> SPARQLDeserializers.compareURIs(resourceTreeDTO.getUri(),child21.getUri()))
                .findFirst().get();

        ResourceTreeDTO child211FromDb =  child21FromDb.getChildren().stream()
                .filter(resourceTreeDTO -> SPARQLDeserializers.compareURIs(resourceTreeDTO.getUri(),child211.getUri()))
                .findFirst().get();
    }

    @Test
    public void testDeleteClass() throws Exception {

        // create type
        VueRDFTypeDTO dto = getTypeDto("testDeleteClass", null);
        getJsonPostResponseAsAdmin(target(rdfTypePath), dto);
        Response deleteResponse = getDeleteByUriResponse(target(deleteRdfTypePath), dto.getUri().toString());
        assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());

        Response getResult = getJsonGetResponseAsAdmin(target(rdfTypePath).queryParam("rdf_type", dto.getUri().toString()));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());

        // test to recreate it, should be OK
        Response recreateResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), recreateResponse.getStatus());
    }

    @Test
    public void testDeleteClassWithAssociatedInstanceFail() throws Exception {

        // create type
        VueRDFTypeDTO dto = getTypeDto("testDeleteClassWithAssociatedInstanceFail", URI.create(Oeso.ScientificObject.toString()));
        Response createResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());

        // create object (here ScientificObject) with the created type
        ScientificObjectModel model = new ScientificObjectModel();
        model.setType(dto.getUri());
        model.setName("test:os-testDeleteClassWithAssociatedInstanceFail");
        getSparqlService().create(model);

        Response deleteResponse = getDeleteByUriResponse(target(deleteRdfTypePath), dto.getUri().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());
    }


    @Test
    public void testDeleteClassWithChildrenFail() throws Exception {
        VueRDFTypeDTO dto = getTypeDto("testDeleteClassWithChildrenFail", URI.create(Oeso.ScientificObject.toString()));
        Response createResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());

        VueRDFTypeDTO child1 = getTypeDto("testDeleteClassWithChildrenFail_child1", dto.getUri());
        createResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), child1);
        assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());

        VueRDFTypeDTO child2 = getTypeDto("testDeleteClassWithChildrenFail_child2", dto.getUri());
        createResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), child2);
        assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());

        Response deleteResponse = getDeleteByUriResponse(target(deleteRdfTypePath), dto.getUri().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testDeleteClassWithDataPropertiesFail() throws Exception {

        // create class
        VueRDFTypeDTO dto = getTypeDto("testDeleteClassWithDataPropertiesFail", URI.create(Oeso.ScientificObject.toString()));
        Response createResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());

        // create data property
        RDFPropertyDTO dataProperty = getPropertyDto("data_prop_to_delete", null, true, dto.getUri(), URI.create(XSD.integer.getURI()));
        Response createDataPropertyResponse = getJsonPostResponseAsAdmin(target(createPropertyPath), dataProperty);
        assertEquals(Response.Status.CREATED.getStatusCode(), createDataPropertyResponse.getStatus());

        // delete class -> bad request
        Response deleteResponse = getDeleteByUriResponse(target(deleteRdfTypePath), dto.getUri().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());

        // ensure message is OK
        JsonNode node = deleteResponse.readEntity(JsonNode.class);
        ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
        assertEquals(errorResponse.getResult().translationKey,"component.ontology.class.exception.delete.has-data-properties");
    }

    @Test
    public void testDeleteClassWithObjectPropertiesFail() throws Exception {

        // create class
        VueRDFTypeDTO dto = getTypeDto("testDeleteClassWithObjectPropertiesFail", URI.create(Oeso.ScientificObject.toString()));
        Response createResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), dto);
        assertEquals(Response.Status.CREATED.getStatusCode(), createResponse.getStatus());

        // create range class
        VueRDFTypeDTO range = getTypeDto("testDeleteClassWithObjectPropertiesFail-range", null);
        Response createRangeResponse = getJsonPostResponseAsAdmin(target(rdfTypePath), range);
        assertEquals(Response.Status.CREATED.getStatusCode(), createRangeResponse.getStatus());

        // create object property
        RDFPropertyDTO objectProperty = getPropertyDto("object_prop_to_delete", null, false, dto.getUri(), range.getUri());
        Response createObjectPropertyResponse = getJsonPostResponseAsAdmin(target(createPropertyPath), objectProperty);
        assertEquals(Response.Status.CREATED.getStatusCode(), createObjectPropertyResponse.getStatus());

        // delete class -> bad request
        Response deleteResponse = getDeleteByUriResponse(target(deleteRdfTypePath), dto.getUri().toString());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());

        // ensure message is OK
        JsonNode node = deleteResponse.readEntity(JsonNode.class);
        ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
        assertEquals(errorResponse.getResult().translationKey,"component.ontology.class.exception.delete.has-object-properties");
    }

    private RDFPropertyDTO getPropertyDto(String suffix, URI parent, boolean dataProperty, URI domain, URI range) {

        RDFPropertyDTO dto = new RDFPropertyDTO();
        dto.setUri(URI.create("test:property_" + suffix));
        dto.setDomain(domain);

        if (dataProperty) {
            dto.setType(URI.create(OWL2.DatatypeProperty.getURI()));
            dto.setRange(range);
        } else {
            dto.setType(URI.create(OWL2.ObjectProperty.getURI()));
//            URI classURI = URI.create("test:class_1");
            dto.setRange(range);
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("en", "property_label_en" + suffix);
        labels.put("fr", "property_label_fr" + suffix);
        labels.put("", "property_label_no_lang" + suffix);
        dto.setLabelTranslations(labels);

        Map<String, String> comments = new HashMap<>();
        comments.put("en", "property_comment_en" + suffix);
        comments.put("fr", "property_comment_fr" + suffix);
        comments.put("", "property_comment_no_lang" + suffix);
        dto.setCommentTranslations(comments);

        dto.setParent(parent);
        return dto;
    }

    @Test
    public void testCreateAndGetProperty() throws Exception {

        // create types
        VueRDFTypeDTO domain = getTypeDto("domain", null);
        VueRDFTypeDTO range = getTypeDto("range", null);
        getJsonPostResponseAsAdmin(target(rdfTypePath), domain);
        getJsonPostResponseAsAdmin(target(rdfTypePath), range);

        // create data and object property
        RDFPropertyDTO dataProperty = getPropertyDto("data_0", null, true, domain.getUri(), URI.create(XSD.integer.getURI()));
        RDFPropertyDTO objectProperty = getPropertyDto("object_1", null, false, domain.getUri(), range.getUri());

        // check that property are created
        Response createDataPropertyResponse = getJsonPostResponseAsAdmin(target(createPropertyPath), dataProperty);
        assertEquals(Response.Status.CREATED.getStatusCode(), createDataPropertyResponse.getStatus());

        Response createObjectPropertyResponse = getJsonPostResponseAsAdmin(target(createPropertyPath), objectProperty);
        assertEquals(Response.Status.CREATED.getStatusCode(), createObjectPropertyResponse.getStatus());


        String getPropertyPath = OntologyAPI.PATH + "/" + OntologyAPI.PROPERTY_PATH;

        // get data property
        Response getResult = getJsonGetResponseAsAdmin(target(getPropertyPath)
                .queryParam("uri", dataProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.DatatypeProperty.getURI())
        );
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // get object property
        getResult = getJsonGetResponseAsAdmin(target(getPropertyPath)
                .queryParam("uri", objectProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.ObjectProperty.getURI())
        );
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testDeleteProperty() throws Exception {

        // create types
        VueRDFTypeDTO domain = getTypeDto("domain", null);
        VueRDFTypeDTO range = getTypeDto("range", null);
        getJsonPostResponseAsAdmin(target(rdfTypePath), domain);
        getJsonPostResponseAsAdmin(target(rdfTypePath), range);

        // create data and object property
        RDFPropertyDTO dataProperty = getPropertyDto("data_0", null, true, domain.getUri(), URI.create(XSD.integer.getURI()));
        RDFPropertyDTO objectProperty = getPropertyDto("object_1", null, false, domain.getUri(), range.getUri());
        getJsonPostResponseAsAdmin(target(createPropertyPath), dataProperty);
        getJsonPostResponseAsAdmin(target(createPropertyPath), objectProperty);

        Response deleteDataPropertyResponse = appendAdminToken(target(propertyPath)
                .queryParam("uri", dataProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.DatatypeProperty.getURI())).delete();

        assertEquals(Response.Status.OK.getStatusCode(), deleteDataPropertyResponse.getStatus());

        Response deleteObjectPropertyResponse = appendAdminToken(target(propertyPath)
                .queryParam("uri", objectProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.ObjectProperty.getURI())).delete();

        assertEquals(Response.Status.OK.getStatusCode(), deleteObjectPropertyResponse.getStatus());
    }

    @Test
    public void testDeletePropertyWithAssociatedRelationFail() throws Exception {

        // create OS type
        VueRDFTypeDTO domain = getTypeDto("domain", URI.create(Oeso.ScientificObject.toString()));
        VueRDFTypeDTO range = getTypeDto("range", URI.create(Oeso.ScientificObject.toString()));
        getJsonPostResponseAsAdmin(target(rdfTypePath), domain);
        getJsonPostResponseAsAdmin(target(rdfTypePath), range);

        // create data/object property on custom OS type
        RDFPropertyDTO dataProperty = getPropertyDto("data_0", null, true, domain.getUri(), URI.create(XSD.integer.getURI()));
        RDFPropertyDTO objectProperty = getPropertyDto("object_1", null, false, domain.getUri(), range.getUri());
        getJsonPostResponseAsAdmin(target(createPropertyPath), dataProperty);
        getJsonPostResponseAsAdmin(target(createPropertyPath), objectProperty);

        // create objects (here ScientificObject) with the created type
        ScientificObjectModel model = new ScientificObjectModel();
        model.setType(domain.getUri());
        model.setName("test:os-testDeletePropertyWithAssociatedRelationFail");
        getSparqlService().create(model);

        // create OS with data and object property
        ScientificObjectModel model2 = new ScientificObjectModel();
        model2.setType(domain.getUri());
        model2.setName("test:os-testDeletePropertyWithAssociatedRelationFail2");
        model2.addRelation(null, dataProperty.getUri(), Integer.class, "5");
        model2.addRelation(null, objectProperty.getUri(), URI.class, model.getUri().toString());
        getSparqlService().create(model2);

        // check that data property deletion FAIL
        Response deleteDataPropertyResponse = appendAdminToken(target(propertyPath)
                .queryParam("uri", dataProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.DatatypeProperty.getURI())).delete();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteDataPropertyResponse.getStatus());
        JsonNode node = deleteDataPropertyResponse.readEntity(JsonNode.class);
        ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {
        });
        assertEquals("Some objects use the data-property test:property_data_0. You must delete these relations first", errorResponse.getResult().message);

        // check that object property deletion FAIL
        Response deleteObjectPropertyResponse = appendAdminToken(target(propertyPath)
                .queryParam("uri", objectProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.ObjectProperty.getURI())).delete();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteObjectPropertyResponse.getStatus());
        node = deleteObjectPropertyResponse.readEntity(JsonNode.class);
        errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {
        });
        assertEquals("Some objects use the object-property test:property_object_1. You must delete these relations first", errorResponse.getResult().message);
    }

    @Test
    public void testDeletePropertyWithChildrenFail() throws Exception {

        // create types
        VueRDFTypeDTO domain = getTypeDto("domain", null);
        VueRDFTypeDTO range = getTypeDto("range", null);
        getJsonPostResponseAsAdmin(target(rdfTypePath), domain);
        getJsonPostResponseAsAdmin(target(rdfTypePath), range);

        // create data and object property
        RDFPropertyDTO dataProperty = getPropertyDto("data_0", null, true, domain.getUri(), URI.create(XSD.integer.getURI()));
        RDFPropertyDTO objectProperty = getPropertyDto("object_1", null, false, domain.getUri(), range.getUri());
        getJsonPostResponseAsAdmin(target(createPropertyPath), dataProperty);
        getJsonPostResponseAsAdmin(target(createPropertyPath), objectProperty);

        RDFPropertyDTO dataPropertyChild = getPropertyDto("data_child_0", dataProperty.getUri(), true, domain.getUri(), URI.create(XSD.integer.getURI()));
        RDFPropertyDTO objectPropertyChild = getPropertyDto("object_child_0", objectProperty.getUri(), false, domain.getUri(), range.getUri());

        // check that children property are created
        Response createDataPropertyResponse = getJsonPostResponseAsAdmin(target(createPropertyPath), dataPropertyChild);
        assertEquals(Response.Status.CREATED.getStatusCode(), createDataPropertyResponse.getStatus());

        Response createObjectPropertyResponse = getJsonPostResponseAsAdmin(target(createPropertyPath), objectPropertyChild);
        assertEquals(Response.Status.CREATED.getStatusCode(), createObjectPropertyResponse.getStatus());

        // check that data property deletion FAIL
        Response deleteDataPropertyResponse = appendAdminToken(target(propertyPath)
                .queryParam("uri", dataProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.DatatypeProperty.getURI())).delete();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteDataPropertyResponse.getStatus());
        JsonNode node = deleteDataPropertyResponse.readEntity(JsonNode.class);
        ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {
        });
        assertEquals("The property test:property_data_0 has child properties. You must delete them thirst", errorResponse.getResult().message);

        // check that object property deletion FAIL
        Response deleteObjectPropertyResponse = appendAdminToken(target(propertyPath)
                .queryParam("uri", objectProperty.getUri().toString())
                .queryParam("rdf_type", OWL2.ObjectProperty.getURI())).delete();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteObjectPropertyResponse.getStatus());
        node = deleteObjectPropertyResponse.readEntity(JsonNode.class);
        errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {
        });
        assertEquals("The property test:property_object_1 has child properties. You must delete them thirst", errorResponse.getResult().message);
    }

    @Override
    public void afterEach() throws Exception {
        OntologyDAO dao = new OntologyDAO(getSparqlService());
        getSparqlService().clearGraph(dao.getCustomGraph().getURI());
        getSparqlService().clearGraph(ScientificObjectModel.class);
    }


}