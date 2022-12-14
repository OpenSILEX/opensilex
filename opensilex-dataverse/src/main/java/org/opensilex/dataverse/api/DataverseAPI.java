//******************************************************************************
//                          DataverseModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dataverse.api;

//import com.researchspace.dataverse.api.v1.DataverseConfig;
//import com.researchspace.dataverse.api.v1.DataverseOperations;
//import com.researchspace.dataverse.entities.Identifier;
//import com.researchspace.dataverse.entities.facade.DatasetFacade;
//import com.researchspace.dataverse.http.DataverseAPIImpl;

import com.researchspace.dataverse.entities.Identifier;
import io.swagger.annotations.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;


/**
 * @author Gabriel Besombes
 */
@Api("Dataverse")
@Path("/dataverse")
@ApiCredentialGroup(
        groupId = DataverseAPI.CREDENTIAL_DATAVERSE_GROUP_ID,
        groupLabelKey = DataverseAPI.CREDENTIAL_DATAVERSE_GROUP_LABEL_KEY
)
public class DataverseAPI {
    public static final String CREDENTIAL_DATAVERSE_GROUP_ID = "dataverse";
    public static final String CREDENTIAL_DATAVERSE_GROUP_LABEL_KEY = "credential-group-dataverse";
    public static final String DATAVERSE_EXAMPLE_API_KEY = "****-***-***-****";

    private static final String SCHEME = "https";
    private static final String BASE_URL = "data-preproduction.inrae.fr";
    private static final String SEARCH_ENDPOINT = "search";
    private static final String QUERY_PARAMETER = "q";
    private static final String FORMAT_PARAMETER = "format";
    private static final String EXPERIMENT_EXAMPLE_URI = "dev:id/experiment/string";
    private static final String DATAVERSE_BASEPATH_EXAMPLE_URL = "https://data-preproduction.inrae.fr";
    private static final String DATAVERSE_EXAMPLE_URI = "opensilex-tests";

    @Inject
    SPARQLService sparql;
    @Inject
    MongoDBService nosql;
    @CurrentUser
    UserModel currentUser;
    /**
     * Create a Dataset on a Dataverse
     *
     * @param xpUri the uri of the experiment to create as a dataset
     * @param dataverseBasePath the url of the dataverse server
     * @param dataverseAlias alias of the dataverse inside which to create the dataset
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Experiment {@link URI}
     * @throws java.lang.Exception
     */
    @POST
    @ApiOperation("Create experiment as a Dataset")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DATAVERSE_GROUP_ID,
            credentialLabelKey = CREDENTIAL_DATAVERSE_GROUP_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A dataset was created", response = Response.class)
    })
    public Response createDataset(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true)  @QueryParam("uri") @NotNull URI xpUri,
            @ApiParam(value = "Dataverse API base path", example = DATAVERSE_BASEPATH_EXAMPLE_URL, required = true)  @QueryParam("dataverseBasePath") @NotNull URI dataverseBasePath,
            @ApiParam(value = "Parent dataverse's alias", example = DATAVERSE_EXAMPLE_URI, required = true)  @QueryParam("dataverseAlias") @NotNull String dataverseAlias,
            @ApiParam(value = "Dataverse API key", example = DATAVERSE_EXAMPLE_API_KEY, required = true)  @QueryParam("externalAPIKey") @NotNull String externalAPIKey
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql, nosql);
        ExperimentModel experimentModel = dao.get(xpUri, currentUser);
        DataverseAPIPostDatasetDTO datasetDTO = new DataverseAPIPostDatasetDTO(experimentModel);
        DataverseClient dataverseClient = new DataverseClient();
        Identifier datasetId = dataverseClient.createADataset(datasetDTO);
        return new SingleObjectResponse<Identifier>(datasetId).getResponse();
//        return null;

//        String xpName = model.getName();
//        URI url = new URI("https://data-preproduction.inrae.fr/api/dataverses/opensilex-tests/datasets");
//        // TODO : Create objects for datasetVersion, metadataBlocks, citation and fields
//        String toPost = "{\n" +
//                "  \"datasetVersion\":{\n" +
//                "    \"metadataBlocks\":{\n" +
//                "      \"citation\":{\n" +
//                "        \"displayName\":\"Common Metadata\",\n" +
//                "        \"fields\":[\n" +
//                "          {\n" +
//                "            \"typeName\":\"title\",\n" +
//                "            \"multiple\":false,\n" +
//                "            \"typeClass\":\"primitive\",\n" +
//                "            \"value\":\"" + xpName + "\"\n" +
//                "          }\n" +
//                "        ]\n" +
//                "      }\n" +
//                "    }\n" +
//                "  }\n" +
//                "}";
//        JSONParser parser = new JSONParser();
//        JSONObject jsonToPost = (JSONObject) parser.parse(toPost);
//        Form form = new Form();
//        form.param("upload-file", toPost);
//        Client httpClient = ClientBuilder.newClient(); // TODO : Should I make a separate class for the client?
//        JSONObject jsonResponse = httpClient.target(url)
//                .request(MediaType.APPLICATION_JSON_TYPE)
//                .header("X-Dataverse-key", API_KEY)
//                .post(Entity.json(jsonToPost), JSONObject.class);
//
//        String responseData = jsonResponse.get("data").toString();
//
//        return new SingleObjectResponse<String>(responseData).getResponse();

//        TODO : Look at API_TOKEN renewal : https://guides.dataverse.org/en/latest/api/auth.html
    }
}
