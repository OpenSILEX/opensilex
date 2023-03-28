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
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
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
    public static final String CREDENTIAL_GROUP_PERSON_ID = "Person";
    public static final String CREDENTIAL_GROUP_PERSON_LABEL_KEY = "credential-groups.person";

    public static final String CREDENTIAL_PERSON_MODIFICATION_ID = "person-modification";
    public static final String CREDENTIAL_PERSON_MODIFICATION_LABEL_KEY = "credential.default.modification";
    private static final String CREDENTIAL_PERSON_DELETE_ID = "person-delete";
    private static final String CREDENTIAL_PERSON_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    /**
     * Create a person and return its URI
     *
     * @see org.opensilex.security.person.dal.PersonDAO
     * @param personDTO person model to create
     * @return Person URI
     * @throws Exception If creation failed
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

        if (sparql.uriExists(PersonModel.class, personDTO.getUri())) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Person already exists",
                    "Duplicated URI: " + personDTO.getUri()
            ).getResponse();
        }

        AccountDAO accountDAO = new AccountDAO(sparql);
        URI accountURI = personDTO.getAccount();
        AccountModel account = accountURI == null ? null : accountDAO.get(accountURI);

        PersonModel person = personDAO.create(
                personDTO.getUri(),
                personDTO.getFirstName(),
                personDTO.getLastName(),
                personDTO.getEmail(),
                account
        );

        return new ObjectUriResponse(Response.Status.CREATED, person.getUri()).getResponse();
    }

    /**
     * Search persons
     *
     * @see PersonDAO
     * @param pattern Regex pattern for filtering list by names or email
     * @param orderByList List of fields to sort as an array of fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
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
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        PersonDAO personDAO = new PersonDAO(sparql);
        ListWithPagination<PersonModel> resultList = personDAO.search(
                pattern,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<PersonDTO> resultDTOList = resultList.convert(
                PersonDTO.class,
                PersonDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Update a person's data
     *
     * @see PersonDAO
     * @param personDTO new information for updating the person
     * @return a message to know if updating worked
     * @throws Exception if update fail
     */
    @PUT
    @ApiOperation("Update a person")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PERSON_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PERSON_MODIFICATION_ID
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
            if (Objects.nonNull(personDTO.getAccount()) && ! accountDAO.accountExists(personDTO.getAccount())){
                return new ErrorResponse(
                        Response.Status.NOT_FOUND,
                        "Account not found",
                        "Unknown account URI: " + personDTO.getAccount()
                ).getResponse();
            }
            AccountModel account = Objects.isNull(personDTO.getAccount()) ? null : accountDAO.get(personDTO.getAccount());

            PersonModel personModel = personDAO.update(
                    personDTO.getUri(),
                    personDTO.getFirstName(),
                    personDTO.getLastName(),
                    personDTO.getEmail(),
                    account);

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
     * @see PersonDAO
     * @param uri : URI of the Person to delete
     * @throws Exception if delete fail
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a person")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PERSON_DELETE_ID,
            credentialLabelKey = CREDENTIAL_PERSON_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User deleted"),
            @ApiResponse(code = 404, message = "URI not found")
    })
    public Response deletePerson(
            @ApiParam(value = "Person URI", example = "http://opensilex.dev/person#harold.haddock.mistea", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {

        if (! sparql.uriExists(PersonModel.class, uri)) {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Person doesn't exists",
                    "URI: " + uri + " doesn't exist"
            ).getResponse();
        }

        PersonDAO personDAO = new PersonDAO(sparql);
        personDAO.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * Return a person by URI
     *
     * @see PersonDAO
     * @param uri URI of the person
     * @return Corresponding person
     * @throws Exception if invalid parameters or person doesn't exist
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

}
