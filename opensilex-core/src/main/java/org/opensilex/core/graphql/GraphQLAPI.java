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
import graphql.Scalars;
import graphql.language.*;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.swagger.annotations.Api;
import org.bson.Document;
import org.opensilex.OpenSilex;
import org.opensilex.core.device.dal.DeviceAttributeModel;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.mapping.SPARQLClassAnalyzer;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Set;

import static graphql.Scalars.GraphQLString;


/**
 * @author rigolle
 */
@Api("GraphQL")
@Path("core/graphql")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLAPI {

    @Inject
    OpenSilex openSilex;

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

    private GraphQLSchema exampleSchema() {
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
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }

    private static final TypeName STRING_TYPE = new TypeName("String");
    private static final NonNullType REQ_STRING_TYPE = new NonNullType(new TypeName("String"));

    private GraphQLSchema computeSchema() {
        TypeDefinitionRegistry registry = new TypeDefinitionRegistry();
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring();

        ObjectTypeDefinition.Builder queryTypeBuilder = ObjectTypeDefinition.newObjectTypeDefinition()
                .name("Query");
        SchemaDefinition schemaDefinition = SchemaDefinition.newSchemaDefinition()
                .operationTypeDefinition(OperationTypeDefinition.newOperationTypeDefinition()
                        .name("Query")
                        .typeName(new TypeName("Query"))
                        .build())
                .build();
        registry.add(schemaDefinition);

        try {
            for (Class clazz : openSilex.getReflections().getTypesAnnotatedWith(SPARQLResource.class, true)) {
                SPARQLResource resourceAnnotation = (SPARQLResource) clazz.getDeclaredAnnotation(SPARQLResource.class);
                if (resourceAnnotation == null) {
                    // Skip classes with no annotation
                    continue;
                }
                String typeName = clazz.getSimpleName();

                // Model type def
                ObjectTypeDefinition.Builder objectTypeBuilder = ObjectTypeDefinition.newObjectTypeDefinition()
                        .name(typeName);
                SPARQLClassAnalyzer analyzer = sparql.getForClass(clazz).getClassAnalizer();
                for (Field f : analyzer.getDataPropertyFields()) {
                    objectTypeBuilder
                            .fieldDefinition(FieldDefinition.newFieldDefinition()
                                    .name(f.getName())
                                    .type(STRING_TYPE)
                                    .build()
                            );
                }
                objectTypeBuilder.fieldDefinition(FieldDefinition.newFieldDefinition()
                        .name(analyzer.getURIFieldName())
                        .type(REQ_STRING_TYPE)
                        .build());
                registry.add(objectTypeBuilder.build());

                // Query
                String simpleQueryName = typeName;
                String listQueryName = simpleQueryName + "List";

                // Definition
                queryTypeBuilder
                        .fieldDefinition(FieldDefinition.newFieldDefinition()
                                .name(listQueryName)
                                .type(new ListType(new TypeName(typeName)))
                                .build())
                        .fieldDefinition(FieldDefinition.newFieldDefinition()
                                .name(simpleQueryName)
                                .type(new TypeName(typeName))
                                .inputValueDefinition(InputValueDefinition.newInputValueDefinition()
                                        .name(analyzer.getURIFieldName())
                                        .type(REQ_STRING_TYPE)
                                        .build())
                                .build());

                // Wiring
                wiring.type("Query", typeWiring -> typeWiring.dataFetcher(listQueryName, environment -> sparql.search(clazz, "en")));
                wiring.type("Query", typeWiring -> typeWiring.dataFetcher(simpleQueryName, environment -> sparql.getByURI(clazz, URI.create(environment.getArgument(analyzer.getURIFieldName())), "en")));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        registry.add(queryTypeBuilder.build());

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(registry, wiring.build());
    }

    private ExecutionResult execute(ExecutionInput input) {
        GraphQL graphQL = GraphQL.newGraphQL(computeSchema()).build();
        return graphQL.execute(input);
    }
}
