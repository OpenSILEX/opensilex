//******************************************************************************
//                        IndexerCommands.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.elastic.cli;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collections;
import java.util.List;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.HelpOption;
import org.opensilex.cli.AbstractOpenSilexCommand;

import org.opensilex.core.device.api.DeviceGetDetailsDTO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.event.api.EventDetailsDTO;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.project.api.ProjectGetDTO;

import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.api.VariableDetailsDTO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.elastic.service.ElasticService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ListWithPagination;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 *
 * @author Cheimae
 */
@Command(
        name = "elastic",
        header = "Subcommand to group OpenSILEX elastic search indexing operations"
)
public class ElasticCommands extends AbstractOpenSilexCommand implements OpenSilexCommand {

    private SPARQLService sparql;
    private MongoDBService mongodb;

    private RestHighLevelClient elasticClient;

    public static final String VARIABLE_INDEX_NAME = "variables";
    public static final String DEVICE_INDEX_NAME = "devices";
    public static final String EVENT_INDEX_NAME = "events";

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

        MongoDBService mongodb = getOpenSilex().getServiceInstance("mongodb", MongoDBService.class);

        try {
            ElasticService elasticService = getOpenSilex().getServiceInstance(ElasticService.DEFAULT_ELASTIC_SERVICE, ElasticService.class);
            elasticClient = elasticService.getClient();
            indexVariable();
            indexDevice();
            indexEvent();

        } finally {
            if (elasticClient != null) {
                elasticClient.close();
            }
            factory.dispose(sparql);
        }
        factory.dispose(sparql);
    }

    public boolean index(String indexName, ElasticService elasticService, MongoDBService mongodb, SPARQLService sparql) throws Exception {
        elasticClient = elasticService.getClient();
        this.sparql = sparql;
        this.mongodb = mongodb;
        try {

            if (indexName == null) {
                indexVariable();
                indexDevice();
                indexEvent();
            } else {
                switch (indexName) {
                    case VARIABLE_INDEX_NAME:
                        indexVariable();
                        break;
                    case DEVICE_INDEX_NAME:
                        indexDevice();
                        break;
                    case EVENT_INDEX_NAME:
                        indexEvent();
                        break;
                }
            }

        } catch (Exception e) {
            return false;
        } finally {
            if (elasticClient != null) {
                elasticClient.close();
            }
        }
        return true;
    }

    private void indexProject() throws Exception {

        List<ProjectModel> projects = sparql.search(ProjectModel.class, "en");

        try {

            DeleteIndexRequest request = new DeleteIndexRequest("projects");
            AcknowledgedResponse deleteIndexResponse = elasticClient.indices().delete(request, RequestOptions.DEFAULT);

        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
            }
        }

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

        for (ProjectModel p : projects) {
            ProjectGetDTO var = ProjectGetDTO.fromModel(p);
            json = gson.toJson(var);

            IndexRequest indexRequest = new IndexRequest("projects");
            indexRequest.source(json, XContentType.JSON);
            IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        }

    }

    private void indexVariable() throws Exception {

        List<VariableModel> Variables = sparql.search(VariableModel.class, "en");

        try {
            DeleteIndexRequest request = new DeleteIndexRequest(VARIABLE_INDEX_NAME);
            AcknowledgedResponse deleteIndexResponse = elasticClient.indices().delete(request, RequestOptions.DEFAULT);

        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json;

        for (VariableModel p : Variables) {

            VariableDetailsDTO var = VariableDetailsDTO.fromModel(p);

            json = gson.toJson(var);

            IndexRequest indexRequest = new IndexRequest(VARIABLE_INDEX_NAME);

            indexRequest.source(json, XContentType.JSON);
            IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        }

    }

    private void indexDevice() throws Exception {

        List<DeviceModel> Devices = sparql.search(DeviceModel.class, "en");

        try {
            DeleteIndexRequest request = new DeleteIndexRequest(DEVICE_INDEX_NAME);
            AcknowledgedResponse deleteIndexResponse = elasticClient.indices().delete(request, RequestOptions.DEFAULT);

        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json;

        for (DeviceModel d : Devices) {
            DeviceGetDetailsDTO var = DeviceGetDetailsDTO.getDTOFromModel(d);
            json = gson.toJson(var);

            IndexRequest indexRequest = new IndexRequest(DEVICE_INDEX_NAME);

            indexRequest.source(json, XContentType.JSON);
            IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);

        }

    }

    private void indexEvent() throws Exception {

        EventDAO eventdao = new EventDAO(sparql, mongodb);

        ListWithPagination<EventModel> events = eventdao.search(null, null, null, null, null, null, Collections.emptyList(), null, null);
        List<EventModel> Events = events.getList();

        try {
            DeleteIndexRequest request = new DeleteIndexRequest(EVENT_INDEX_NAME);
            AcknowledgedResponse deleteIndexResponse = elasticClient.indices().delete(request, RequestOptions.DEFAULT);

        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json;

        for (EventModel e : Events) {
            EventDetailsDTO var = new EventDetailsDTO();
            var.fromModel(e);
            json = gson.toJson(var);

            IndexRequest indexRequest = new IndexRequest(EVENT_INDEX_NAME);

            indexRequest.source(json, XContentType.JSON);
            IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        }

    }

}
