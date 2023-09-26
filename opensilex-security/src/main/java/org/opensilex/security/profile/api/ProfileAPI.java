//******************************************************************************
//                          UserAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.profile.api;

import io.swagger.annotations.*;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
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

import static org.apache.jena.vocabulary.RDF.uri;

/**
 * <pre>
 * Profile API for OpenSilex which provides:
 *
 * - create: Create a profile
 * - get: Get a profile by URI
 * - search: Search a filtered, ordered and paginated list of profiles
 * - update: Update a profile
 * - delete: Delete a profile
 * </pre>
 *
 * @author Vincent Migot
 */
@Api(SecurityModule.REST_SECURITY_API_ID)
@Path("/security/profiles")
@ApiCredentialGroup(
        groupId = ProfileAPI.CREDENTIAL_PROFILE_GROUP_ID,
        groupLabelKey = ProfileAPI.CREDENTIAL_PROFILE_GROUP_LABEL_KEY
)
public class ProfileAPI {

    public static final String CREDENTIAL_PROFILE_GROUP_ID = "Profiles";
    public static final String CREDENTIAL_PROFILE_GROUP_LABEL_KEY = "credential-groups.profiles";

    public static final String CREDENTIAL_PROFILE_MODIFICATION_ID = "profile-modification";
    public static final String CREDENTIAL_PROFILE_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_PROFILE_DELETE_ID = "profile-delete";
    public static final String CREDENTIAL_PROFILE_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel currentUser;

    @POST
    @ApiOperation("Add a profile")
    @ApiResponses({
        @ApiResponse(code = 201, message = "A profile is created"),
        @ApiResponse(code = 403, message = "This current user can't create profiles"),
        @ApiResponse(code = 409, message = "The profile name already exists")
    })
    @ApiProtected()
    @ApiCredential(
            credentialId = CREDENTIAL_PROFILE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROFILE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(
            @ApiParam("Profile description") @Valid ProfileCreationDTO profileDTO
    ) throws Exception {
        // Create profile DAO
        ProfileDAO profileDAO = new ProfileDAO(sparql);

        // check if profile URI already exists
        if (sparql.uriExists(ProfileModel.class, profileDTO.getUri())) {
            // Return error response 409 - CONFLICT if user URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Profile already exists",
                    "Duplicated URI: " + profileDTO.getUri()
            ).getResponse();
        }

        // check if profile name already exists
        if (profileDAO.profileNameExists(profileDTO.getName())) {
            // Return error response 409 - CONFLICT if profile name already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Profile already exists",
                    "Duplicated name: " + profileDTO.getName()
            ).getResponse();
        }

        // create new profile
        ProfileModel profile = profileDAO.create(
                profileDTO.getUri(),
                profileDTO.getName(),
                profileDTO.getCredentials(),
                currentUser.getUri()
        );
        // return user URI
        return new CreatedUriResponse(profile.getUri()).getResponse();
    }

    @PUT
    @ApiOperation("Update a profile")
    @ApiProtected()
    @ApiCredential(
            credentialId = CREDENTIAL_PROFILE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROFILE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile uri of the updated profile", response = String.class),
        @ApiResponse(code = 400, message = "Invalid parameters")
    })
    public Response updateProfile(
            @ApiParam("Profile description") @Valid ProfileUpdateDTO dto
    ) throws Exception {
        ProfileDAO dao = new ProfileDAO(sparql);

        // Get user model from DTO uri
        ProfileModel model = dao.get(dto.getUri());

        if (model != null) {
            // If model exists, update it
            ProfileModel profile = dao.update(
                    dto.getUri(),
                    dto.getName(),
                    dto.getCredentials()
            );

            return new ObjectUriResponse(Response.Status.OK, profile.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Project not found",
                    "Unknown project URI: " + uri
            ).getResponse();
        }
    }

    /**
     * Get a profile by it's URI
     *
     * @param uri uri of the profile
     * @return corresponding profile
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a profile")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Profile retrieved", response = ProfileGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Profile not found", response = ErrorDTO.class)
    })
    public Response getProfile(
            @ApiParam(value = "Profile URI", example = "dev-users:Admin_OpenSilex", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        // Get user from DAO by URI
        ProfileDAO dao = new ProfileDAO(sparql);
        ProfileModel model = dao.get(uri);

        // Check if user is found
        if (model != null) {
            // Return user converted in UserGetDTO
            return new SingleObjectResponse<>(
                    ProfileGetDTO.fromModel(model)
            ).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Profile not found",
                    "Unknown profile URI: " + uri.toString()
            ).getResponse();
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a profile")
    @ApiProtected()
    @ApiCredential(
            credentialId = CREDENTIAL_PROFILE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_PROFILE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProfile(
            @ApiParam(value = "Profile URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        ProfileDAO dao = new ProfileDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @ApiOperation("Search profiles")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profiles", response = ProfileGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchProfiles(
            @ApiParam(value = "Regex pattern for filtering list by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String pattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        // Search profiles with Profile DAO
        ProfileDAO dao = new ProfileDAO(sparql);
        ListWithPagination<ProfileModel> resultList = dao.search(
                pattern,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<ProfileGetDTO> resultDTOList = resultList.convert(
                ProfileGetDTO.class,
                (result) -> {
                    return ProfileGetDTO.fromModel(result);
                }
        );

        // Return paginated list of profiles DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("all")
    @ApiOperation("Get all profiles")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return all profiles", response = ProfileGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getAllProfiles(
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("order_by") List<OrderBy> orderByList
    ) throws Exception {
        // Search profiles with Profile DAO
        ProfileDAO dao = new ProfileDAO(sparql);
        List<ProfileModel> resultList = dao.getAll(orderByList);
        // Convert list to DTO
        List<ProfileGetDTO> resultDTOList = new ArrayList<>(resultList.size());

        resultList.forEach(result -> {
            resultDTOList.add(ProfileGetDTO.fromModel(result));
        });

        // Return list of profiles DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
