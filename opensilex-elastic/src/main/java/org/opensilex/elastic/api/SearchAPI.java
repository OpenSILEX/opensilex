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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.opensilex.core.variable.api.VariableGetDTO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.elastic.service.ElasticService;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.utils.ListWithPagination;


/**
 *
 * @author assarar
 */
@Api(SearchAPI.CREDENTIAL_SEARCH_GROUP_ID)
@Path("/elastic")
@ApiCredentialGroup(
        groupId = SearchAPI.CREDENTIAL_SEARCH_GROUP_ID,
        groupLabelKey = SearchAPI.CREDENTIAL_SEARCH_GROUP_LABEL_KEY
)

public class SearchAPI {

    public static final String CREDENTIAL_SEARCH_GROUP_ID = "Elastic search";
    public static final String CREDENTIAL_SEARCH_GROUP_LABEL_KEY = "credential-groups.search";

    @CurrentUser
    UserModel currentUser;

    @Inject
    ElasticService elastic;

    @GET
    @Path("search_variable")
    @ApiOperation(
            value = "Search variables by name, long name, entity, characteristic, method or unit name",
            notes = "The following fields could be used for sorting : \n\n"
            + " _entity_name/entityName : the name of the variable entity\n\n"
            + " _characteristic_name/characteristicName : the name of the variable characteristic\n\n"
            + " _method_name/methodName : the name of the variable method\n\n"
            + " _unit_name/unitName : the name of the variable unit\n\n"
    )
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables", response = VariableGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariablesES(
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern,
            @ApiParam(value = "number of documents to skip", example = "0") @QueryParam("from") @DefaultValue("0") @Min(0) int from,
            @ApiParam(value = "maximum number of documents to return", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int size
    ) throws Exception {
        RestHighLevelClient elasticClient = elastic.getClient();

        
        
      QueryStringQueryBuilder query  = QueryBuilders.queryStringQuery(namePattern);

        SearchResponse response = elasticClient.search(new SearchRequest("variables")
        .source(new SearchSourceBuilder()
                .query(query)
                .from(from)
                .size(size)
                .trackTotalHits(true)
                )
                , RequestOptions.DEFAULT
        );

        SearchHit[] searchHits = response.getHits().getHits();
        elasticClient.close();

        
        List<VariableModel> results = Arrays.stream(searchHits)
                .map(hit -> JSON.parseObject(hit.getSourceAsString(), VariableModel.class))
                .collect(Collectors.toList());

        
        ListWithPagination<VariableGetDTO> resultDTOList = new ListWithPagination(
                results.stream()
                        .map(model -> VariableGetDTO.fromModel(model))
                        .collect(Collectors.toList()));

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

}
