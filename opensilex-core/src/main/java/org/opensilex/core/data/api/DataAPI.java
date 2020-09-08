/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author sammy
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/data")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataAPI {
    public static final String CREDENTIAL_DATA_GROUP_ID = "Data";
    public static final String CREDENTIAL_DATA_GROUP_LABEL_KEY = "credential-groups.data";

    public static final String DATA_EXAMPLE_URI = "http://opensilex.dev/id/data/1598857852858";
    public static final String DATA_EXAMPLE_OBJECTURI = "http://opensilex.dev/opensilex/2020/o20000345";
    public static final String DATA_EXAMPLE_VARIABLEURI = "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6";
    public static final String DATA_EXAMPLE_PROVENANCEURI = "http://opensilex.dev/provenance/1598001689415";
    public static final String DATA_EXAMPLE_VALUE = "8.6";
    public static final String DATA_EXAMPLE_MINIMAL_DATE  = "2020-08-21T00:00:00";
    public static final String DATA_EXAMPLE_MAXIMAL_DATE = "2020-09-21T00:00:00";
    
    public static final String CREDENTIAL_DATA_MODIFICATION_ID = "data-modification";
    public static final String CREDENTIAL_DATA_MODIFICATION_LABEL_KEY = "credential.data.modification";

    public static final String CREDENTIAL_DATA_READ_ID = "data-read";
    public static final String CREDENTIAL_DATA_READ_LABEL_KEY = "credential.data.read";

    public static final String CREDENTIAL_DATA_DELETE_ID = "data-delete";
    public static final String CREDENTIAL_DATA_DELETE_LABEL_KEY = "credential.data.delete";

    @Inject
    private DataNucleusService nosql;
    
    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel user;

    @POST
    @Path("create")
    @ApiProtected
    @ApiOperation("Add data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add data", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response addData(
            @ApiParam("Data description") @Valid DataCreationDTO dto
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql);
        DataModel model = dto.newModel();
        if(! dao.valid(model))
            throw new Exception("Unknown Variable or Object or Provenance URI");
        dao.prepareURI(model);
        model = (DataModel) dao.create(model);
        return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
    }
    
    /*@POST
    @Path("listcreate")
    @ApiProtected
    @ApiOperation("Add list of data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add data", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response addListData(
            @ApiParam("Data description") @Valid List<DataCreationDTO> dtoList
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql);
        for(DataCreationDTO dto : dtoList ){
            DataModel model = dto.newModel();
            if(! dao.valid(model))
                throw new Exception("Unknown Variable or Object or Provenance URI");
            dao.prepareURI(model);
            model = (DataModel) dao.create(model);
        }
        return new ObjectUriResponse().getResponse();
    }*/
    
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_READ_ID, credentialLabelKey = CREDENTIAL_DATA_READ_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Data retrieved", response = DataGetDTO.class),
        @ApiResponse(code = 404, message = "Data not found", response = ErrorResponse.class)})
    public Response getData(
            @ApiParam(value = "Data URI", /*example = "platform-data:irrigation",*/ required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        DataDAO dao = new DataDAO(nosql,sparql);
        DataModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(DataGetDTO.fromModel(model)).getResponse();
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Factor not found",
                    "Unknown data URI: " + uri.toString()).getResponse();
        }
    }
    
    @GET
    @Path("search")
    @ApiOperation("Search data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_READ_ID, credentialLabelKey = CREDENTIAL_DATA_READ_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return data list", response = DataGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchDataList(
            //@ApiParam(value = "Search by uri", example = DATA_EXAMPLE_URI) @QueryParam("uri") URI uri,
            @ApiParam(value = "Search by object uri", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("objectUri") URI objectUri,
            @ApiParam(value = "Search by variable uri", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variableUri") URI variableUri,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenanceUri") URI provenanceUri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("startDate") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("endDate") String endDate,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql);
        ListWithPagination<DataModel> resultList = dao.search(
                user,
                //uri,
                objectUri,
                variableUri,
                provenanceUri,
                startDate,
                endDate,
                page,
                pageSize
        );
        
        //List<DataGetDTO> resultDTOList = new ArrayList<>();
        
        ListWithPagination<DataGetDTO> resultDTOList = resultList.convert(DataGetDTO.class, DataGetDTO::fromModel);
        
        /*for(DataModel dataModel: resultList){
            resultDTOList.add(DataGetDTO.fromModel(dataModel));
        }
        
        ListWithPagination<DataGetDTO> paginatedListResponse = new ListWithPagination<DataGetDTO>(resultDTOList,
              page, pageSize, resultList.size());*/

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_DELETE_ID, credentialLabelKey = CREDENTIAL_DATA_DELETE_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Data deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Data URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteData(
            @ApiParam(value = "Data URI", example = DATA_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) {

        try {
            DataDAO dao = new DataDAO(nosql,sparql);
            dao.delete(uri);
            return new ObjectUriResponse(uri).getResponse();

        } catch (NoSQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Data URI", e.getMessage())
                    .getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
    
    @PUT
    @Path("{uri}/confidence")
    @ApiProtected
    @ApiOperation("Update confidence")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response confidence(
            @ApiParam("Data description") @Valid DataConfidenceDTO dto,
            @ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        try{
            DataDAO dao = new DataDAO(nosql, sparql);
            DataModel model = dto.newModel();
            
            if(!nosql.existByURI(model, uri)) throw new NoSQLInvalidURIException(uri);
            model.setUri(uri);
            model = (DataModel) dao.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        }catch (NoSQLInvalidURIException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Data URI", e.getMessage()).getResponse();
        }catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
        
    }
    
    @PUT
    @Path("update")
    @ApiProtected
    @ApiOperation("Update data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response update(
            @ApiParam("Data description") @Valid DataUpdateDTO dto
            //@ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        try{
            DataDAO dao = new DataDAO(nosql, sparql);
            DataModel model = dto.newModel();
            if(!dao.valid(model)){
                throw new Exception("Unknown Variable, Object or Provenance URI");
            };
            
            if(!nosql.existByURI(model, dto.getUri())) throw new NoSQLInvalidURIException(dto.getUri());

            model = (DataModel) dao.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        }catch (NoSQLInvalidURIException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Data URI", e.getMessage()).getResponse();
        }catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
        
    }
}
