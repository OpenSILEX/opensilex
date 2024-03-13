//******************************************************************************
//                          GermplasmAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.utils.ListWithPagination;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author Alice BOIZET
 */
public class GermplasmAPITest extends BaseGermplasmAPITest {
    @Test
    public void testCreate() throws Exception {

        // create species
        final Response postResultSpecies = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultSpecies.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        URI createdSpeciesUri = extractUriFromResponse(postResultSpecies);
        final Response getResultSpecies = getJsonGetByUriResponseAsAdmin(target(uriPath), createdSpeciesUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultSpecies.getStatus());
        
    }

    //CRUD tests for germs with parents

    @Test
    public void testCreateWithParents() throws Exception {
        //Create some parents
        final Response postResultSpecies = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        URI someParentMURI = extractUriFromResponse(postResultSpecies);

        final Response postResultSpeciesF = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        URI someParentFURI = extractUriFromResponse(postResultSpeciesF);

        //Test create
        GermplasmCreationDTO someChildDTO = new GermplasmCreationDTO();
        someChildDTO.setName("john junior");
        someChildDTO.setRdfType(new URI(Oeso.Species.toString()));
        List<RDFObjectRelationDTO> someAccessionChildDTORelations = new ArrayList<>();
        RDFObjectRelationDTO someAccessionChildDTORelation1 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmM.getURI()), someParentMURI.toString(), false);
        someAccessionChildDTORelations.add(someAccessionChildDTORelation1);
        RDFObjectRelationDTO someAccessionChildDTORelation3 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmF.getURI()), someParentFURI.toString(), false);
        someAccessionChildDTORelations.add(someAccessionChildDTORelation3);
        someChildDTO.setRelations(someAccessionChildDTORelations);
        final Response postResultChild1 = getJsonPostResponseAsAdmin(target(createPath), someChildDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultChild1.getStatus());

        GermplasmCreationDTO someChildDTO2 = new GermplasmCreationDTO();
        someChildDTO2.setName("bla junior");
        someChildDTO2.setRdfType(new URI(Oeso.Species.toString()));
        List<RDFObjectRelationDTO> childDTORelations2 = new ArrayList<>();
        RDFObjectRelationDTO someVarietyChildDTORelation1 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmM.getURI()), someParentMURI.toString(), false);
        someAccessionChildDTORelations.add(someVarietyChildDTORelation1);
        RDFObjectRelationDTO someVarietyChildDTORelation2 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmF.getURI()), someParentFURI.toString(), false);
        childDTORelations2.add(someVarietyChildDTORelation2);
        someChildDTO2.setRelations(childDTORelations2);
        final Response postResultVarietyChild = getJsonPostResponseAsAdmin(target(createPath), someChildDTO2);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultVarietyChild.getStatus());

    }

    @Test
    public void testUpdateWithParents() throws Exception {
        //Create some parents
        final Response postResultSpecies = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        URI someParentFURI = extractUriFromResponse(postResultSpecies);

        //create a germplasm to update
        GermplasmCreationDTO germplasmToUpdateDTO = getCreationSpeciesDTO();
        final Response germplasmToUpdatePostResponse = getJsonPostResponseAsAdmin(target(createPath), germplasmToUpdateDTO);
        URI germplasmToUpdateURI = extractUriFromResponse(germplasmToUpdatePostResponse);

        //Test update
        List<RDFObjectRelationDTO> newRelations = new ArrayList<>();
        RDFObjectRelationDTO parentFRelation = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmF.getURI()), someParentFURI.toString(), false);
        newRelations.add(parentFRelation);

        germplasmToUpdateDTO.setUri(germplasmToUpdateURI);
        germplasmToUpdateDTO.setRelations(newRelations);
        final Response updateResponse = getJsonPutResponse(target(updatePath), germplasmToUpdateDTO);
        assertEquals(Status.OK.getStatusCode(), updateResponse.getStatus());

        //Test a search on updated germplasm to make sure the update worked properly
        HashMap<String, Object> params = new HashMap<String, Object>() {
            {
                put("parent_germplasms_f", Collections.singletonList(someParentFURI));
            }
        };
        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();
        assertEquals(germplasmList.size(), 1);
        assertEquals(germplasmList.get(0).uri, germplasmToUpdateURI);
    }

    @Test
    public void testSearchWithParents() throws Exception {
        //Create some parents
        final Response postResultSpecies = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        URI someParentMURI = extractUriFromResponse(postResultSpecies);

        final Response postResultSpecies2 = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        URI someParentM2URI = extractUriFromResponse(postResultSpecies2);

        final Response postResultSpeciesF = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        URI someParentFURI = extractUriFromResponse(postResultSpeciesF);

        //create children
        GermplasmCreationDTO someChildDTO = new GermplasmCreationDTO();
        someChildDTO.setName("john junior");
        someChildDTO.setRdfType(new URI(Oeso.Species.toString()));
        List<RDFObjectRelationDTO> someAccessionChildDTORelations = new ArrayList<>();
        RDFObjectRelationDTO someAccessionChildDTORelation1 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmM.getURI()), someParentMURI.toString(), false);
        someAccessionChildDTORelations.add(someAccessionChildDTORelation1);
        RDFObjectRelationDTO someAccessionChildDTORelation2 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmM.getURI()), someParentM2URI.toString(), false);
        someAccessionChildDTORelations.add(someAccessionChildDTORelation2);
        RDFObjectRelationDTO someAccessionChildDTORelation3 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmF.getURI()), someParentFURI.toString(), false);
        someAccessionChildDTORelations.add(someAccessionChildDTORelation3);
        someChildDTO.setRelations(someAccessionChildDTORelations);
        final Response postResultChild1 = getJsonPostResponseAsAdmin(target(createPath), someChildDTO);
        URI child1Uri = extractUriFromResponse(postResultChild1);

        GermplasmCreationDTO someChildDTO2 = new GermplasmCreationDTO();
        someChildDTO2.setName("bla junior");
        someChildDTO2.setRdfType(new URI(Oeso.Species.toString()));
        List<RDFObjectRelationDTO> childDTORelations2 = new ArrayList<>();
        RDFObjectRelationDTO someVarietyChildDTORelation1 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmM.getURI()), child1Uri.toString(), false);
        someAccessionChildDTORelations.add(someVarietyChildDTORelation1);
        RDFObjectRelationDTO someVarietyChildDTORelation2 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasmF.getURI()), someParentFURI.toString(), false);
        childDTORelations2.add(someVarietyChildDTORelation2);
        someChildDTO2.setRelations(childDTORelations2);
        final Response postResultVarietyChild = getJsonPostResponseAsAdmin(target(createPath), someChildDTO2);
        URI uriNotUsedInAParentTillNow = extractUriFromResponse(postResultVarietyChild);

        GermplasmCreationDTO someChildWithNoParentTypeDTO = new GermplasmCreationDTO();
        someChildWithNoParentTypeDTO.setName("indiana jones");
        someChildWithNoParentTypeDTO.setRdfType(new URI(Oeso.Species.toString()));
        List<RDFObjectRelationDTO> someChildWithNoParentTypeDTORelations = new ArrayList<>();
        RDFObjectRelationDTO someChildWithNoParentTypeDTORelation1 = new RDFObjectRelationDTO(new URI(Oeso.hasParentGermplasm.getURI()), uriNotUsedInAParentTillNow.toString(), false);
        someChildWithNoParentTypeDTORelations.add(someChildWithNoParentTypeDTORelation1);
        someChildWithNoParentTypeDTO.setRelations(someChildWithNoParentTypeDTORelations);
        final Response postResultChildNoParentType = getJsonPostResponseAsAdmin(target(createPath), someChildWithNoParentTypeDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultChildNoParentType.getStatus());
        URI childWithNoParentTypeUri = extractUriFromResponse(postResultChildNoParentType);

        //Test search by parentA
        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("parent_germplasms_m", Collections.singletonList(someParentMURI));
            }
        };
        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();
        assertEquals(germplasmList.size(), 1);
        assertEquals(germplasmList.get(0).uri, child1Uri);

        //Test search by parent B
        params = new HashMap<String, Object>() {
            {
                put("parent_germplasms_f", Collections.singletonList(someParentFURI));
            }
        };
        searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        node = getResult.readEntity(JsonNode.class);
        germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        germplasmList = germplasmListResponse.getResult();
        assertEquals(germplasmList.size(), 2);

        //Test search parent with a parentA property
        params = new HashMap<String, Object>() {
            {
                put("parent_germplasms", Collections.singletonList(someParentMURI));
            }
        };
        searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        node = getResult.readEntity(JsonNode.class);
        germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        germplasmList = germplasmListResponse.getResult();
        assertEquals(germplasmList.size(), 1);
        assertEquals(germplasmList.get(0).uri, child1Uri);

        //Test search by parent when the child's parent was defined with no type (not a or b)
        params = new HashMap<String, Object>() {
            {
                put("parent_germplasms", Collections.singletonList(uriNotUsedInAParentTillNow));
            }
        };
        searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        node = getResult.readEntity(JsonNode.class);
        germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        germplasmList = germplasmListResponse.getResult();
        assertEquals(germplasmList.size(), 1);
        assertEquals(germplasmList.get(0).uri, childWithNoParentTypeUri);
    }


    @Test
    public void testGetByUri() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        SingleObjectResponse<GermplasmGetSingleDTO> getResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        GermplasmGetSingleDTO germplasmGetDto = getResponse.getResult();
        assertNotNull(germplasmGetDto);
    }

    @Test
    public void testSearch() throws Exception {
        GermplasmCreationDTO creationDTO = getCreationSpeciesDTO();
        getJsonPostResponseAsAdmin(target(createPath), creationDTO);

        Map<String, Object> params = new HashMap<>() {
            {
                put("name", getCreationSpeciesDTO().name);
                put("rdf_type", getCreationSpeciesDTO().getType());
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();

        assertFalse(germplasmList.isEmpty());
    }

    /**
     * Run this test at Dao level since the search method don't return attributes.
     * For now the {@link GermplasmDAO#search(GermplasmSearchFilter, boolean, boolean)} with fetchMetada = true
     * is only used into {@link GermplasmAPI#exportGermplasm(GermplasmSearchFilter)}.
     *
     */
    @Test
    public void testSearchWithMetadata() throws Exception {

        GermplasmDAO dao = new GermplasmDAO(getSparqlService(),getMongoDBService());

        // create two germplasm models with attributes
        GermplasmModel model1 = new GermplasmModel();
        model1.setLabel(new SPARQLLabel("germplasm1", ""));
        model1.setType(URI.create(Oeso.Species.toString()));

        Map<String,String> attributes = new HashMap<>();
        attributes.put("p1","v1");
        attributes.put("p2","v2");
        model1.setMetadata(new MetaDataModel(attributes));

        dao.create(model1);

        GermplasmModel model2 = new GermplasmModel();
        model2.setLabel(new SPARQLLabel("germplasm2", ""));
        model2.setType(URI.create(Oeso.Species.toString()));

        Map<String,String> attributes2 = new HashMap<>();
        attributes2.put("p3","v3");
        attributes2.put("p4","v4");
        model2.setMetadata(new MetaDataModel(attributes2));

        dao.create(model2);

        // search models and ensure that metadata are OK for each model
        ListWithPagination<GermplasmModel> models = dao.search(new GermplasmSearchFilter(),true, false);

        GermplasmModel model1FromDb = models.getList().stream()
                .filter(modelFromDb -> SPARQLDeserializers.compareURIs(modelFromDb.getUri(),model1.getUri()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("testSearchWithMetadata"));

        Assert.assertEquals(model1FromDb.getMetadata().getAttributes(),attributes);

        GermplasmModel model2FromDb = models.getList().stream()
                .filter(modelFromDb -> SPARQLDeserializers.compareURIs(modelFromDb.getUri(),model2.getUri()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("testSearchWithMetadata"));

        Assert.assertEquals(model2FromDb.getMetadata().getAttributes(),attributes2);
    }

    @Test
    public void testUpdate() throws Exception {

        // create a species
        GermplasmCreationDTO species = getCreationSpeciesDTO();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), species);

        // update the germplasm
        species.setUri(extractUriFromResponse(postResult));
        species.setName("new alias");
        
        //check that you can update species
        final Response updateSpecies = getJsonPutResponse(target(updatePath), species);
        assertEquals(Status.OK.getStatusCode(), updateSpecies.getStatus());        

    }

    @Test
    public void testDelete() throws Exception {

        // create the species that can be deleted and check if URI exists
        GermplasmCreationDTO speciesToDelete = getCreationSpeciesDTO();
        Response postResponse1 = getJsonPostResponseAsAdmin(target(createPath), speciesToDelete);
        URI uriToDelete = extractUriFromResponse(postResponse1);
        // delete the species that can be deleted 
        Response delResult = getDeleteByUriResponse(target(deletePath), uriToDelete.toString());
        assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

        // check if URI no longer exists
        Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uriToDelete.toString());
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());

    }

}
