
//******************************************************************************
//                        
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.elastic.api;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opensilex.elastic.cli.ElasticCommands;
import org.opensilex.elastic.service.ElasticService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;


/**
 *
 * @author assarar
 */
@Api(GlobalSearchAPI.CREDENTIAL_SEARCH_GROUP_ID)
@Path("/elastic")
@ApiCredentialGroup(
        groupId = SearchAPI.CREDENTIAL_SEARCH_GROUP_ID,
        groupLabelKey = SearchAPI.CREDENTIAL_SEARCH_GROUP_LABEL_KEY
)

public class GlobalSearchAPI  {

    public static final String CREDENTIAL_SEARCH_GROUP_ID = "Elastic search";
    public static final String CREDENTIAL_SEARCH_GROUP_LABEL_KEY = "credential-groups.search";

    @CurrentUser
    UserModel currentUser;

    @Inject
    ElasticService elastic;
    
    @Inject
    MongoDBService mongodb;
     
    @Inject
    SPARQLService sparql;
      
    
    @GET
    @Path("search")
    @ApiOperation(
            value = "Search by name, long name, entity, characteristic, method or unit name"
    )
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return all objects that matches the query ", response = GlobalSearchDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchES(
            @ApiParam(value = "StringQuery", example = "sunagri") @QueryParam("name") String namePattern,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        RestHighLevelClient elasticClient = elastic.getClient();
        int from = page*pageSize;
        
        //Creates the SearchRequest. Without arguments this runs against all indices.
        SearchRequest searchRequest = new SearchRequest();
        
        QueryStringQueryBuilder query  = QueryBuilders.queryStringQuery(namePattern);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder(); 


        SearchResponse response = elasticClient.search(searchRequest
        .source(sourceBuilder
                .query(query)
                .from(from)
                .size(pageSize)
                .trackTotalHits(true)
                )
                , RequestOptions.DEFAULT
        );
        
         
        SearchHit[] searchHits = response.getHits().getHits();
        CountRequest countRequest = new CountRequest(); 
        countRequest.source(sourceBuilder);
        

        CountResponse countResponse = elasticClient
                       .count(countRequest, RequestOptions.DEFAULT);
        
        int count = (int) countResponse.getCount();

     
        elasticClient.close();

        
        List<GlobalSearchDTO> results = Arrays.stream(searchHits)
                .map(hit -> JSON.parseObject(hit.getSourceAsString(), GlobalSearchDTO.class))
                .collect(Collectors.toList());


        
        ListWithPagination<GlobalSearchDTO> resultDTOList = new ListWithPagination(
                new ArrayList<>(results),page,pageSize,count);

       return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    
    @POST
    @Path("reload-indexes")
    @ApiOperation(value = "Reload Elasticsearch indexes (available indexes : variables, devices, events)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Indexes Reloaded", response =  SingleObjectResponse.class),
        @ApiResponse(code = 500, message = "Indexes not reloaded", response = ErrorDTO.class)

    })
    @ApiProtected

    @Produces(MediaType.APPLICATION_JSON)
    public Response reloadIndexes(           
       @ApiParam(value = "Index name", example = "variables") @QueryParam("indexName") String indexName
       ) throws Exception {
        System.out.println("org.opensilex.elastic.api.GlobalSearchAPI.reloadIndexes()");
        ElasticCommands elasticCommands = new ElasticCommands();
        boolean index = elasticCommands.index(indexName,elastic, mongodb, sparql);
     
        return new SingleObjectResponse<>(index).getResponse();


    }

    @DELETE
    @Path("delete-indexes")
    @ApiOperation(value = "Delete Elasticsearch indexes (available indexes : variables, devices, events)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Indexes deleted", response =  SingleObjectResponse.class),
        @ApiResponse(code = 500, message = "Indexes not deleted", response = ErrorDTO.class)

    })
    @ApiProtected
 
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteIndexes(           
       @ApiParam(value = "Index name", example = "variables",required = true) @QueryParam("indexName") @Required String indexName
       ) throws Exception {
        System.out.println("org.opensilex.elastic.api.GlobalSearchAPI.deleteIndexes()");
        ElasticCommands elasticCommands = new ElasticCommands();
        RestHighLevelClient elasticClient = elastic.getClient();

        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        AcknowledgedResponse deleteIndexResponse = elasticClient.indices().delete(request, RequestOptions.DEFAULT);

        return new SingleObjectResponse<>(deleteIndexResponse).getResponse();


    }

}
