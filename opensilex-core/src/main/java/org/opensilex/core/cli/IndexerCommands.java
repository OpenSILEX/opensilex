
//******************************************************************************
//                        IndexerCommands.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.cli;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.HelpOption;
import org.opensilex.cli.AbstractOpenSilexCommand;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;


/**
 *
 * @author Cheimae
 */
@Command(
        name = "indexer",
        header = "Subcommand to group OpenSILEX indexing operations"
)
public class IndexerCommands extends AbstractOpenSilexCommand implements OpenSilexCommand {

    private SPARQLService sparql;

    private RestHighLevelClient elasticClient;

    @CommandLine.Command(
            name = "index-db",
            header = "Index data",
            description = "Index all data stored in OpenSilex databases"
    )
    public void index(
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparql = factory.provide();
        try {
            elasticClient = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost("localhost", 9200, "http"),
                            new HttpHost("localhost", 9201, "http")
                    )
            );
            //indexProject();
            indexVariable();
        } finally {
            elasticClient.close();
            factory.dispose(sparql);
        }
        factory.dispose(sparql);
    }

    private void indexProject() throws Exception {
        
        List<ProjectModel> projects = sparql.search(ProjectModel.class, "en");
        
        try {
            
            DeleteIndexRequest request = new DeleteIndexRequest("projects");
            AcknowledgedResponse deleteIndexResponse = elasticClient.indices().delete(request, RequestOptions.DEFAULT);
        
        }catch (ElasticsearchException exception) {
          if (exception.status() == RestStatus.NOT_FOUND) {

           
        
            ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
            if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("description")) {
                return true;
            }
            if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("objective")) {
                return true;
            }
            if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("homePage")) {
                return true;
            }
            if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("administrativeContacts")) {
                return true;
            }
            if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("coordinators")) {
                return true;
            }
            if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("scientificContacts")) {
                return true;
            }
            
             if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("creator")) {
                return true;
            }
            if (field.getDeclaringClass() == ProjectModel.class && field.getName().equals("relatedProjects")) {
                return true;
            } 
            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
        };
      
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(strategy).create();
        String json;
        
           for(ProjectModel p : projects) {
           json = gson.toJson(p);
              
           IndexRequest indexRequest = new IndexRequest("projects");
           indexRequest.source(json, XContentType.JSON); 
           IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        
            }
           // System.out.println("----------------------------------------------------------------------------");
           //System.out.println(json);
           

    } 
           
      }
        }
    private void indexVariable() throws Exception {
        
        List<VariableModel> Variables = sparql.search(VariableModel.class, "en");
        
        /*try {
            
            DeleteIndexRequest request = new DeleteIndexRequest("Variables");
            AcknowledgedResponse deleteIndexResponse = elasticClient.indices().delete(request, RequestOptions.DEFAULT);
        
        }catch (ElasticsearchException exception) {
          if (exception.status() == RestStatus.NOT_FOUND) {*/

           

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json;
        
           for(VariableModel p : Variables) {
           json = gson.toJson(p);
              
           IndexRequest indexRequest = new IndexRequest("variables");
           indexRequest.source(json, XContentType.JSON); 
           IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        
            }
           // System.out.println("----------------------------------------------------------------------------");
           //System.out.println(json);
           

    } 
           
      }
        //}
    
    
//}