//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.api;

import io.swagger.annotations.*;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.security.SecurityConfig;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.exceptions.ServiceUnavailableException;
import org.opensilex.server.response.*;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.util.*;

/**
 * <pre>
 * Person API for OpenSilex which provides:
 *
 * - create: Create a person
 * </pre>
 *
 * @author Yvan Roux
 */
@Api(SecurityModule.REST_SECURITY_API_ID)
@Path("/security/persons")
@ApiCredentialGroup(
        groupId = PersonAPI.CREDENTIAL_GROUP_PERSON_ID,
        groupLabelKey = PersonAPI.CREDENTIAL_GROUP_PERSON_LABEL_KEY
)
public class PersonAPI {
    public static final String CREDENTIAL_GROUP_PERSON_ID = "Persons";
    public static final String CREDENTIAL_GROUP_PERSON_LABEL_KEY = "credential-groups.persons";

    public static final String CREDENTIAL_PERSON_MODIFICATION_ID = "person-modification";
    public static final String CREDENTIAL_PERSON_MODIFICATION_LABEL_KEY = "credential.default.modification";
    private static final String CREDENTIAL_PERSON_DELETE_ID = "person-delete";
    private static final String CREDENTIAL_PERSON_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel currentUser;

    /**
     * Create a person and return its URI
     *
     * @param personDTO person model to create
     * @return Person URI
     * @throws Exception If creation failed
     * @see org.opensilex.security.person.dal.PersonDAO
     */
    @POST
    @ApiOperation("Add a person")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PERSON_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PERSON_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
            @ApiResponse(code = 201, message = "A person is created"),
            @ApiResponse(code = 409, message = "The person already exists (duplicate URI)")
    })
    public Response createPerson(
            @ApiParam("Person description") @Valid PersonDTO personDTO
    ) throws Exception {
        PersonDAO personDAO = new PersonDAO(sparql);
        PersonModel person = PersonModel.fromDTO(personDTO, sparql);
        person.setPublisher(currentUser.getUri());
        personDAO.create(person, new ORCIDClient());

        return new CreatedUriResponse(person.getUri()).getResponse();
    }

    /**
     * Search persons
     *
     * @param pattern     Regex pattern for filtering list by names or email
     * @param orderByList List of fields to sort as an array of fieldName=asc|desc
     * @param page        Page number
     * @param pageSize    Page size
     * @return filtered, ordered and paginated list
     * @see PersonDAO
     */
    @GET
    @ApiOperation("Search persons")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return persons", response = PersonDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchPersons(
            @ApiParam(value = "Regex pattern for filtering list by name or email", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @ApiParam(value = "set 'true' if you want to select only persons without account", example = "false") @QueryParam("only_without_account") @DefaultValue("false") boolean onlyWithoutAccount,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        PersonDAO personDAO = new PersonDAO(sparql);

        ListWithPagination<PersonModel> resultList;
        if (onlyWithoutAccount) {
            resultList = personDAO.searchPersonsWithoutAccount(
                    pattern,
                    orderByList,
                    page,
                    pageSize
            );
        } else {
            resultList = personDAO.search(
                    pattern,
                    orderByList,
                    page,
                    pageSize
            );
        }

        ListWithPagination<PersonDTO> resultDTOList = resultList.convert(
                PersonDTO.class,
                PersonDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Update a person's data
     *
     * @param personDTO new information for updating the person
     * @return a message to know if updating worked
     * @throws Exception if update fail
     * @see PersonDAO
     */
    @PUT
    @ApiOperation("Update a person")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PERSON_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PERSON_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Person updated", response = String.class),
            @ApiResponse(code = 400, message = "Invalid parameters"),
            @ApiResponse(code = 404, message = "Person not found")
    })
    public Response updatePerson(
            @ApiParam("Person description") @Valid PersonDTO personDTO
    ) throws Exception {
        PersonDAO personDAO = new PersonDAO(sparql);

        PersonModel model = personDAO.get(personDTO.getUri());

        if (model != null) {

            AccountDAO accountDAO = new AccountDAO(sparql);
            if (Objects.nonNull(personDTO.getAccount()) && !accountDAO.accountExists(personDTO.getAccount())) {
                return new ErrorResponse(
                        Response.Status.NOT_FOUND,
                        "Account not found",
                        "Unknown account URI: " + personDTO.getAccount()
                ).getResponse();
            }

            PersonModel personModel = personDAO.update(personDTO, new ORCIDClient());

            return new ObjectUriResponse(Response.Status.OK, personModel.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Person not found",
                    "Unknown person URI: " + personDTO.getUri()
            ).getResponse();
        }
    }

    /**
     * delete a person and its data
     *
     * @param uri : URI of the Person to delete
     * @throws Exception if delete fail
     * @see PersonDAO
     */

    /** Temporarly commented to avoid mistakes until there is no protections on deletion */

//    @DELETE
//    @Path("{uri}")
//    @ApiOperation("Delete a person")
//    @ApiProtected
//    @ApiCredential(
//            credentialId = CREDENTIAL_PERSON_DELETE_ID,
//            credentialLabelKey = CREDENTIAL_PERSON_DELETE_LABEL_KEY
//    )
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "User deleted"),
//            @ApiResponse(code = 404, message = "URI not found")
//    })
//    public Response deletePerson(
//            @ApiParam(value = "Person URI", example = "http://opensilex.dev/person#harold.haddock.mistea", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
//    ) throws Exception {
//
//        if (!sparql.uriExists(PersonModel.class, uri)) {
//            return new ErrorResponse(
//                    Response.Status.NOT_FOUND,
//                    "Person doesn't exists",
//                    "URI: " + uri + " doesn't exist"
//            ).getResponse();
//        }
//
//        PersonDAO personDAO = new PersonDAO(sparql);
//        personDAO.delete(uri);
//        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
//    }

    /**
     * Return a person by URI
     *
     * @param uri URI of the person
     * @return Corresponding person
     * @throws Exception if invalid parameters or person doesn't exist
     * @see PersonDAO
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a Person")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Person retrieved", response = PersonDTO.class),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Person not found", response = ErrorDTO.class)
    })
    public Response getPerson(
            @ApiParam(value = "Person URI", example = "http://opensilex.dev/person#harold.haddock.mistea", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        PersonDAO personDAO = new PersonDAO(sparql);
        PersonModel personModel = personDAO.get(uri);

        if (personModel != null) {
            return new SingleObjectResponse<>(
                    PersonDTO.fromModel(personModel)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Person not found",
                    "Unknown person URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     * *
     * Return a list of persons corresponding to the given URIs
     *
     * @param uris list of persons uri
     * @return Corresponding list of persons
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("by_uris")
    @ApiOperation("Get persons by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return persons", response = PersonDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Persons not found (if any provided URIs is not found)", response = ErrorDTO.class)
    })
    public Response getPersonsByURI(
            @ApiParam(value = "Persons URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {

        PersonDAO dao = new PersonDAO(sparql);
        List<PersonModel> models = dao.getList(uris);

        if (!models.isEmpty()) {
            List<PersonDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> resultDTOList.add(PersonDTO.fromModel(result)));

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Persons not found",
                    "Unknown person URIs"
            ).getResponse();
        }
    }

    /**
     * *
     * Return a record corresponding to the data found on the Orcid API
     *
     * @param orcid you want data from, with or without https://orcid.org/ prefix
     * @return Corresponding data from the orcid API
     */
    @GET
    @Path("orcid_record")
    @ApiOperation("Get infos from an ORCID")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return orcid record", response = OrcidRecordDTO.class),
            @ApiResponse(code = 404, message = "orcid is not found by ORCID API ", response = ErrorDTO.class)
    })
    public Response getOrcidRecord(
            @ApiParam(value = "orcid", required = true) @QueryParam("orcid") @NotNull URI orcid
    ) {
        PersonDAO personDAO = new PersonDAO(sparql);

        String orcidId = personDAO.getIdPartOfAnOrcidUri(orcid);

        personDAO.requireOrcidIDIsWellFormed(orcidId);

        ORCIDClient orcidClient = new ORCIDClient();
        orcidClient.assertOrcidConnexionIsOk();

        return new SingleObjectResponse<>(
                orcidClient.getRecord(orcidId)
        ).getResponse();
    }

    /**
     * This method gets the file to the path found in the security config and send it in a response.
     * If the file is not available in the requested language, or if language was not specified, it will send the version in the language of the current user.
     * If the user is not connected or if the file is not available in its language, it will send the file in the default language of the OpenSilex instance.
     * If this file doesn't exist, it sends the file in the first language of the config.
     * Return ErrorResponses if there is no config set or if the file of the config doesn't exist.
     */
    @GET
    @Path("GDPR")
    @ApiOperation("Get RGPD PDF")
    @Produces("application/pdf")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieve file"),
            @ApiResponse(code = 404, message = "File does not exists at the location precised in the configuration file", response = ErrorDTO.class),
            @ApiResponse(code = 503, message = "Location of file was not provided in the OpenSilex configuration", response = ErrorDTO.class)
    })
    public Response getGdprFile(
            @ApiParam(value = "preferred language of the file", example = "fr") @QueryParam("language") String askedLanguage
    ) throws OpenSilexModuleNotFoundException {

        SecurityConfig config = sparql.getOpenSilex().getModuleConfig(SecurityModule.class, SecurityConfig.class);
        Map<String, String> filePaths = config.gdprPdfPathsByLanguages();

        if (filePaths.isEmpty()) {
            throw new ServiceUnavailableException("Location of file was not provided in the OpenSilex configuration");
        }

        String filePath = filePaths.values().iterator().next();

        if (filePaths.containsKey(OpenSilex.DEFAULT_LANGUAGE)) {
            filePath = filePaths.get(OpenSilex.DEFAULT_LANGUAGE);
        }

        if (Objects.nonNull(currentUser)){
            filePath = filePaths.get(currentUser.getLanguage());
        }

        if (Objects.nonNull(askedLanguage) && filePaths.containsKey(askedLanguage)){
            filePath = filePaths.get(askedLanguage);
        }

        File file = new File(filePath);
        if ( ! file.exists() ){
            throw new NotFoundException("File does not exists at the location precised in the configuration file");
        }

        Response.ResponseBuilder response = Response.ok(file);
        response.header("Content-Disposition", "attachment; filename=\"GDPR.pdf\"");
        return response.build();
    }

}
