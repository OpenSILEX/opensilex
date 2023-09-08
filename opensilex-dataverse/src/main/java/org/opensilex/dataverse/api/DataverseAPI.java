//******************************************************************************
//                          DataverseModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dataverse.api;

import com.researchspace.dataverse.entities.Identifier;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.config.InvalidConfigException;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.dataverse.DataverseModule;
import org.opensilex.dataverse.OpensilexDataverseConfig;
import org.opensilex.dataverse.ontology.OesoDataverse;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.Date;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidLanguage;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.model.OpenSilexLocale;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * @author Gabriel Besombes
 */
@Api(DataverseAPI.CREDENTIAL_DATAVERSE_GROUP_ID)
@Path("/dataverse")
@ApiCredentialGroup(
        groupId = DataverseAPI.CREDENTIAL_DATAVERSE_GROUP_ID,
        groupLabelKey = DataverseAPI.CREDENTIAL_DATAVERSE_GROUP_LABEL_KEY
)
public class DataverseAPI {
    public static final String CREDENTIAL_DATAVERSE_GROUP_ID = "Dataverse";
    public static final String CREDENTIAL_DATAVERSE_GROUP_LABEL_KEY = "credential-groups.dataverse";

    public static final String CREDENTIAL_DATAVERSE_MODIFICATION_ID = "dataverse-modification";
    public static final String CREDENTIAL_DATAVERSE_MODIFICATION_LABEL_KEY = "credential.default.modification";
    private static final String EXPERIMENT_EXAMPLE_URI = "dev:id/experiment/bc-test-release";
    private static final String DATASET_TITLE_EXAMPLE = "Test_dataset";
    private static final String DATASET_AUTHORS_EXAMPLE = "dev:id/user/person.test.dataverse";
    private static final String DATASET_CONTACTS_EXAMPLE = "dev:id/user/person.test.dataverse";
    private static final String DATASET_LANGUAGE_EXAMPLE = "en";
    private static final String DATASET_METADATA_LANGUAGE_EXAMPLE = "en";
    private static final String DATASET_PRODUCTION_DATE = "2020-02-20";
    private static final String DATASET_DEPRECATED_DATE = "false";
    private static final String DATASET_RDF_TYPE = "http://www.opensilex.org/vocabulary/oeso-dataverse#RechercheDataGouvDataset";


    @Inject
    private DataverseModule dataverseModule;
    @Inject
    SPARQLService sparql;
    @Inject
    MongoDBService nosql;
    @Inject
    FileStorageService fs;

    @CurrentUser
    AccountModel currentAccount;
    /**
     * Create a Dataset on a Dataverse
     *
     * @param experimentUri the uri of the experiment to create as a dataset
     * @param dataverseBasePath the url of the dataverse server
     * @param dataverseAlias alias of the dataverse inside which to create the dataset
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Experiment {@link URI}
     * @throws java.lang.Exception
     */
    @POST
    @ApiOperation(
            value = "Create experiment as a Dataset",
            notes = "To consult the document created use the Document API"
    )
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DATAVERSE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATAVERSE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A dataset was created", response = URI.class)
    })
    public Response createDataset(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true)  @QueryParam("experimentUri") @NotNull @ValidURI URI experimentUri,
            @ApiParam(value = "Dataset Title", example = DATASET_TITLE_EXAMPLE, required = true)  @QueryParam("datasetTitle") @NotNull String datasetTitle,
            @ApiParam(value = "Dataset Authors", example = DATASET_AUTHORS_EXAMPLE, required = true)  @QueryParam("datasetAuthors") @NotNull @ValidURI List<URI> datasetAuthors,
            @ApiParam(value = "Dataset Contacts", example = DATASET_CONTACTS_EXAMPLE, required = true)  @QueryParam("datasetContacts") @NotNull @ValidURI List<URI> datasetContacts,
            @ApiParam(value = "Dataset Language from list of values : [en, fr]", example = DATASET_LANGUAGE_EXAMPLE, required = true)  @QueryParam("datasetLanguage") @NotNull @ValidLanguage(moduleClass = DataverseModule.class, configClass = OpensilexDataverseConfig.class, configKey = "dataverseLanguages") OpenSilexLocale datasetLanguage,
            @ApiParam(value = "Dataset Metadata Language from list of values : [en, fr]", example = DATASET_METADATA_LANGUAGE_EXAMPLE, required = true)  @QueryParam("datasetMetadataLanguage") @NotNull @ValidLanguage(moduleClass = DataverseModule.class, configClass = OpensilexDataverseConfig.class, configKey = "dataverseLanguages") OpenSilexLocale datasetMetadataLanguage,
            @ApiParam(value = "URI of the rdf_type of the dataset", example = DATASET_RDF_TYPE, required = true)  @QueryParam("datasetRDFType") @NotNull @ValidURI URI datasetRDFType,
            @ApiParam(value = "Dataset Production Date", example = DATASET_PRODUCTION_DATE)  @QueryParam("productionDate") @Date(DateFormat.YMD) String productionDate,
            @ApiParam(value = "Dataset URI")  @QueryParam("datasetUri") @ValidURI URI datasetUri,
            @ApiParam(value = "Dataset deprecated", example = DATASET_DEPRECATED_DATE)  @QueryParam("datasetDeprecated") boolean datasetDeprecated,
            @ApiParam(value = "Dataverse API base path")  @QueryParam("dataverseBasePath") URL dataverseBasePath,
            @ApiParam(value = "Parent dataverse alias")  @QueryParam("dataverseAlias") String dataverseAlias,
            @ApiParam(value = "Dataverse API key")  @QueryParam("externalAPIKey") String externalAPIKey
    ) throws Exception {

        boolean isType = SPARQLModule.getOntologyStoreInstance()
                .classExist(datasetRDFType, URI.create(OesoDataverse.DataverseDataset.getURI()));
        if (!isType) {
            throw new BadRequestException("Wrong rdf_type. Must be a subtype of " + URI.create(OesoDataverse.DataverseDataset.getURI()));
        }


        ExperimentDAO expeDAO = new ExperimentDAO(sparql, nosql);
        ExperimentModel experimentModel = expeDAO.get(experimentUri, currentAccount);

        PersonDAO personDAO = new PersonDAO(sparql);
        List<PersonModel> authorsModels = personDAO.getList(datasetAuthors);
        List<PersonModel> contactsModels = personDAO.getList(datasetContacts);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date date = format.parse(productionDate);

        DataverseAPIPostDatasetDTO datasetDTO = new DataverseAPIPostDatasetDTO(experimentModel, currentAccount, datasetTitle, authorsModels, contactsModels, datasetLanguage, datasetMetadataLanguage, date);
        OpensilexDataverseConfig opensilexDataverseConfig = dataverseModule.getConfig(OpensilexDataverseConfig.class);
        if (dataverseBasePath == null || StringUtils.isEmpty(dataverseBasePath.toString())) {
            dataverseBasePath = new URL(opensilexDataverseConfig.rechercheDataGouvBasePath());
        }
        if (StringUtils.isEmpty(dataverseAlias)) {
            dataverseAlias = opensilexDataverseConfig.dataverseAlias();
        }
        if (StringUtils.isEmpty(externalAPIKey)) {
            externalAPIKey = opensilexDataverseConfig.externalAPIKey();
        }
        DataverseClient dataverseClient = new DataverseClient(dataverseBasePath, dataverseAlias, externalAPIKey);
        Identifier datasetId = dataverseClient.createADataset(datasetDTO);

        DocumentModel docModel = new DocumentModel();
        docModel.setUri(datasetUri);
        docModel.setPublisher(currentAccount.getUri());
        docModel.setTargets(Collections.singletonList(experimentUri));
        docModel.setTitle(datasetTitle);
        docModel.setIdentifier(datasetId.getPersistentId());
        docModel.setSource(
                UriBuilder.fromUri(dataverseBasePath.toURI())
                        .path("dataset.xhtml")
                        .queryParam("persistentId", datasetId.getPersistentId())
                        .build()
        );
        docModel.setType(datasetRDFType);
        docModel.setDeprecated(Boolean.toString(datasetDeprecated));
        docModel.setDate(productionDate);
        if(!datasetDTO.getDescription().isEmpty()){
            docModel.setDescription(datasetDTO.getDescription());
        } else {
            docModel.setDescription(datasetDTO.getTopic());
        }
        docModel.setLanguage(datasetLanguage.getDisplayLanguage(currentAccount.getLocale()));
        docModel.setAuthors(datasetAuthors.stream().map(URI::toString).collect(Collectors.toList()));

        DocumentDAO docDAO = new DocumentDAO(sparql, nosql, fs);
        docDAO.createWithSource(docModel);
        return new ObjectUriResponse(docModel.getUri()).getResponse();
    }

    /**
     * GET available dataset languages
     *
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the available dataset languages
     */
    @GET
    @ApiOperation(
            value = "Get the available dataset languages"
    )
    @ApiProtected
    @Path("datasetLanguages")
    @ApiCredential(
            credentialId = CREDENTIAL_DATAVERSE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATAVERSE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Available dataset languages retrieved successfully", response = HashMap.class)
    })
    public Response availableDatasetLanguages() throws InvalidConfigException {
        HashMap<String, String> availableLanguages = new HashMap<>();
        for (String lang : dataverseModule.getConfig(OpensilexDataverseConfig.class).dataverseLanguages()){
            Locale langLocale = OpenSilexLocale.getLocaleFromString(lang)
                    .orElseThrow(() -> new InvalidConfigException("String '" + lang + "' from instance configuration couldn't be matched with a known language"));
            availableLanguages.put(langLocale.getLanguage(), langLocale.getDisplayLanguage(currentAccount.getLocale()));
        }
        return new SingleObjectResponse<HashMap>(availableLanguages).getResponse();
    }

    /**
     * GET available metadata dataset languages
     *
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the available metadata languages for datasets
     */
    @GET
    @ApiOperation(
            value = "Get the available dataset metadata languages"
    )
    @ApiProtected
    @Path("datasetMetadataLanguages")
    @ApiCredential(
            credentialId = CREDENTIAL_DATAVERSE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATAVERSE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Available dataverse languages retrieved successfully", response = HashMap.class)
    })
    public Response availableDatasetMetadataLanguages() throws InvalidConfigException {
        HashMap<String, String> availableDatasteMetadataLanguages = new HashMap<>();
        for (String lang : dataverseModule.getConfig(OpensilexDataverseConfig.class).datasetMetadataLanguages()){
            Locale langLocale = OpenSilexLocale.getLocaleFromString(lang)
                    .orElseThrow(() -> new InvalidConfigException("String '" + lang + "' from instance configuration couldn't be matched with a known language"));
            availableDatasteMetadataLanguages.put(langLocale.getLanguage(), langLocale.getDisplayLanguage(currentAccount.getLocale()));
        }
        return new SingleObjectResponse<HashMap>(availableDatasteMetadataLanguages).getResponse();
    }

    /**
     * GET Recherche Data Gouv Base Path
     *
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the Recherche Data Gouv Base Path
     */
    @GET
    @ApiOperation(
            value = "Get the Recherche Data Gouv url from the instance configuration"
    )
    @ApiProtected
    @Path("RechercheDataGouvBasePath")
    @ApiCredential(
            credentialId = CREDENTIAL_DATAVERSE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATAVERSE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Recherche Data Gouv Base Path retrieved successfully", response = URI.class)
    })
    public Response rechercheDataGouvBasePath() {
        OpensilexDataverseConfig opensilexDataverseConfig = dataverseModule.getConfig(OpensilexDataverseConfig.class);
        return new SingleObjectResponse(opensilexDataverseConfig.rechercheDataGouvBasePath()).getResponse();
    }

}
