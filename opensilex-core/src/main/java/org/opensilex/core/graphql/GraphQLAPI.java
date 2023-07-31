/*******************************************************************************
 *                         GraphQLAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 28/07/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.swagger.annotations.Api;
import org.bson.Document;
import org.opensilex.core.device.dal.DeviceAttributeModel;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;


/**
 * @author rigolle
 */
@Api("GraphQL")
@Path("core/graphql")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLAPI {
    @Inject
    SPARQLService sparql;

    @Inject
    MongoDBService mongo;

    //@todo brancher à une ressource RDF -> OK. Test requête custom ? (ex. OS dans XP)
    //@todo brancher à une ressource mongo -> OK (ex. provenance)
    //@todo faire la liaison mongo <-> RDF -> OK (ex. deviceAttribute - device)
    //@todo injection de dépendance dans les fetchers ?
    @GET
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGraphQL(
            @QueryParam("query") String query
    ) {
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query(query)
                .build();

        return Response
                .ok(execute(input).toSpecification())
                .build();
    }

    @POST
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postGraphQL(
            @Valid GraphQLPostDTO postDTO
    ) {
        ExecutionInput.Builder inputBuilder = ExecutionInput.newExecutionInput()
                .query(postDTO.getQuery());

        if (postDTO.getOperationName() != null) {
            inputBuilder.operationName(postDTO.getOperationName());
        }

        if (postDTO.getVariables() != null) {
            inputBuilder.variables(postDTO.getVariables());
        }

        ExecutionInput input = inputBuilder.build();

        return Response
                .ok(execute(input).toSpecification())
                .build();
    }

    private ExecutionResult execute(ExecutionInput input) {
        // Définition du schéma
        InputStream schemaFile = getClass().getClassLoader().getResourceAsStream("graphql/schema.graphqls");
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schemaFile);

        // Chercher en bdd
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("hello", new StaticDataFetcher("world"))
                        .dataFetcher("experiments", environment -> sparql.search(ExperimentModel.class, "en"))
                        .dataFetcher("provenances", environment -> mongo.search(ProvenanceModel.class, ProvenanceDAO.PROVENANCE_COLLECTION_NAME, new Document(), null))
                        .dataFetcher("deviceAttributes", environment -> mongo.search(DeviceAttributeModel.class, DeviceDAO.ATTRIBUTES_COLLECTION_NAME, new Document(), null))
                )
                .type("DeviceAttribute", typeWiring -> typeWiring
                        .dataFetcher("device", environment -> sparql.getByURI(DeviceModel.class, environment.<DeviceAttributeModel>getSource().getUri(), "en"))
                )
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        return graphQL.execute(input);
    }
}
