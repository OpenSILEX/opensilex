//******************************************************************************
//                          UserAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.profile.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import static org.apache.jena.vocabulary.RDF.uri;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.profile.dal.ProfileDAO;
import org.opensilex.rest.profile.dal.ProfileModel;
import static org.opensilex.rest.user.api.UserAPI.CREDENTIAL_GROUP_USER_ID;
import static org.opensilex.rest.user.api.UserAPI.CREDENTIAL_GROUP_USER_LABEL_KEY;
import static org.opensilex.rest.user.api.UserAPI.CREDENTIAL_USER_READ_ID;
import static org.opensilex.rest.user.api.UserAPI.CREDENTIAL_USER_READ_LABEL_KEY;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

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
@Api(ProfileAPI.CREDENTIAL_GROUP_PROFILE_ID)
@Path("/profile")
public class ProfileAPI {

    public static final String CREDENTIAL_GROUP_PROFILE_ID = "Profiles";
    public static final String CREDENTIAL_GROUP_PROFILE_LABEL_KEY = "credential-groups.profiles";

    public static final String CREDENTIAL_PROFILE_MODIFICATION_ID = "profile-modification";
    public static final String CREDENTIAL_PROFILE_MODIFICATION_LABEL_KEY = "credential.profile.modification";

    public static final String CREDENTIAL_PROFILE_DELETE_ID = "profile-delete";
    public static final String CREDENTIAL_PROFILE_DELETE_LABEL_KEY = "credential.profile.delete";

    public static final String CREDENTIAL_PROFILE_READ_ID = "profile-read";
    public static final String CREDENTIAL_PROFILE_READ_LABEL_KEY = "credential.profile.read";

    private final SPARQLService sparql;

    /**
     * Inject SPARQL service
     */
    @Inject
    public ProfileAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    @POST
    @Path("create")
    @ApiOperation("Create a profile and return it's URI")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Profile sucessfully created"),
        @ApiResponse(code = 403, message = "Current user can't create profiles"),
        @ApiResponse(code = 409, message = "Profile name already exists")
    })
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_PROFILE_ID,
            groupLabelKey = CREDENTIAL_GROUP_PROFILE_LABEL_KEY,
            credentialId = CREDENTIAL_PROFILE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROFILE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfile(
            @ApiParam("Profile creation informations") @Valid ProfileCreationDTO profileDTO
    ) throws Exception {
        // Create profile DAO
        ProfileDAO profileDAO = new ProfileDAO(sparql);

        // check if profile name already exists
        ProfileModel profile = profileDAO.getProfileByName(profileDTO.getName());
        if (profile == null) {
            // create new user
            profile = profileDAO.create(
                    profileDTO.getUri(),
                    profileDTO.getName(),
                    profileDTO.getCredentials()
            );
            // return user URI
            return new ObjectUriResponse(Response.Status.CREATED, profile.getUri()).getResponse();
        } else {
            // Return error response 409 - CONFLICT if user already exists
            return new ErrorResponse(
                    Status.CONFLICT,
                    "Profile already exists",
                    "Duplicated name: " + profileDTO.getName()
            ).getResponse();
        }
    }

    @PUT
    @Path("update")
    @ApiOperation("Update a profile")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_PROFILE_ID,
            groupLabelKey = CREDENTIAL_GROUP_PROFILE_LABEL_KEY,
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
    @Path("get/{uri}")
    @ApiOperation("Get a profile by it's URI")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_USER_ID,
            groupLabelKey = CREDENTIAL_GROUP_USER_LABEL_KEY,
            credentialId = CREDENTIAL_USER_READ_ID,
            credentialLabelKey = CREDENTIAL_USER_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile", response = ProfileGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "User not found", response = ErrorDTO.class)
    })
    public Response getProfile(
            @ApiParam(value = "User URI", example = "dev-users:Admin_OpenSilex", required = true) @PathParam("uri") @NotNull URI uri
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
    @Path("delete/{uri}")
    @ApiOperation("Delete a profile")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_PROFILE_ID,
            groupLabelKey = CREDENTIAL_GROUP_PROFILE_LABEL_KEY,
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
    @Path("search")
    @ApiOperation("Search profiles")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_PROFILE_ID,
            groupLabelKey = CREDENTIAL_GROUP_PROFILE_LABEL_KEY,
            credentialId = CREDENTIAL_PROFILE_READ_ID,
            credentialLabelKey = CREDENTIAL_PROFILE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile list", response = ProfileGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchProfiles(
            @ApiParam(value = "Regex pattern for filtering list by name", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
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
                ProfileGetDTO::fromModel
        );

        // Return paginated list of profiles DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("get-all")
    @ApiOperation("Get all profiles")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_PROFILE_ID,
            groupLabelKey = CREDENTIAL_GROUP_PROFILE_LABEL_KEY,
            credentialId = CREDENTIAL_PROFILE_READ_ID,
            credentialLabelKey = CREDENTIAL_PROFILE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile list", response = ProfileGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getAllProfiles(
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "email=asc") @QueryParam("orderBy") List<OrderBy> orderByList
    ) throws Exception {
        // Search profiles with Profile DAO
        ProfileDAO dao = new ProfileDAO(sparql);
        List<ProfileModel> resultList = dao.getAll(orderByList);
        // Convert list to DTO
        List<ProfileGetDTO> resultDTOList = new ArrayList<>();
        resultList.forEach(result -> {
            resultDTOList.add(ProfileGetDTO.fromModel(result));
        });

        // Return list of profiles DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
