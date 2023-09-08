//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.api;

import io.swagger.annotations.*;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        personDAO.create(person);

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

            PersonModel personModel = personDAO.update(personDTO);

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
            @ApiResponse(code = 404, message = "Persons not found (if any provided URIs is not found", response = ErrorDTO.class)
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

}
