/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.extensions;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.SKOS;
import static org.opensilex.core.factor.api.FactorAPI.CREDENTIAL_FACTOR_GROUP_ID;
import static org.opensilex.core.factor.api.FactorAPI.CREDENTIAL_FACTOR_GROUP_LABEL_KEY;
import static org.opensilex.core.factor.api.FactorAPI.CREDENTIAL_FACTOR_MODIFICATION_ID;
import static org.opensilex.core.factor.api.FactorAPI.CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY;
import static org.opensilex.core.factor.api.FactorAPI.FACTOR_EXAMPLE_URI;
import org.opensilex.core.factor.dal.FactorDAO;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author charlero
 */
public interface OntologyReferenceRessourceAPI {
    
    /**
     * Updates the on linked to an experiment.
     *
     * @param InstanceUri
     * @param ontologiesReferences
     * @return the query result
     */
    @PUT
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The list of ontologies references associated with this instance", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Invalid or unknown Instance URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public default Response putOntologiesReferences(
            @ApiParam(value = "Instance URI", example = "", required = true) @PathParam("uri") @NotNull URI InstanceUri,
            @ApiParam(value = "List of ontologies references") ArrayList<OntologyReference> ontologiesReferences) {
        try {
            this.withOntologiesReferences(InstanceUri,ontologiesReferences);
            return new ObjectUriResponse(Response.Status.OK, InstanceUri).getResponse();
        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Instance URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
    
    public void withOntologiesReferences(URI InstanceUri, List<OntologyReference> ontologiesReferences) throws SPARQLInvalidURIException, Exception;
}
