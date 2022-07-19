//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.swagger.annotations.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.core.CoreModule;
import org.opensilex.core.URIsListPostDTO;
import org.opensilex.core.sharedResource.SharedResourcesDTO;
import org.opensilex.core.variable.api.characteristic.CharacteristicCreationDTO;
import org.opensilex.core.variable.api.entity.EntityCreationDTO;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityCreationDTO;
import org.opensilex.core.variable.api.method.MethodCreationDTO;
import org.opensilex.core.sharedResource.ImportUrisDTO;
import org.opensilex.core.variable.api.unit.UnitCreationDTO;
import org.opensilex.core.variable.dal.*;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Api(VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
@Path(VariableAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class VariableAPI {

    public static final String PATH = "/core/variables";

    public static final String CREDENTIAL_VARIABLE_GROUP_ID = "Variables";
    public static final String CREDENTIAL_VARIABLE_GROUP_LABEL_KEY = "credential-groups.variables";

    public static final String CREDENTIAL_VARIABLE_MODIFICATION_ID = "variable-modification";
    public static final String CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_VARIABLE_DELETE_ID = "variable-delete";
    public static final String CREDENTIAL_VARIABLE_DELETE_LABEL_KEY = "credential.default.delete";

    public static final String LOCAL_RESOURCE = "http://localhost";


    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService mongodb;
    @Inject
    private FileStorageService fs;

    @Inject
    private CoreModule coreModule;

    @Context
    protected HttpServletRequest httpRequest;


    @CurrentUser
    UserModel currentUser;

    private VariableDAO getDao() throws URISyntaxException {
        return new VariableDAO(sparql,mongodb,fs);
    }

    /*Cette fonction permet de récupérer la liste des ressources partagées disponibles indiquées dans le fichier opensilex.yml*/
    private List<SharedResourcesDTO> getAllSharedResources(

    ) throws Exception {

        //Création du dto de l'instance locale
        SharedResourcesDTO localInstance = new SharedResourcesDTO();
        localInstance.setUri(new URI(LOCAL_RESOURCE));
        localInstance.setLabel("component.sharedResources.local-instance");
        localInstance.setLocal(true);

        // création de la liste des dtos de toutes les ressources disponibles
        List<SharedResourcesDTO> sharedResourcesDTOS = new ArrayList<>();
        sharedResourcesDTOS.add(localInstance);
        sharedResourcesDTOS.addAll(coreModule.getSharedResources());

        return sharedResourcesDTOS;
    }

    /*Cette fonction renvoie un token au format String en fonction de l'adresse de la ressource donnée en paramètre (ex : http://138.102.159.36:8083/rest)*/
    /*La fonction utilise le compte admin pour se connecter (admin@opensilex.org / admin)*/
    private String getToken(String urlSharedResource)throws Exception {

        //URL du service qui génère le token
        URL urlToken = new URL(urlSharedResource + "/security/authenticate");
        //création de la connexion
        HttpURLConnection connection = (HttpURLConnection) urlToken.openConnection();
        //propriétés du service
        connection.setDoOutput(true); // POST
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        //paramètres du service
        String data = "{\"identifier\": \"admin@opensilex.org\", \"password\": \"admin\"}";
        byte[] out = data.getBytes(StandardCharsets.UTF_8);
        //envoi des paramètres en entrée
        OutputStream stream = connection.getOutputStream();
        stream.write(out);
        //lecture de la réponse
        String stringResponse = readResponse(connection);
        ObjectMapper mapperSearch = new ObjectMapper();
        JsonNode jsonResult = mapperSearch.readTree(stringResponse);
        //récupération du token au format String
        String token = "Bearer " + jsonResult.get("result").get("token").asText();

        connection.disconnect();
        return token;
    }

    /*Cette fonction renvoie la réponse (String) d'une connexion de type HttpURLConnection*/
    private String readResponse(HttpURLConnection connection)throws Exception {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String content = "";
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            content += inputLine;
        in.close();
        return content;
    }

    /*Cette fonction renvoie la réponse (String) d'un service (ex : http://138.102.159.36:8083/rest/ontology/shared_resources)*/
    private String connectionToService(String urlService, String token)throws Exception {

        // connexion au service
        URL url = new URL(urlService);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", token);

        // lecture de la réponse si la connexion a fonctionné
        int statut = connection.getResponseCode();
        if (statut == 200) {
            String Response = readResponse(connection);
            connection.disconnect();
            return Response;
        }
        return null;
    }

    /*Cette fonction renvoie la réponse (JsonNode) d'un service (ex : http://138.102.159.36:8083/rest/ontology/shared_resources)*/
    private JsonNode jsonResponseToService(String urlService, String token)throws Exception {

        // récupération de la réponse du service au format String
        String Response = connectionToService(urlService,token);
        // conversion de la réponse en JsonNode
        if (Response != null) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResult = mapper.readTree(Response);
            return jsonResult;
        }
        return null;
    }

    /*Cette fonction convertit un uri court en uri long si le prefixe de cet uri court existe dans la liste des prefixes de la bdd*/
    private URI prefixeTranslation(URI uri) throws Exception {

        // récupération des namespaces (indiqués dans la fonction getDefaultPrefixes() de SPARQLService.java)
        Map<String, String> nameSpaces = SPARQLService.getPrefixes();
        String prefixeUri = uri.getScheme();

        if (nameSpaces.get(prefixeUri) != null){
            URI translatedUri = new URI(uri.toString().replace(prefixeUri + ":",nameSpaces.get(prefixeUri)));
            return translatedUri;
        }else{
            return null;
        }
    }

    /*Cette fonction permet d'enregistrer tous les champs (n'existant pas dans la bdd locale) d'une variable partagée au moment de sa récupération sur l'instance locale*/
    private <M extends BaseVariableModel<M>, D extends BaseVariableCreationDTO<M>> URI createVariableElement(JsonNode variableJson, URI resource, String token, String fieldName, Class<M> modelClass, Class<D> creationDtoClass)
            throws Exception{

        URI shortUri = new URI("");

        JsonNode fieldUri = variableJson.get(fieldName).get("uri");
        if (fieldUri != null){
            String filedUriString = variableJson.get(fieldName).get("uri").asText();

            // vérification de l'existence du champ en local
            BaseVariableDAO<M> dao = new BaseVariableDAO<>(modelClass, sparql);
            M model = dao.get(new URI(filedUriString));

            // si le champ n'existe pas en local
            if (model == null) {
                JsonNode filedJsonResult;
                String urlService = "";

                // récupération des informations du champ dans la ressource partagée
                if (Objects.equals(fieldName, "entity")){
                    urlService += resource.toString() + "/core/entities/" + URLEncoder.encode(filedUriString, StandardCharsets.UTF_8.name());
                    filedJsonResult = jsonResponseToService(urlService, token);
                } else if (Objects.equals(fieldName, "entity_of_interest")) {
                    urlService += resource.toString() + "/core/entities_of_interest/" + URLEncoder.encode(filedUriString, StandardCharsets.UTF_8.name());
                    filedJsonResult = jsonResponseToService(urlService, token);
                }else{
                    urlService += resource.toString() + "/core/" + fieldName + "s/" + URLEncoder.encode(filedUriString, StandardCharsets.UTF_8.name());
                    filedJsonResult = jsonResponseToService(urlService, token);
                }

                // l'instanciation directe n'est pas possible car le type D n'est pas connu et hérite d'une classe abstraite
                D fieldDto = creationDtoClass.getConstructor().newInstance();

                // Ajout des informations récupérées sur la ressource partagée dans le CreationDto
                JsonNode result = filedJsonResult.get("result");

                fieldDto.setUri(new URI(result.get("uri").asText()));
                fieldDto.setName(result.get("name").asText());
                fieldDto.setDescription(result.get("description").asText());

                JsonNode jsonCloseMatch = result.get("close_match");
                JsonNode jsonBroadMatch = result.get("broad_match");
                JsonNode jsonExactMatch = result.get("exact_match");
                JsonNode jsonNarrowMatch = result.get("narrow_match");

                List<URI> listCloseMatch = new ArrayList<>();
                List<URI> listBroadMatch = new ArrayList<>();
                List<URI> listExactMatch = new ArrayList<>();
                List<URI> listNarrowMatch = new ArrayList<>();

                int rank = 0;
                while (jsonCloseMatch.get(rank) != null || jsonBroadMatch.get(rank) != null || jsonExactMatch.get(rank) != null || jsonNarrowMatch.get(rank) != null){
                    if (jsonCloseMatch.get(rank) != null){
                        listCloseMatch.add(new URI(jsonCloseMatch.get(rank).asText()));
                    }
                    if (jsonBroadMatch.get(rank) != null){
                        listBroadMatch.add(new URI(jsonBroadMatch.get(rank).asText()));
                    }
                    if (jsonExactMatch.get(rank) != null){
                        listExactMatch.add(new URI(jsonExactMatch.get(rank).asText()));
                    }
                    if (jsonNarrowMatch.get(rank) != null){
                        listNarrowMatch.add(new URI(jsonNarrowMatch.get(rank).asText()));
                    }
                    rank +=1;
                }
                if (!listCloseMatch.isEmpty()){
                    fieldDto.setCloseMatch(listCloseMatch);
                }
                if (!listBroadMatch.isEmpty()){
                    fieldDto.setBroadMatch(listBroadMatch);
                }
                if (!listExactMatch.isEmpty()){
                    fieldDto.setExactMatch(listExactMatch);
                }
                if (!listNarrowMatch.isEmpty()){
                    fieldDto.setNarrowMatch(listNarrowMatch);
                }

                // Cas particulier du champ Unit
                if (Objects.equals(creationDtoClass,UnitCreationDTO.class)){
                    UnitCreationDTO unitCreationDTO = (UnitCreationDTO) fieldDto; // on fait un cast pour modifier le type de dto
                    unitCreationDTO.setSymbol(result.get("symbol").asText());
                    unitCreationDTO.setAlternativeSymbol(result.get("alternative_symbol").asText());
                }

                // on utilise ensuite ce CreationDto pour enregistrer la variable
                BaseVariableDAO<M> fieldDao = new BaseVariableDAO<>(modelClass, sparql);
                M fieldModel = fieldDto.newModel();
                fieldModel.setCreator(currentUser.getUri());

                fieldDao.create(fieldModel);
                shortUri = new URI(SPARQLDeserializers.getShortURI(fieldModel.getUri().toString()));
            }
        }
        return shortUri;
    }

    @POST
    @ApiOperation("Add a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A variable is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A Variable with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    
    public Response createVariable(
            @ApiParam("Variable description") @Valid VariableCreationDTO dto
    ) throws Exception {
        try {
            VariableDAO dao = getDao();
            VariableModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Variable already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a variable")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variable retrieved", response = VariableDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown variable URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariable(
            @ApiParam(value = "Variable URI", example = "http://opensilex.dev/set/variables/Plant_Height", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Resource URI", example = "http://138.102.159.36:8083/rest") @QueryParam("resource") URI resource
    ) throws Exception {

        if(resource == null){
            VariableDAO dao = getDao();
            VariableModel variable = dao.get(uri);
            if (variable == null) {
                throw new NotFoundURIException(uri);
            }
            return new SingleObjectResponse<>(new VariableDetailsDTO(variable)).getResponse();

        }else{
            String token = getToken(resource.toString());
            String VariableURI = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name());
            // utilisation du service de recherche d'une variable en fonction de son uri dans la ressource partagée choisie
            String stringSearchResponse = connectionToService(resource.toString() + "/core/variables/" + VariableURI, token);

            // la variable n'existe pas dans la ressource
            if (stringSearchResponse == null) {
                throw new NotFoundURIException(uri);
            }else{  // la variable existe dans la ressource
                ObjectMapper mapperSearch = new ObjectMapper();
                JsonNode jsonResultSearch = mapperSearch.readTree(stringSearchResponse);
                // conversion de la réponse en VariableDetailsDTO
                SingleObjectResponse<VariableDetailsDTO> getResponse = mapperSearch.convertValue(jsonResultSearch, new TypeReference<SingleObjectResponse<VariableDetailsDTO>>() {
                });
                VariableDetailsDTO sharedVariableDetailsDto = getResponse.getResult();

                return new SingleObjectResponse<>(sharedVariableDetailsDto).getResponse();
            }
        }
    }


    @PUT
    @ApiOperation("Update a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variable updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown variable URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVariable(
            @ApiParam("Variable description") @Valid VariableUpdateDTO dto
    ) throws Exception {
        VariableDAO dao = getDao();

        VariableModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variable deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown variable URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariable(
            @ApiParam(value = "Variable URI", example = "http://opensilex.dev/set/variables/Plant_Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariableDAO dao = getDao();
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @ApiOperation(
            value = "Search variables",
            notes = "The following fields could be used for sorting : \n\n" +
                    " _entity_name/entityName : the name of the variable entity\n\n"+
                    " _characteristic_name/characteristicName : the name of the variable characteristic\n\n"+
                    " _method_name/methodName : the name of the variable method\n\n"+
                    " _unit_name/unitName : the name of the variable unit\n\n"
    )
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables", response = VariableGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariables(
            @ApiParam(value = "Resource") @QueryParam("resource") URI resource,
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern,
            @ApiParam(value = "Entity filter") @QueryParam("entity") @ValidURI URI entity,
            @ApiParam(value = "Entity of interest filter") @QueryParam("entity_of_interest") @ValidURI URI interestEntity,
            @ApiParam(value = "Characteristic filter") @QueryParam("characteristic") @ValidURI URI characteristic,
            @ApiParam(value = "Method filter") @QueryParam("method") @ValidURI URI method,
            @ApiParam(value = "Unit filter") @QueryParam("unit") @ValidURI URI unit,
            @ApiParam(value = "Group filter") @QueryParam("group_of_variables") @ValidURI URI group,
            @ApiParam(value = "Data type filter") @QueryParam("data_type") @ValidURI URI dataType,
            @ApiParam(value = "Time interval filter") @QueryParam("time_interval") String timeInterval,
            @ApiParam(value = "Species filter") @QueryParam("species") List<URI> species,
            @ApiParam(value = "Set this param to true to get associated data") @DefaultValue("false") @QueryParam("withAssociatedData") boolean withAssociatedData,
            @ApiParam(value = "Experiment filter") @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Scientific object filter") @QueryParam("scientific_objects") List<URI> objects,
            @ApiParam(value = "Device filter") @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        List<VariableGetDTO> resultDTOList = new ArrayList<>();
        int totalVariables = 0;

        // si la recherche se fait en local
        if (resource == null) {
            VariableDAO dao = getDao();
            ListWithPagination<VariableModel> variables = dao.search(
                    namePattern,
                    entity,
                    interestEntity,
                    characteristic,
                    method,
                    unit,
                    group,
                    dataType,
                    timeInterval,
                    species,
                    withAssociatedData,
                    devices,
                    experiments,
                    objects,
                    orderByList,
                    page,
                    pageSize,
                    this.currentUser
            );

            totalVariables = variables.getTotal();

            // récupération de la liste non paginée des dtos
            List<VariableGetDTO> listDTO = variables.convert(
                    VariableGetDTO.class,
                    VariableGetDTO::fromModel
            ).getList();

            // recherche de correspondance de l'uri de la ressource partagée avec le label de cette ressource pour l'afficher au survol du logo "shared"
            List<SharedResourcesDTO> sharedResourcesDTOList = getAllSharedResources();

            for (VariableGetDTO variableDto : listDTO) {
                String variableResourceUri = variableDto.getOnShared();
                int rankList = 1;
                boolean resourceFound = false;
                while (rankList < sharedResourcesDTOList.size() && !resourceFound){
                    if (Objects.equals(sharedResourcesDTOList.get(rankList).getUri().toString(), variableResourceUri)){
                        variableDto.setOnShared(sharedResourcesDTOList.get(rankList).getLabel());
                        resourceFound = true;
                    }
                    rankList += 1;
                }

                resultDTOList.add(variableDto);
            }

        }else{

            UriBuilder url = UriBuilder.fromUri(resource)
                    .path(PATH);
            // récupération des paramètres de la requête pour les recopier dans l'appel au service de recherche de variables sur la RP sélectionnée
            for (Map.Entry<String, String[]> entry : httpRequest.getParameterMap().entrySet()){
                url.queryParam(entry.getKey(),entry.getValue());
            }

            // utilisation du service de recherche des variables sur la ressource partagée
            String token = getToken(resource.toString());
            String SearchResponse = connectionToService(url.toString(), token);
            ObjectMapper mapperSearch = new ObjectMapper();
            JsonNode jsonResultSearch = mapperSearch.readTree(SearchResponse);

            // conversion du résultat en liste de dtos
            SingleObjectResponse<List<VariableGetDTO>> getResponse = mapperSearch.convertValue(jsonResultSearch, new TypeReference<SingleObjectResponse<List<VariableGetDTO>>>() {});
            List<VariableGetDTO> dtoFromApi = getResponse.getResult();

            // recherche en local si la variable existe pour afficher le logo "database"
            for(VariableGetDTO variableDto : dtoFromApi){
                URI sharedVariableUri = variableDto.getUri();
                VariableDAO dao = getDao();
                VariableModel variable;
                if (prefixeTranslation(sharedVariableUri) != null){
                    variable = dao.get(prefixeTranslation(sharedVariableUri));
                }else{
                    variable = dao.get(sharedVariableUri);
                }
                if (variable != null) {
                    variableDto.setOnLocal(true);
                }
                resultDTOList.add(variableDto);
            }

            // récupération du nombre total de variables pour la pagination
            MetadataDTO metadata = getResponse.getMetadata();
            long totalCount = metadata.getPagination().getTotalCount();
            totalVariables = (int)totalCount;
        }

        ListWithPagination<VariableGetDTO> resultDTOPaginatedList = new ListWithPagination<>(resultDTOList,page,pageSize, totalVariables);

        return new PaginatedListResponse<>(resultDTOPaginatedList).getResponse();

    }
    
    @GET
    @Path("details")
    @ApiOperation(
            value = "Search detailed variables by name, long name, entity, characteristic, method or unit name",
            notes = "The following fields could be used for sorting : \n\n" +
                    " _entity_name : the name of the variable entity\n\n"+
                    " _characteristic_name : the name of the variable characteristic\n\n"+
                    " _method_name : the name of the variable method\n\n"+
                    " _unit_name : the name of the variable unit\n\n"
    )
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return detailed variables", response = VariableDetailsDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariablesDetails(
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        VariableDAO dao = getDao();
        ListWithPagination<VariableModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<VariableDetailsDTO> resultDTOList = resultList.convert(
                VariableDetailsDTO.class,
                VariableDetailsDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("datatypes")
    @ApiOperation(value = "Get variables datatypes")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return data types", response = VariableDatatypeDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatatypes() throws URISyntaxException {

        List<VariableDatatypeDTO> variablesXsdTypes = Arrays.asList(
                new VariableDatatypeDTO(XSDDatatype.XSDboolean, "datatypes.boolean"),
                new VariableDatatypeDTO(XSDDatatype.XSDdate, "datatypes.date"),
                new VariableDatatypeDTO(XSDDatatype.XSDdateTime, "datatypes.datetime"),
                new VariableDatatypeDTO(XSDDatatype.XSDdecimal, "datatypes.decimal"),
                new VariableDatatypeDTO(XSDDatatype.XSDinteger, "datatypes.number"),
                new VariableDatatypeDTO(XSDDatatype.XSDstring, "datatypes.string")
        );

        return new PaginatedListResponse<>(variablesXsdTypes).getResponse();
    }

 /**
     * * Return a list of variables corresponding to the given URIs
     *
     * @param uris list of variables uri
     * @return Corresponding list of variables
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed variables by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables", response = VariableDetailsDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Variable not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getVariablesByURIs(
            @ApiParam(value = "Variables URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        VariableDAO dao = getDao();
        List<VariableModel> models = dao.getList(uris);

        if (!models.isEmpty()) {
            List<VariableDetailsDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(new VariableDetailsDTO(result));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Variables not found",
                    "Unknown variable URIs"
            ).getResponse();
        }
    }
    
    private Response buildCSVForClassicExport(List<VariableModel> variableList) throws IOException {
                // Convert list to DTO
        List<VariableExportDTOClassic> resultDTOList = new ArrayList<>();
        for (VariableModel variable : variableList) {
            VariableExportDTOClassic dto = VariableExportDTOClassic.fromModel(variable);
            resultDTOList.add(dto);          
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new VariableExportDTOClassic()); // to return an empty table
        }
        
        //Construct manually json to convert it to csv
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);
        List<JsonNode> list = new ArrayList<>();
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {               
                list.add(jsonNode);              
            }
        }
        
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(fieldName -> {
            csvSchemaBuilder.addColumn(fieldName);
        });
                        
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader().withArrayElementSeparator(" ");
        StringWriter str = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class).with(csvSchema).writeValue(str, arrayNode);

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_classic_variable" + dtf.format(date) + ".csv";

        return Response.ok(str.toString(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }

    private Response buildCSVForDetailsExport(List<VariableModel> variableList) throws IOException {
                // Convert list to DTO
        List<VariableExportDTODetails> resultDTOList = new ArrayList<>();
        for (VariableModel variable : variableList) {
            VariableExportDTODetails dto = VariableExportDTODetails.fromModel(variable);
            resultDTOList.add(dto);          
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new VariableExportDTODetails()); // to return an empty table
        }
        
        //Construct manually json to convert it to csv
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);
        List<JsonNode> list = new ArrayList<>();
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {               
                list.add(jsonNode);              
            }
        }
        
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(fieldName -> {
            csvSchemaBuilder.addColumn(fieldName);
        });
                        
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader().withArrayElementSeparator(" ");
        StringWriter str = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class).with(csvSchema).writeValue(str, arrayNode);

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_detailed_variable" + dtf.format(date) + ".csv";

        return Response.ok(str.toString(), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }
    
    @POST
    @Path("export_classic_by_uris")
    @ApiOperation("export variable by list of uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with variable list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response classicExportVariableByURIs(
            @ApiParam(value = "List of variable URI", example = "http://opensilex.dev/set/variables/Plant_Height") URIsListPostDTO dto
    ) throws Exception {
        VariableDAO dao = getDao();
        List<VariableModel> variableList = dao.getList(dto.getUris());
        
        return buildCSVForClassicExport(variableList);
        
    }

    @POST
    @Path("export_details_by_uris")
    @ApiOperation("export detailed variable by list of uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with detailed variable list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response detailsExportVariableByURIs(
            @ApiParam(value = "List of variable URI", example = "http://opensilex.dev/set/variables/Plant_Height") URIsListPostDTO dto
    ) throws Exception {
        VariableDAO dao = getDao();
        List<VariableModel> variableList = dao.getList(dto.getUris());
        
        return buildCSVForDetailsExport(variableList);
        
    }


    @POST
    @Path("import_on_local")
    @ApiOperation("Import the selected variables from the shared resources")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Import variables")
    })
    public Response importVariables(
            @ApiParam(value = "List of variable URI", required = true) ImportUrisDTO dto
    ) throws Exception {

        List<URI> uris = dto.getUris();
        List<URI> createdUris = new ArrayList<>();
        List<VariableDetailsDTO> variablesList;
        URI resource = dto.getResource();

        // construction de l'adresse du service avec l'uri encodé de chaque variable
        String token = getToken(resource.toString());
        String urlService = resource.toString() + "/core/variables/by_uris?";
        Boolean firstUri = true;

        for (URI uri : uris) {
            VariableDAO dao = getDao();
            VariableModel variable = dao.get(uri);
            if (variable == null) { // la variable à importer n'existe pas déjà en local
                if (firstUri) {
                    urlService += "uris=" + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name());
                    firstUri = false;
                } else {
                    urlService += "&uris=" + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name());
                }
            }
        }
        // utilisation du service de recherche des variables en fonction de leur uri sur la ressource partagée
        String stringResponse = connectionToService(urlService, token);
        JsonNode jsonResult;
        if (stringResponse != null) {
            ObjectMapper mapper = new ObjectMapper();
            jsonResult = mapper.readTree(stringResponse);
            SingleObjectResponse<List<VariableDetailsDTO>> getResponse = mapper.convertValue(jsonResult, new TypeReference<SingleObjectResponse<List<VariableDetailsDTO>>>() {});
            variablesList = getResponse.getResult();

            JsonNode result = jsonResult.get("result");
            int rank = 0;
            // boucle sur chaque variable renvoyée par le service
            while (result.get(rank) != null){
                JsonNode variableJson = result.get(rank);

                // récupération du nom de chaque champ de la variable
                List<String> variableFieldsList = new ArrayList<>();
                variableJson.fieldNames().forEachRemaining((fieldName) -> variableFieldsList.add(fieldName));

                // pour chaque métadonnée constituant la variable (unit, method, entity, entity of interest, characteristic)
                // on vérifie son existence en local et on la sauvegarde si elle n'existe pas en local
                if (variableFieldsList.contains("entity")){
                    URI shortUriEntity = createVariableElement(variableJson, resource, token, "entity", EntityModel.class, EntityCreationDTO.class);
                    if (!Objects.equals(shortUriEntity, new URI(""))){
                        createdUris.add(shortUriEntity);
                    }
                }

                if (variableFieldsList.contains("entity_of_interest")) {
                    URI shortUriInterestEntity = createVariableElement(variableJson, resource, token, "entity_of_interest", InterestEntityModel.class, InterestEntityCreationDTO.class);
                    if (!Objects.equals(shortUriInterestEntity, new URI(""))) {
                        createdUris.add(shortUriInterestEntity);
                    }
                }

                if (variableFieldsList.contains("characteristic")){
                    URI shortUriCharacteristic = createVariableElement(variableJson, resource, token, "characteristic", CharacteristicModel.class, CharacteristicCreationDTO.class);
                    if (!Objects.equals(shortUriCharacteristic, new URI(""))){
                        createdUris.add(shortUriCharacteristic);
                    }
                }

                if (variableFieldsList.contains("method")){
                    URI shortUriMethod = createVariableElement(variableJson, resource, token, "method", MethodModel.class, MethodCreationDTO.class);
                    if (!Objects.equals(shortUriMethod, new URI(""))){
                        createdUris.add(shortUriMethod);
                    }
                }

                if (variableFieldsList.contains("unit")){
                    URI shortUriUnit = createVariableElement(variableJson, resource, token, "unit", UnitModel.class, UnitCreationDTO.class);
                    if (!Objects.equals(shortUriUnit, new URI(""))){
                        createdUris.add(shortUriUnit);
                    }
                }

                // conversion du DetailDto en CreationDto
                VariableCreationDTO variableDto = VariableCreationDTO.fromDetailsDto(variablesList.get(rank));

                try {
                    // utilisation du CreationDto pour créer le model de la variable qui sera enregistrée
                    VariableDAO dao = getDao();
                    VariableModel model = variableDto.newModel();

                    URI translatedUri = prefixeTranslation(model.getUri());
                    if (translatedUri != null){
                        model.setUri(translatedUri);
                    }
                    model.setExactMatch(variableDto.getExactMatch());
                    model.setCloseMatch(variableDto.getCloseMatch());
                    model.setBroadMatch(variableDto.getBroadMatch());
                    model.setNarrowMatch(variableDto.getNarrowMatch());
                    model.setOnShared(resource);
                    model.setCreator(currentUser.getUri());

                    model = dao.create(model);
                    URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
                    createdUris.add(shortUri);

                } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
                    return new ErrorResponse(Response.Status.CONFLICT, "Variable already exists", duplicateUriException.getMessage()).getResponse();
                }

                rank++; // passage à la variable suivante
            }
        }

        return new ObjectUriResponse(Response.Status.CREATED, createdUris).getResponse();
    }
}













