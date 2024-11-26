//******************************************************************************
//                          ScientificObjectAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.XSD;
import org.geojson.GeoJsonObject;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.DataAPITest;
import org.opensilex.core.data.DataFileAPITest;
import org.opensilex.core.data.api.CriteriaDTO;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.api.DataFileCreationDTO;
import org.opensilex.core.data.api.SingleCriteriaDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.utils.MathematicalOperator;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentGetDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.api.FactorCreationDTO;
import org.opensilex.core.experiment.factor.api.FactorLevelCreationDTO;
import org.opensilex.core.experiment.factor.api.FactorLevelGetDTO;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.experiment.factors.api.FactorsAPITest;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.api.GermplasmAPITest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.provenance.api.ProvenanceAPITest;
import org.opensilex.core.provenance.api.ProvenanceCreationDTO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.skos.model.SkosModelTest;
import org.opensilex.core.variable.api.VariableApiTest;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * @author Vincent MIGOT
 * @author Jean Philippe VERT
 */
public class ScientificObjectAPITest extends AbstractMongoIntegrationTest {

    protected static final String path = "/core/scientific_objects";

    public static final String uriPath = path + "/{uri}";
    public static final String createPath = path + "/";
    public static final String updatePath = path + "/";
    public static final String deletePath = path + "/{uri}";
    public static final String searchPath = path + "/";



    public static final ServiceDescription create;

    static {
        try {
            create = new ServiceDescription(
                    ScientificObjectAPI.class.getMethod("createScientificObject", ScientificObjectCreationDTO.class),
                    createPath
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String searchGeomPath = path + "/geometry";
    public static final String exportGeospatialPath = path + "/export_geospatial";

    public static final String GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH = "http://www.opensilex.org/vocabulary/test-germplasm-restriction#";
    public static final Path GERMPLASM_RESTRICTION_ONTOLOGY_PATH = Paths.get("ontologies", "germplasmRestriction.owl");

    private int soCount = 1;
    private URI experiment;
    private URI speciesUri;

    private final DataAPITest dataApi = new DataAPITest();
    private final VariableApiTest variableApi = new VariableApiTest();


    private final static String EXPERIMENT_QUERY_PARAM = "experiment";

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void beforeTest() throws Exception {
        final Response postResultXP = getJsonPostResponseAsAdmin(target(ExperimentAPITest.createPath), ExperimentAPITest.getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResultXP.getStatus());
        final Response postResultGermplasm = getJsonPostResponseAsAdmin(target(GermplasmAPITest.createPath), GermplasmAPITest.getCreationSpeciesDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResultGermplasm.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        experiment = extractUriFromResponse(postResultXP);
        speciesUri = extractUriFromResponse(postResultGermplasm);
        final Response getResultXP = getJsonGetByUriResponseAsAdmin(target(ExperimentAPITest.uriPath), experiment.toString());
        assertEquals(Status.OK.getStatusCode(), getResultXP.getStatus());
    }

    @After
    public void afterTest() throws Exception {

        // delete xp
        final Response delResult = getDeleteByUriResponse(target(ExperimentAPITest.deletePath), experiment.toString());
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        // delete xp graph if some os has been inserted into experiment
        SPARQLService sparql = getSparqlService();
        Node experimentNode = NodeFactory.createURI(experiment.toString());
        if (sparql.count(experimentNode, ScientificObjectModel.class) > 0) {
            sparql.clearGraph(experiment);
        }

        final Response delResultGermplasm = getDeleteByUriResponse(target(GermplasmAPITest.deletePath), speciesUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), delResultGermplasm.getStatus());

        experiment = null;
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Arrays.asList(
                ScientificObjectModel.class,
                VariableModel.class,
                FactorModel.class);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Arrays.asList(
                GeospatialDAO.GEOSPATIAL_COLLECTION_NAME,
                DataDAO.DATA_COLLECTION_NAME,
                DataDAO.FILE_COLLECTION_NAME,
                ProvenanceDAO.PROVENANCE_COLLECTION_NAME
        );
    }

    protected ScientificObjectCreationDTO getCreationDTO(boolean withGeometry, boolean globalOs, boolean withGermplasm) throws Exception {
        ScientificObjectCreationDTO dto = new ScientificObjectCreationDTO();

        if (withGeometry) {
            List<Position> list = new LinkedList<>();
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97171243, 43.61332417));
            list.add(new Position(3.9717427, 43.61330558));
            list.add(new Position(3.97170272, 43.61327122));
            list.add(new Position(3.97167246, 43.61328981));
            list.add(new Position(3.97167246, 43.61328981));
            Geometry geometry = new Polygon(list);
            dto.setGeometry(geometryToGeoJson(geometry));
        }

        dto.setName("SO " + soCount++);
        dto.setType(new URI("http://www.opensilex.org/vocabulary/oeso#ScientificObject"));
        if(! globalOs){
            dto.setExperiment(experiment);
        }

        if (withGermplasm) {
            RDFObjectRelationDTO germplasmRelation = new RDFObjectRelationDTO();
            germplasmRelation.setProperty(new URI(Oeso.hasGermplasm.getURI()));
            germplasmRelation.setValue(speciesUri.toString());
            List<RDFObjectRelationDTO> relationList = new ArrayList<>();
            relationList.add(germplasmRelation);
            dto.setRelations(relationList);
        }

        return dto;
    }

    public ScientificObjectCreationDTO getCreationDTO(boolean withGeometry) throws Exception {
        return getCreationDTO(withGeometry, false, false);
    }

    public void testCreate(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getResponse(createdUri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreate() throws Exception {
        testCreate(true);
    }

    @Test
    public void testCreateWithoutGeometry() throws Exception {
        testCreate(false);
    }

    /**
     * Tests the automatic update of the species in an experiment based on their scientific objects.
     * This test needs the "hasGermplasm" restriction on scientific objects to work, thus the ontology
     * "germplasmRestriction" is added.
     *
     */
    @Test
    public void testCreateAndDeleteWithGermplasmWithRestriction() throws Exception {
        URI germplasmRestrictionOntologyGraphUri = new URI(GERMPLASM_RESTRICTION_ONTOLOGY_GRAPH);
        getSparqlService().loadOntology(germplasmRestrictionOntologyGraphUri,
                OpenSilex.getResourceAsStream(GERMPLASM_RESTRICTION_ONTOLOGY_PATH.toString()), Lang.RDFXML);

        ScientificObjectCreationDTO scientificObjectDTO = getCreationDTO(false, false, true);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObjectDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // Check that the experiment is updated with the correct species
        ExperimentGetDTO experimentGetDTO = retrieveExperiment(experiment);

        assertEquals(1, experimentGetDTO.getSpecies().size());
        assertEquals(SPARQLDeserializers.getShortURI(experimentGetDTO.getSpecies().get(0)),
                SPARQLDeserializers.getShortURI(speciesUri));

        // Remove the relation from the scientific object
        scientificObjectDTO.setUri(extractUriFromResponse(postResult));
        scientificObjectDTO.setRelations(Collections.emptyList());
        final Response putResult = getJsonPutResponse(target(updatePath), scientificObjectDTO);
        assertEquals(Status.OK.getStatusCode(), putResult.getStatus());

        // Check that the species of the experiment is gone
        experimentGetDTO = retrieveExperiment(experiment);

        assertEquals(0, experimentGetDTO.getSpecies().size());

        getSparqlService().clearGraph(germplasmRestrictionOntologyGraphUri);
    }

    /**
     * Tests that creating a scientific object with a germplasm fails when the "hasGermplasm" restriction is not present
     * in the ontology
     *
     */
    @Test
    public void testCreateWithGermplasmWithoutRestrictionShouldFail() throws Exception {
        ScientificObjectCreationDTO scientificObjectDTO = getCreationDTO(false, false, true);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObjectDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    private ExperimentGetDTO retrieveExperiment(URI experimentUri) throws Exception {
        Response experimentResponse = getJsonGetByUriResponseAsAdmin(target(ExperimentAPITest.uriPath), experimentUri.toString());
        JsonNode experimentNode = experimentResponse.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getExperimentResponse =
                mapper.convertValue(experimentNode, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {});
        return getExperimentResponse.getResult();
    }

    @Test
    public void createGloabalOSWithFactorLevelsShouldFail() throws Exception{
        List<FactorLevelGetDTO> factorLevelGetDTOS = createFactor(experiment);
        String factorLevelUri = factorLevelGetDTOS.get(0).getUri().toString();

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, true, false);
        RDFObjectRelationDTO factorLevelRelation = new RDFObjectRelationDTO(new URI(Oeso.hasFactorLevel.getURI()), factorLevelUri, false);
        creationDTO.setRelations(List.of(factorLevelRelation));

        new UserCallBuilder(ScientificObjectAPITest.create)
                .setBody(creationDTO)
                .buildAdmin()
                .executeCallAndAssertStatus(Status.BAD_REQUEST);
    }

    @Test
    public void createOsWithFactorLevelNotMatchingWithExpeShouldFail() throws Exception {
        //create expe that has no factor level but will not be linked to the os
        URI experiment2 = new UserCallBuilder(ExperimentAPITest.create)
                .setBody(ExperimentAPITest.getCreationDTO())
                .buildAdmin()
                .executeCallAndReturnURI();

        List<FactorLevelGetDTO> factorLevelGetDTOS = createFactor(experiment2);
        String factorLevelUri = factorLevelGetDTOS.get(0).getUri().toString();

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, false, false);
        RDFObjectRelationDTO factorLevelRelation = new RDFObjectRelationDTO(new URI(Oeso.hasFactorLevel.getURI()), factorLevelUri, false);
        creationDTO.setRelations(List.of(factorLevelRelation));

        new UserCallBuilder(ScientificObjectAPITest.create)
                .setBody(creationDTO)
                .buildAdmin()
                .executeCallAndAssertStatus(Status.BAD_REQUEST);
    }

    private List<FactorLevelGetDTO> createFactor(URI experiment) throws Exception {
        FactorCreationDTO dto = new FactorCreationDTO();
        dto.setName("Factor name");
        dto.setCategory(new URI("http://purl.obolibrary.org/obo/PECO_0007085"));
        dto.setDescription("Factor Description");
        // skos model
        SkosModelTest.setValidSkosReferences(dto);
        dto.setExperiment(experiment);
        // factors levels
        List<FactorLevelCreationDTO> factorsLevels = new ArrayList<>();
        FactorLevelCreationDTO factorLevelDto = new FactorLevelCreationDTO();
        factorLevelDto.setName("factorsLevel");
        factorLevelDto.setDescription("autogenerated");
        factorsLevels.add(factorLevelDto);
        FactorLevelCreationDTO factorLevelDto2 = new FactorLevelCreationDTO();
        factorLevelDto2.setName("factorsLevel2");
        factorLevelDto2.setDescription("autogenerated2");
        factorsLevels.add(factorLevelDto2);
        dto.setFactorLevels(factorsLevels);

        URI factorUri = new UserCallBuilder(new FactorsAPITest().create)
                .setBody(dto)
                .buildAdmin()
                .executeCallAndReturnURI();

        return new UserCallBuilder(new FactorsAPITest().getFactorLevels)
                .setUriInPath(factorUri)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<List<FactorLevelGetDTO>>>() {})
                .getDeserializedResponse()
                .getResult();
    }

    @Test
    public void testUpdateOsWithSameNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        creationDTO.setUri(extractUriFromResponse(postResult));

        final Response putWithSameNameResult = getJsonPutResponse(target(updatePath),creationDTO);
        assertEquals(Status.OK.getStatusCode(), putWithSameNameResult.getStatus());
    }


    @Test
    public void testCreateWithDuplicateNameFail() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        final Response postDuplicateResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), postDuplicateResult.getStatus());
    }

    @Test
    public void testUpdateWithDuplicateNameFail() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        ScientificObjectCreationDTO creationDTO2 = getCreationDTO(false);
        creationDTO2.setName(creationDTO.getName()+"_diff");
        final Response postResult2 = getJsonPostResponseAsAdmin(target(createPath),creationDTO2);
        assertEquals(Status.CREATED.getStatusCode(), postResult2.getStatus());

        creationDTO2.setName(creationDTO.getName());
        final Response duplicatePutResult = getJsonPostResponseAsAdmin(target(updatePath),creationDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), duplicatePutResult.getStatus());
    }

    @Test
    public void testCreateGlobalOSWithDuplicateNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, true, false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        final Response postDuplicateResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postDuplicateResult.getStatus());
    }

    @Test
    public void testUpdateGlobalOSWithDuplicateNameOk() throws Exception {

        ScientificObjectCreationDTO creationDTO = getCreationDTO(false, true, false);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath),creationDTO);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        creationDTO.setUri(extractUriFromResponse(postResult));

        ScientificObjectCreationDTO creationDTO2 = getCreationDTO(false, true, false);
        creationDTO2.setName(creationDTO.getName()+"_diff");
        final Response postResult2 = getJsonPostResponseAsAdmin(target(createPath),creationDTO2);
        assertEquals(Status.CREATED.getStatusCode(), postResult2.getStatus());
        creationDTO2.setUri(extractUriFromResponse(postResult2));

        creationDTO2.setName(creationDTO.getName());
        final Response duplicatePutResult = getJsonPutResponse(target(updatePath),creationDTO2);
        assertEquals(Status.OK.getStatusCode(), duplicatePutResult.getStatus());
    }

    private Response getResponse(URI createdUri) throws Exception {
        WebTarget target = target(uriPath).resolveTemplate("uri", createdUri.toString());
        target = target.queryParam("experiment", experiment.toString());

        return appendAdminToken(target).get();
    }

    @Test
    public void testUpdateWithNoUriFail() throws Exception {

        // create the so
        ScientificObjectCreationDTO soDTO = getCreationDTO(false);
        getJsonPostResponseAsAdmin(target(createPath), soDTO);

        final Response updateResult = getJsonPutResponse(target(updatePath), soDTO);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), updateResult.getStatus());
    }

    public void testUpdate(boolean withGeometry) throws Exception {
        // create the so
        ScientificObjectCreationDTO soDTO = getCreationDTO(withGeometry);
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), soDTO);

        // update the so
        soDTO.setUri(extractUriFromResponse(postResult));
        soDTO.setName("new alias");
        Geometry geometry = new Point(new Position(3.97167246, 43.61328981));
        soDTO.setGeometry(geometryToGeoJson(geometry));

        final Response updateResult = getJsonPutResponse(target(updatePath), soDTO);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new scientific object and compare it to the expected scientific object
        final Response getResult = getResponse(soDTO.getUri());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectDetailDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectDetailDTO>>() {
        });
        ScientificObjectDetailDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(soDTO.getName(), dtoFromApi.getName());
        assertEquals(soDTO.getType(), new URI(SPARQLDeserializers.getExpandedURI(dtoFromApi.getType())));
        assertEquals(soDTO.getGeometry().toString(), dtoFromApi.getGeometry().toString());
    }

    @Test
    public void testUpdate() throws Exception {
        testUpdate(true);
    }

    @Test
    public void testUpdateWithoutGeometry() throws Exception {
        testUpdate(false);
    }

    public void testDelete(boolean withGeometry) throws Exception {
        // create object and check if URI exists
        Response postResponse = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResponse);

        // delete object and check if URI no longer exists
        WebTarget getDeleteByUriTarget = target(deletePath);
        getDeleteByUriTarget = getDeleteByUriTarget.resolveTemplate("uri", uri);
        getDeleteByUriTarget = getDeleteByUriTarget.queryParam("experiment", experiment.toString());

        try(final Response delResult = appendAdminToken(getDeleteByUriTarget).delete()){
            assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

            final Response getResult = getResponse(uri);
            assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
        }

    }

    @Test
    public void testDelete() throws Exception {
        testDelete(true);
    }

    @Test
    public void testDeleteWithoutGeometry() throws Exception {
        testDelete(false);
    }

    public void testGetDetail(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getResponse(uri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectDetailDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectDetailDTO>>() {
        });
        ScientificObjectDetailDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test
    public void testGetDetail() throws Exception {
        testGetDetail(true);
    }

    @Test
    public void testGetDetailWithoutGeometry() throws Exception {
        testGetDetail(false);
    }

    @Test
    public void testGetByUriFail() throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(true));
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getResponse(new URI(uri + "7FG4FG89FG4GH4GH57"));
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    public void testGetScientificObjectsListByUris(boolean withGeometry) throws Exception {
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(withGeometry));
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getResponse(uri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectNodeDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectNodeDTO>>() {
        });
        ScientificObjectNodeDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }

    @Test
    public void testGetScientificObjectsListByUris() throws Exception {
        testGetScientificObjectsListByUris(true);
    }

    @Test
    public void testGetScientificObjectsListByUrisWithoutGeometry() throws Exception {
        testGetScientificObjectsListByUris(false);
    }

    @Test
    public void testSearchScientificObjectsWithGeometryListByUris() throws Exception {

        ScientificObjectCreationDTO dtoWithGeom = getCreationDTO(true);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dtoWithGeom);
        dtoWithGeom.setUri(extractUriFromResponse(postResult));

        ScientificObjectCreationDTO dtoWithoutGeom = getCreationDTO(false);
        postResult = getJsonPostResponseAsAdmin(target(createPath), dtoWithoutGeom);
        dtoWithoutGeom.setUri(extractUriFromResponse(postResult));

        // create new experimentation
        final Response postResultXP = getJsonPostResponseAsAdmin(target(ExperimentAPITest.createPath), ExperimentAPITest.getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResultXP.getStatus());

        ScientificObjectCreationDTO dtoOtherXP = getCreationDTO(true);
        dtoOtherXP.setExperiment(extractUriFromResponse(postResultXP));
        postResult = getJsonPostResponseAsAdmin(target(createPath), dtoOtherXP);
        dtoOtherXP.setUri(extractUriFromResponse(postResult));

        //search by context URI
        Map<String, Object> params = new HashMap<String, Object>() {{
            put(EXPERIMENT_QUERY_PARAM, experiment);
        }};

        List<ScientificObjectNodeDTO> results = getSearchResultsAsAdmin(searchGeomPath, params, new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {
        });
        assertEquals(1,results.size());
        assertEquals(dtoWithGeom.getUri(),new URI(SPARQLDeserializers.getExpandedURI(results.get(0).getUri())));
    }

    @Test
    public void testSearchScientificObjectsWithGeometryListByUrisWithoutExperimentFail() throws Exception {
        ScientificObjectCreationDTO dtoWithGeom = getCreationDTO(true);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dtoWithGeom);
        dtoWithGeom.setUri(extractUriFromResponse(postResult));

        final Response result = getJsonGetResponseAsAdmin(target(searchGeomPath));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getStatus());
    }

    @Test
    public void testDeleteWithChildrenFail() throws Exception {
        // create parent
        ScientificObjectCreationDTO parentDto = getCreationDTO(false,false,false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), parentDto);
        parentDto.setUri(extractUriFromResponse(postResult));

        // create child with link with parent
        ScientificObjectCreationDTO childDto = getCreationDTO(false,false,false);
        RDFObjectRelationDTO partOfRelation = new RDFObjectRelationDTO();
        partOfRelation.setProperty(URI.create(Oeso.isPartOf.getURI()));
        partOfRelation.setValue(parentDto.getUri().toString());
        childDto.setRelations(Collections.singletonList(partOfRelation));

        postResult = getJsonPostResponseAsAdmin(target(createPath), childDto);
        childDto.setUri(extractUriFromResponse(postResult));

        // ensure delete is a fail -> try to delete OS in a experiment but the OS has child
        try(final Response deleteResponse = appendAdminToken(target(deletePath)
                .resolveTemplate("uri", parentDto.getUri())
                .queryParam("experiment",experiment)).delete()){

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());

            JsonNode node = deleteResponse.readEntity(JsonNode.class);
            ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
            assertEquals("Scientific object can't be deleted : object has child", errorResponse.getResult().message);
        }

    }

    @Test
    public void testGlobalDeleteWhenOsIsInExperimentFail() throws Exception {

        ScientificObjectCreationDTO dto = getCreationDTO(false,false,false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dto);
        dto.setUri(extractUriFromResponse(postResult));

        // ensure delete is a fail -> try to delete OS globally but the OS is involved into an experiment

        try(final Response deleteResponse  = appendAdminToken(target(deletePath)
                .resolveTemplate("uri", dto.getUri()))
                .delete()){

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());
            JsonNode node = deleteResponse.readEntity(JsonNode.class);
            ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
            assertEquals("Scientific object can't be deleted : object is used into an experiment", errorResponse.getResult().message);
        }
    }

    private void testDeleteFail(boolean globalOs, URI objectURI, String expectedErrorMsg) throws Exception {

        // ensure delete is a fail -> try to delete OS with associated data
        WebTarget deleteTarget  = target(deletePath)
                .resolveTemplate("uri", objectURI);

        // if the OS has an experiment, then delete from xp fail because of associated data
        if(! globalOs){
            deleteTarget = deleteTarget.queryParam("experiment",experiment);
        }

        try(Response deleteResponse = appendAdminToken(deleteTarget).delete()){
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteResponse.getStatus());

            JsonNode node = deleteResponse.readEntity(JsonNode.class);
            ErrorResponse errorResponse = mapper.convertValue(node, new TypeReference<ErrorResponse>() {});
            assertEquals(expectedErrorMsg, errorResponse.getResult().message);
        }

    }

    private void testOsDeleteFailWhenAssociatedDataFile(boolean globalOs) throws Exception {

        // create provenance
        ProvenanceCreationDTO provenance = new ProvenanceCreationDTO();
        provenance.setName("testOsDeleteFailWhenAssociatedDataFile");
        Response postProvenanceResponse = getJsonPostResponseAsAdmin(target(new ProvenanceAPITest().createPath), provenance);
        provenance.setUri(extractUriFromResponse(postProvenanceResponse));

        // create OS
        ScientificObjectCreationDTO scientificObject = getCreationDTO(false, globalOs, false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObject);
        scientificObject.setUri(extractUriFromResponse(postResult));

        // create data provenance
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance.getUri());
        if(! globalOs){
            dataProvenance.setExperiments(Collections.singletonList(experiment));
        }else{
            dataProvenance.setExperiments(Collections.emptyList());
        }

        //  create data file
        DataFileCreationDTO dataFile = new DataFileCreationDTO();
        dataFile.setTarget(scientificObject.getUri());
        dataFile.setDate(LocalDate.now().toString());
        dataFile.setRdfType(URI.create(Oeso.Datafile.getURI()));
        dataFile.setProvenance(dataProvenance);

        // create tmp text file
        File file = tmpFolder.newFile("testOsDeleteFailWhenAssociatedDataFile");
        try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            outputStream.write("test_data_file".getBytes());
        }
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, APPLICATION_OCTET_STREAM_TYPE);

        try(FormDataMultiPart multiPart = new FormDataMultiPart()){
            multiPart.field("description", dataFile, MediaType.APPLICATION_JSON_TYPE).bodyPart(fileDataBodyPart);
            final Response postDataFileResult = getJsonPostResponseMultipart(target(DataFileAPITest.createPath), multiPart);
            dataFile.setUri(extractUriFromResponse(postDataFileResult));
        }

        testDeleteFail(globalOs,scientificObject.getUri(),"Scientific object can't be deleted : object has associated data files");
    }

    private void testOsDeleteFailWhenAssociatedData(boolean globalOs) throws Exception {

        // create provenance
        ProvenanceCreationDTO provenance = new ProvenanceCreationDTO();
        provenance.setName("testOsDeleteFailWhenAssociatedData");
        Response postProvenanceResponse = getJsonPostResponseAsAdmin(target(new ProvenanceAPITest().createPath), provenance);
        provenance.setUri(extractUriFromResponse(postProvenanceResponse));

        // create variable
        VariableApiTest variableApiTest = new VariableApiTest();
        VariableCreationDTO variable = variableApiTest.getCreationDto();
        Response postVariableResponse = getJsonPostResponseAsAdmin(target(variableApiTest.createPath), variableApiTest.getCreationDto());
        variable.setUri(extractUriFromResponse(postVariableResponse));

        // create OS
        ScientificObjectCreationDTO scientificObject = getCreationDTO(false, globalOs, false);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), scientificObject);
        scientificObject.setUri(extractUriFromResponse(postResult));

        // create associated data
        DataCreationDTO data = new DataCreationDTO();
        data.setValue(50);
        data.setTarget(scientificObject.getUri());
        data.setDate(LocalDate.now().toString());
        data.setVariable(variable.getUri());

        // create data provenance
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance.getUri());
        if(! globalOs){
            dataProvenance.setExperiments(Collections.singletonList(experiment));
        }else{
            dataProvenance.setExperiments(Collections.emptyList());
        }
        data.setProvenance(dataProvenance);
        Response postDataResponse = getJsonPostResponseAsAdmin(target(DataAPITest.CREATE_PATH),Collections.singletonList(data));
        data.setUri(extractUriListFromPaginatedListResponse(postDataResponse).get(0));

        testDeleteFail(globalOs,scientificObject.getUri(),"Scientific object can't be deleted : object has associated data");
    }
    @Test
    public void testOsDeleteFailWhenDataAssociated() throws Exception {
       testOsDeleteFailWhenAssociatedData(true);
    }

    @Test
    public void testOsDeleteFailWhenDataFilesAssociated() throws Exception {
        testOsDeleteFailWhenAssociatedDataFile(true);
    }

    @Test
    public void testOsDeleteFailWhenDataAssociatedWithinExperiment() throws Exception {
        testOsDeleteFailWhenAssociatedData(false);
    }

    @Test
    public void testOsDeleteFailWhenDataFilesAssociatedWithinExperiment() throws Exception {
        testOsDeleteFailWhenAssociatedDataFile(false);
    }

    //Criteria search tests :

    /**
     * Objects, Variables and data used for criteria search tests :
     */
    private URI createVariableOfDatatype(URI datatype) throws Exception {
        VariableCreationDTO variableCreationDTO = variableApi.getCreationDto();
        variableCreationDTO.setDataType(datatype);
        Response postResultVar = getJsonPostResponseAsAdmin(target(variableApi.createPath), variableCreationDTO);
        return extractUriFromResponse(postResultVar);
    }

    /**
     * Test to check criteria search when there are only "IsNotMeasured" operators
     */
    @Test
    public void testCriteriaSearchOnlyIsNotMeasureds() throws Exception {
        //create objects
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI osToReturn = extractUriFromResponse(postResult);
        postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI osToKickOff = extractUriFromResponse(postResult);

        //create variable
        URI integerVariable = createVariableOfDatatype(new URI(XSD.integer.getURI()));

        //Create data
        //Provenance for data :
        ProvenanceCreationDTO provenance = new ProvenanceCreationDTO();
        provenance.setName("criteriaSearchTestProv");
        Response postProvenanceResponse = getJsonPostResponseAsAdmin(target(new ProvenanceAPITest().createPath), provenance);
        provenance.setUri(extractUriFromResponse(postProvenanceResponse));

        // create data provenance
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance.getUri());
        dataProvenance.setExperiments(Collections.emptyList());

        DataCreationDTO falseIntData = dataApi.getCreationDataDTO("2020-10-11T10:29:06.402+0200", integerVariable, osToKickOff, 20, dataProvenance);
        DataCreationDTO falseIntData2 = dataApi.getCreationDataDTO("2020-10-12T10:29:06.402+0200", integerVariable, osToKickOff, 30, dataProvenance);
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        dtoList.add(falseIntData);
        dtoList.add(falseIntData2);
        final Response postResultData = getJsonPostResponseAsAdmin(target(DataAPITest.PATH), dtoList);
        LOGGER.info(postResultData.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        //Search test
        SingleCriteriaDTO someCriteria = new SingleCriteriaDTO();
        someCriteria.setVariableUri(integerVariable);
        someCriteria.setCriteria(MathematicalOperator.NotMeasured);

        CriteriaDTO criteriaForCurrentTest = new CriteriaDTO();
        criteriaForCurrentTest.setCriteriaList(Collections.singletonList(someCriteria));

        Map<String, Object> currentScientificObjectSearchParams = new HashMap<String, Object>() {
            {
                put("criteria_on_data", URLEncoder.encode(new ObjectMapper().writeValueAsString(criteriaForCurrentTest), "UTF-8"));
            }
        };
        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, currentScientificObjectSearchParams);
        Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        List<ScientificObjectNodeDTO> searchResult = getSearchResultsAsAdmin(
                searchPath,
                currentScientificObjectSearchParams,
                new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {});

        assertEquals(1, searchResult.size());
        assertTrue(SPARQLDeserializers.compareURIs(osToReturn, searchResult.get(0).getUri()));
    }

    /**
     * Test to check criteria search when there is a "Not measured" as well as some data
     */
    @Test
    public void testCriteriaSearchWithDataAndANotMeasured() throws Exception {
        //create objects
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI osToReturn = extractUriFromResponse(postResult);
        postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        //The next os will validate data criteria but will also have a value where we want it to be not measured
        URI osThatsAlmostGood= extractUriFromResponse(postResult);
        postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        //The next os will have only one data that does not validate the criteria
        URI osThatDoesNotVal= extractUriFromResponse(postResult);
        postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        //This os can do nothing right, it's a stupid os
        URI stupidOs= extractUriFromResponse(postResult);

        //create variables
        URI integerVariableToBeNotMeasured = createVariableOfDatatype(new URI(XSD.integer.getURI()));
        URI integerVariable = createVariableOfDatatype(new URI(XSD.integer.getURI()));

        //Create data
        //Provenance for data :
        ProvenanceCreationDTO provenance = new ProvenanceCreationDTO();
        provenance.setName("criteriaSearchTestProv");
        Response postProvenanceResponse = getJsonPostResponseAsAdmin(target(new ProvenanceAPITest().createPath), provenance);
        provenance.setUri(extractUriFromResponse(postProvenanceResponse));

        // create data provenance
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance.getUri());
        dataProvenance.setExperiments(Collections.emptyList());

        DataCreationDTO osThatsAlmostGoodData = dataApi.getCreationDataDTO("2020-10-11T10:29:06.402+0200", integerVariableToBeNotMeasured, osThatsAlmostGood, 20, dataProvenance);
        DataCreationDTO osThatsAlmostGoodData2 = dataApi.getCreationDataDTO("2020-10-12T10:29:06.402+0200", integerVariable, osThatsAlmostGood, 5, dataProvenance);
        DataCreationDTO osToReturnData = dataApi.getCreationDataDTO("2020-10-13T10:29:06.402+0200", integerVariable, osToReturn, 5, dataProvenance);
        DataCreationDTO otherOsThatsAlmostGoodData = dataApi.getCreationDataDTO("2020-10-14T10:29:06.402+0200", integerVariable, osThatDoesNotVal, 20, dataProvenance);
        DataCreationDTO stupidOsData = dataApi.getCreationDataDTO("2020-10-15T10:29:06.402+0200", integerVariableToBeNotMeasured, stupidOs, 20, dataProvenance);
        DataCreationDTO stupidOsData2 = dataApi.getCreationDataDTO("2020-10-16T10:29:06.402+0200", integerVariable, osThatDoesNotVal, 20, dataProvenance);
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        dtoList.add(osThatsAlmostGoodData);
        dtoList.add(osThatsAlmostGoodData2);
        dtoList.add(osToReturnData);
        dtoList.add(otherOsThatsAlmostGoodData);
        dtoList.add(stupidOsData);
        dtoList.add(stupidOsData2);
        final Response postResultData = getJsonPostResponseAsAdmin(target(DataAPITest.PATH), dtoList);
        LOGGER.info(postResultData.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        //Search test
        SingleCriteriaDTO notMeasuredCriteria = new SingleCriteriaDTO();
        notMeasuredCriteria.setVariableUri(integerVariableToBeNotMeasured);
        notMeasuredCriteria.setCriteria(MathematicalOperator.NotMeasured);

        SingleCriteriaDTO dataCriteria = new SingleCriteriaDTO();
        dataCriteria.setVariableUri(integerVariable);
        dataCriteria.setCriteria(MathematicalOperator.LessThan);
        dataCriteria.setValue("10");

        List<SingleCriteriaDTO> criteriaDTOList = new ArrayList<>();
        criteriaDTOList.add(notMeasuredCriteria);
        criteriaDTOList.add(dataCriteria);

        CriteriaDTO criteriaForCurrentTest = new CriteriaDTO();
        criteriaForCurrentTest.setCriteriaList(criteriaDTOList);

        Map<String, Object> currentScientificObjectSearchParams = new HashMap<String, Object>() {
            {
                put("criteria_on_data", URLEncoder.encode(new ObjectMapper().writeValueAsString(criteriaForCurrentTest), "UTF-8"));
            }
        };
        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, currentScientificObjectSearchParams);
        Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        List<ScientificObjectNodeDTO> searchResult = getSearchResultsAsAdmin(
                searchPath,
                currentScientificObjectSearchParams,
                new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {});

        assertEquals(1, searchResult.size());
        assertTrue(SPARQLDeserializers.compareURIs(osToReturn, searchResult.get(0).getUri()));
    }

    /**
     * Test to check criteria searches on int types, a decimal types and datetime types.
     * Also verifies that excluded objects from result are correct
     */
    @Test
    public void testCriteriaSearchWithNoIsNotMeasureds() throws Exception {
        //Create objects, for each ensure that the result is a well-formed URI, else throw exception
        //Separate objects for int tests and 2 other objects for other tests to make sure correct objects get excluded
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI osWithCorrectIntData = extractUriFromResponse(postResult);
        postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI osWithWrongIntData = extractUriFromResponse(postResult);
        postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI osWithCorrectDatas = extractUriFromResponse(postResult);
        postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false, true, false));
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());
        URI osWithWrongDatas = extractUriFromResponse(postResult);

        //create variables
        URI integerVariable = createVariableOfDatatype(new URI(XSD.integer.getURI()));
        URI decimalVariable = createVariableOfDatatype(new URI(XSD.decimal.getURI()));
        URI datetimeVariable = createVariableOfDatatype(new URI(XSD.dateTime.getURI()));

        //Create data
        //Provenance for data :
        ProvenanceCreationDTO provenance = new ProvenanceCreationDTO();
        provenance.setName("criteriaSearchTestProv");
        Response postProvenanceResponse = getJsonPostResponseAsAdmin(target(new ProvenanceAPITest().createPath), provenance);
        provenance.setUri(extractUriFromResponse(postProvenanceResponse));

        // create data provenance
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance.getUri());
        dataProvenance.setExperiments(Collections.emptyList());
        ArrayList<DataCreationDTO> dtoList = new ArrayList<>();
        DataCreationDTO falseIntData = dataApi.getCreationDataDTO("2020-10-11T10:29:06.402+0200", integerVariable, osWithWrongIntData, 20, dataProvenance);
        DataCreationDTO correctIntData = dataApi.getCreationDataDTO("2020-10-12T10:29:06.402+0200", integerVariable, osWithCorrectIntData, 5, dataProvenance);
        DataCreationDTO correctDecimalData = dataApi.getCreationDataDTO("2020-10-13T10:29:06.402+0200", decimalVariable, osWithCorrectDatas, 20.5, dataProvenance);
        DataCreationDTO falseDecimalData= dataApi.getCreationDataDTO("2020-10-14T10:29:06.402+0200", decimalVariable, osWithWrongDatas, 5.5, dataProvenance);
        DataCreationDTO correctDatetimeData = dataApi.getCreationDataDTO("2020-10-11T10:29:06.402+0200", datetimeVariable, osWithCorrectDatas, "2020-10-11T10:29:06.402+0200", dataProvenance);
        DataCreationDTO falseDatetimeData = dataApi.getCreationDataDTO("2020-10-12T10:29:06.402+0200", datetimeVariable, osWithWrongDatas, "2050-03-01T00:01:00Z", dataProvenance);
        dtoList.add(correctIntData);
        dtoList.add(falseIntData);
        dtoList.add(correctDecimalData);
        dtoList.add(falseDecimalData);
        dtoList.add(correctDatetimeData);
        dtoList.add(falseDatetimeData);
        final Response postResultData = getJsonPostResponseAsAdmin(target(DataAPITest.CREATE_PATH), dtoList);
        LOGGER.info(postResultData.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultData.getStatus());

        //Search tests :

        SingleCriteriaDTO lessThan10SingleCriteria = new SingleCriteriaDTO();
        lessThan10SingleCriteria.setVariableUri(integerVariable);
        lessThan10SingleCriteria.setCriteria(MathematicalOperator.LessThan);
        lessThan10SingleCriteria.setValue("10");

        SingleCriteriaDTO wrongDatatypeForIntCriteria = new SingleCriteriaDTO();
        wrongDatatypeForIntCriteria.setVariableUri(integerVariable);
        wrongDatatypeForIntCriteria.setCriteria(MathematicalOperator.LessThan);
        wrongDatatypeForIntCriteria.setValue("this is not an int idiot");

        SingleCriteriaDTO inexistingVariableCriteria = new SingleCriteriaDTO();
        inexistingVariableCriteria.setVariableUri(new URI("some_random_fake_uri"));
        inexistingVariableCriteria.setCriteria(MathematicalOperator.LessThan);
        inexistingVariableCriteria.setValue("this value shouldn't matter");

        SingleCriteriaDTO moreThan10Dot5SingleCriteria = new SingleCriteriaDTO();
        moreThan10Dot5SingleCriteria.setVariableUri(decimalVariable);
        moreThan10Dot5SingleCriteria.setCriteria(MathematicalOperator.MoreThan);
        moreThan10Dot5SingleCriteria.setValue("10.5");

        SingleCriteriaDTO equalToo20201011SingleCriteria = new SingleCriteriaDTO();
        equalToo20201011SingleCriteria.setVariableUri(datetimeVariable);
        equalToo20201011SingleCriteria.setCriteria(MathematicalOperator.EqualToo);
        equalToo20201011SingleCriteria.setValue("2020-10-11T10:29:06.402+0200");

        //Test a single criteria on int type
        CriteriaDTO criteriaForCurrentTest = new CriteriaDTO();
        criteriaForCurrentTest.setCriteriaList(Collections.singletonList(lessThan10SingleCriteria));

        Map<String, Object> currentScientificObjectSearchParams = new HashMap<String, Object>() {
            {
                put("criteria_on_data", URLEncoder.encode(new ObjectMapper().writeValueAsString(criteriaForCurrentTest), "UTF-8"));
            }
        };
        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, currentScientificObjectSearchParams);
        Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        List<ScientificObjectNodeDTO> searchResult = getSearchResultsAsAdmin(
                searchPath,
                currentScientificObjectSearchParams,
                new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {});

        assertEquals(1, searchResult.size());
        assertTrue(SPARQLDeserializers.compareURIs(osWithCorrectIntData, searchResult.get(0).getUri()));


        //Test that we get only osWithCorrectDatas using two criteria
        criteriaForCurrentTest.setCriteriaList(new ArrayList<>(Arrays.asList(moreThan10Dot5SingleCriteria, equalToo20201011SingleCriteria)));
        currentScientificObjectSearchParams = new HashMap<String, Object>() {
            {
                put("criteria_on_data", URLEncoder.encode(new ObjectMapper().writeValueAsString(criteriaForCurrentTest), "UTF-8"));
            }
        };
        searchTarget = appendSearchParams(target(searchPath), 0, 20, currentScientificObjectSearchParams);
        getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        searchResult = getSearchResultsAsAdmin(
                searchPath,
                currentScientificObjectSearchParams,
                new TypeReference<PaginatedListResponse<ScientificObjectNodeDTO>>() {});

        assertEquals(1, searchResult.size());
        assertTrue(SPARQLDeserializers.compareURIs(osWithCorrectDatas, searchResult.get(0).getUri()));

        //test error responses bad data type, invalid variable
        criteriaForCurrentTest.setCriteriaList(new ArrayList<>(Collections.singletonList(wrongDatatypeForIntCriteria)));
        currentScientificObjectSearchParams = new HashMap<String, Object>() {
            {
                put("criteria_on_data", URLEncoder.encode(new ObjectMapper().writeValueAsString(criteriaForCurrentTest), "UTF-8"));
            }
        };
        searchTarget = appendSearchParams(target(searchPath), 0, 20, currentScientificObjectSearchParams);
        getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), getResult.getStatus());
        criteriaForCurrentTest.setCriteriaList(new ArrayList<>(Collections.singletonList(inexistingVariableCriteria)));
        currentScientificObjectSearchParams = new HashMap<String, Object>() {
            {
                put("criteria_on_data", URLEncoder.encode(new ObjectMapper().writeValueAsString(criteriaForCurrentTest), "UTF-8"));
            }
        };
        searchTarget = appendSearchParams(target(searchPath), 0, 20, currentScientificObjectSearchParams);
        getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), getResult.getStatus());


    }


    private ScientificObjectCreationDTO getCreationDTO(URI experiment, String name, URI uri) throws Exception {
        ScientificObjectCreationDTO dto = new ScientificObjectCreationDTO();
        dto.setName(name);
        dto.setUri(uri);
        dto.setType(URI.create(Oeso.ScientificObject.getURI()));
        dto.setExperiment(experiment);
        Response postResult = getJsonPostResponseAsAdmin(target(createPath), dto);
        dto.setUri(extractUriFromResponse(postResult));
        return dto;
    }

    @Test
    public void testUriGenerationInExperimentalContext() throws Exception {

        // create two experiments
        SPARQLService sparql = getSparqlService();
        ExperimentModel xp = new ExperimentModel();
        xp.setStartDate(LocalDate.now());
        xp.setObjective("testGraphLocationAndUriGeneration");
        xp.setName("testGraphLocationAndUriGeneration");
        sparql.create(xp);

        ExperimentModel xp2 = new ExperimentModel();
        xp2.setStartDate(LocalDate.now());
        xp2.setObjective("testGraphLocationAndUriGeneration2");
        xp2.setName("testGraphLocationAndUriGeneration2");
        sparql.create(xp2);

        ScientificObjectCreationDTO os1 = getCreationDTO(xp.getUri(),"os1",null);

        // two os with same name but in different experiment -> /1
        ScientificObjectCreationDTO os1_1 =  getCreationDTO(xp2.getUri(),"os1",null);
        Assert.assertFalse(SPARQLDeserializers.compareURIs(os1.getUri(),os1_1.getUri()));
        Assert.assertTrue(os1_1.getUri().toString().endsWith("os1/1"));

        // os with unique name -> OK
        ScientificObjectCreationDTO os1_2 = getCreationDTO(xp2.getUri(),"os2",null);
        Assert.assertTrue(os1_2.getUri().toString().endsWith("os2"));

        // object URI reuse in another experiment
        ScientificObjectCreationDTO os1_reuse = getCreationDTO(xp2.getUri(),"os1_reuse",os1.getUri());
        Assert.assertEquals(os1.getUri(),os1_reuse.getUri());

        // re-insert OS inside same experiment -> duplicate URI error
        ScientificObjectCreationDTO duplicateInXp = new ScientificObjectCreationDTO();
        duplicateInXp.setName("os1");
        duplicateInXp.setUri(os1.getUri());
        duplicateInXp.setType(URI.create(Oeso.ScientificObject.getURI()));
        duplicateInXp.setExperiment(xp2.getUri());
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), getJsonPostResponseAsAdmin(target(createPath), duplicateInXp).getStatus());

        // global context
        URI defaultGraphURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        ScientificObjectCreationDTO os_global_1 =  getCreationDTO(null,"os_global_1",null);

        // two os with same name into global graph -> /1
        ScientificObjectCreationDTO os_global_1_1 = getCreationDTO(null,"os_global_1",null) ;
        Assert.assertFalse(SPARQLDeserializers.compareURIs(os_global_1.getUri(),os_global_1_1.getUri()));
        Assert.assertTrue(os_global_1_1.getUri().toString().endsWith("os_global_1/1"));

        // os with unique name -> OK
        ScientificObjectCreationDTO os_global_1_2 = getCreationDTO(null,"os_global_2",null);
        Assert.assertTrue(os_global_1_2.getUri().toString().endsWith("os_global_2"));

        // re-insert OS inside global graph -> duplicate URI error
        // re-insert OS inside same experiment -> duplicate URI error
        ScientificObjectCreationDTO duplicateInGlobal = new ScientificObjectCreationDTO();
        duplicateInXp.setName("os_global_1");
        duplicateInXp.setUri(os_global_1.getUri());
        duplicateInXp.setType(URI.create(Oeso.ScientificObject.getURI()));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), getJsonPostResponseAsAdmin(target(createPath), duplicateInGlobal).getStatus());
    }

    @Test
    public void testExportOSasShpandGeoJson() throws Exception {

        //Create one OS Model
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationDTO(false));
        URI uri = extractUriFromResponse(postResult);

        ScientificObjectModel osModel = new ScientificObjectModel();

        osModel.setName("SO " + soCount++);
        osModel.setUri(uri);
        osModel.setType(new URI("http://www.opensilex.org/vocabulary/oeso#ScientificObject"));
        osModel.setTypeLabel(new SPARQLLabel("ScientificObject","en"));

        SPARQLModelRelation germplasmRelation = new SPARQLModelRelation();
        germplasmRelation.setProperty(Oeso.hasGermplasm);
        germplasmRelation.setValue(speciesUri.toString());

        SPARQLModelRelation replicationRelation = new SPARQLModelRelation();
        replicationRelation.setProperty(Oeso.hasReplication);
        replicationRelation.setValue("2");

        List<SPARQLModelRelation> relationList = new ArrayList<>();
        relationList.add(germplasmRelation);
        relationList.add(replicationRelation);

        osModel.setRelations(relationList);

        //build geometry
        List<Position> list = new LinkedList<>();
        list.add(new Position(3.97167246, 43.61328981));
        list.add(new Position(3.97171243, 43.61332417));
        list.add(new Position(3.9717427, 43.61330558));
        list.add(new Position(3.97170272, 43.61327122));
        list.add(new Position(3.97167246, 43.61328981));
        list.add(new Position(3.97167246, 43.61328981));
        Geometry geometry = new Polygon(list);
        String geoJSON = geometry.toJson();
        GeoJsonObject geoJsonGeometry = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class);

        GeometryDTO objToExport=new GeometryDTO();
        objToExport.setGeometry(geoJsonGeometry);
        objToExport.setUri(osModel.getUri());

        ArrayList<GeometryDTO> objectsList= new ArrayList<>();
        objectsList.add(objToExport);

        //build params
        ArrayList<URI> propsList= new ArrayList<>();
        propsList.add(new URI(SPARQLDeserializers.getShortURI(Oeso.hasGeometry.getURI())));
        propsList.add(new URI("vocabulary:hasFactorLevel"));
        propsList.add(new URI("vocabulary:hasGermplasm"));
        propsList.add(new URI("vocabulary:hasReplication"));

        Map<String, Object> paramsShp = new HashMap<>() {{
            put(EXPERIMENT_QUERY_PARAM, experiment);
            put("format", "shp");
            put("selected_props",propsList);
            put("pageSize",10000);
        }};

        Map<String, Object> paramsGJson = new HashMap<>() {{
            put(EXPERIMENT_QUERY_PARAM, experiment);
            put("format", "geojson");
            put("selected_props",propsList);
            put("pageSize",10000);
        }};

        // assert service
        final Response resultShp =  getOctetPostResponseAsAdmin(appendQueryParams(target(exportGeospatialPath),paramsShp),objectsList);
        assertEquals(Status.OK.getStatusCode(), resultShp.getStatus());
        // assert service
        final Response resultGJson =  getOctetPostResponseAsAdmin(appendQueryParams(target(exportGeospatialPath),paramsGJson),objectsList);
        assertEquals(Status.OK.getStatusCode(), resultGJson.getStatus());
    }
}
