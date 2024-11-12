package org.opensilex.core.data.bll;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.CSVDataTypeException;
import org.opensilex.core.exception.DataTypeException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.provenance.dal.GlobalProvenanceEntity;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SparqlMinimalFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.query.SparqlMultiClassQuery;
import org.opensilex.sparql.service.query.SparqlMultiGraphQuery;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used for performing the validation of {@link DataModel} during insertion
 * It runs in 3 steps :
 *
 * <ul>
 *     <li>Collect fields by reading the provided list and grouping values with one batch/field to validate. Complexity : O(n) with n model</li>
 *     <li>Perform validations by using SPARQL/Mongodb dao on a batch of values for each field to validate </li>
 *     <li>Re-read the models by updating field if required or by applying more complex validation which required a previous call to database. Complexity O(n)</li>
 * </ul>
 * <p>
 * The field group (the field or list of field which are validated are the following) :
 * <br><br>
 * <ul>
 *     <li><b>(variable, value)</b>
 *     <ul>
 *         <li>Execute a single SPARQL query for retrieving uniques variables</li>
 *         <li>Since variable datatype is required for checking data value type coherence, the query must fetch variables from the database</li>
 *     </ul>
 *     </li>
 *     <li><b>(target, provenance.experiments)</b>
 *     <ul>
 *         <li>Execute a SPARQL query per distinct experiment for validating access</li>
 *         <li>Execute a SPARQL query per distinct experiment for checking target existence (as Scientific Object or Facility)</li>
 *         <li>Since we only need to check if the target exists, the query don't fetch results from database but only check for existence</li>
 *     </ul>
 *     <li><b>(provenance, provenance.wasAssociatedWith)</b>
 *     <ul>
 *         <li>Execute a MQL query for retrieving each provenance</li>
 *         <li>Execute a SPARQL query to check if the provenance agent is an {@link AccountModel} or a {@link DeviceModel})</li>
 *         <li>Execute a SPARQL and/or a MQL query to check if the provenance activity exists. For now it can be any object in the TripleStore or a {@link org.opensilex.core.data.dal.DataFileModel}</li>
 *     </ul>
 *     </li>
 * </ul>
 *
 * @author rcolin
 */
public class DataValidation {

    // Data structures for collecting values before a batch validation
    final Map<String, VariableModel> variableByUri;
    final Map<String, Map<String, URI>> targetsByXp;
    final Map<String, ProvenanceModel> provenancesByUri;
    final Map<String, SPARQLResourceModel> agentByUri;
    final Map<String, SPARQLResourceModel> activitiesByUri;

    final Set<URI> variableURIs;
    final Set<URI> experimentURIs;
    final Set<URI> provenanceURIs;

    final Collection<DataModel> models;
    final SPARQLService sparql;
    final MongoDBService mongodb;
    final AccountModel user;

    final SparqlNoProxyFetcher<VariableModel> variableFetcher;

    private static final List<Class<? extends SPARQLResourceModel>> agentClasses = List.of(DeviceModel.class, AccountModel.class);

    public DataValidation(Collection<DataModel> models, SPARQLService sparql, MongoDBService mongodb, AccountModel user) {
        this.models = models;
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.user = user;

        try {
            variableFetcher =  new SparqlNoProxyFetcher<>(VariableModel.class, sparql, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Prefer the user of PatriciaTrie instead of Map with URI as key due to faster string compare and lower memory space
        variableByUri = new PatriciaTrie<>();
        targetsByXp = new PatriciaTrie<>();
        provenancesByUri = new PatriciaTrie<>();
        agentByUri = new PatriciaTrie<>();
        activitiesByUri = new PatriciaTrie<>();

        experimentURIs = new HashSet<>();
        provenanceURIs = new HashSet<>();
        variableURIs = new HashSet<>();
    }

    public DataPostInsert validate() throws Exception {
        // Perform validation
        collectValidation();
        batchValidations();
        DataPostInsert postInsert = updateModels();

        // Clean collections to reduce memory footprint
        variableByUri.clear();
        targetsByXp.clear();
        provenancesByUri.clear();
        agentByUri.clear();
        experimentURIs.clear();
        provenanceURIs.clear();

        return postInsert;
    }

    private void collectValidation() {
        // collect fields for which a validation is required
        for (DataModel data : models) {

            // collect variable
            if( ! variableByUri.containsKey(data.getVariable().toString())){
                variableByUri.put(data.getVariable().toString(), null);
                variableURIs.add(data.getVariable());
            }

            // collect provenance URI
            DataProvenanceModel provenance = data.getProvenance();
            String provUri = provenance.getUri().toString();
            if (!provenancesByUri.containsKey(provUri)) {
                provenancesByUri.put(provUri, null);
                provenanceURIs.add(provenance.getUri());
            }

            // collect provenance agents
            if (provenance.getProvWasAssociatedWith() != null) {
                provenance.getProvWasAssociatedWith().forEach(agent -> agentByUri.put(agent.getUri().toString(), null));
            }

            // collect provenance activities
            if (provenance.getProvUsed() != null) {
                provenance.getProvUsed().forEach(activity -> activitiesByUri.put(activity.getUri().toString(), null));
            }

            // collect provenance for experiments/target
            if (provenance.getExperiments() != null) {
                for (URI xp : provenance.getExperiments()) {
                    // collect the experiment without checking if there are an target. Since we still need to check experiment permissions (with or without target)
                    var xpTargets = targetsByXp.computeIfAbsent(xp.toString(), k -> new PatriciaTrie<>());
                    experimentURIs.add(xp);
                    if(data.getTarget() != null){
                        xpTargets.put(data.getTarget().toString(), data.getTarget());
                    }
                }
            } else if (data.getTarget() != null) {
                // use an empty string to diff the case where xp is not present yet (null)
                targetsByXp.computeIfAbsent("", key -> new PatriciaTrie<>()).put(data.getTarget().toString(), data.getTarget());
            }
        }
    }

    private void variableAndDataTypeValidation() throws Exception {

        // Load variable list -> throw SPARQLInvalidUriListException if any variable is unknown
        List<VariableModel> variables = sparql.loadListByURIs(
                sparql.getDefaultGraph(VariableModel.class),
                VariableModel.class,
                variableURIs,
                user.getLanguage(),
                result -> variableFetcher.getInstance(result, user.getLanguage()),
                null
        );

        // Here we assume that models from the Dao return a well formatted URI (Same format as variableByUri)
        variables.forEach(model -> variableByUri.put(model.getUri().toString(), model));
    }

    private static final String NO_TARGET_FOUND_ERROR_MSG = "Some target were not found. Each target must be a Facility or a Scientific object declared inside the experiment %s ";
    private static final String NO_ACTIVITY_FOUND_ERROR_MSG = "Some provenance activity were not found. Each activity must exists ";
    private static final String NO_AGENT_FOUND_ERROR_MSG = "Some provenance agent were not found. Each agent must be an existing Device or Account ";

    private void experimentAndTargetValidation() throws Exception {

        // Validate user permissions on experiments
        ExperimentDAO xpDao = new ExperimentDAO(sparql, mongodb);
        xpDao.validateExperimentAccess(experimentURIs, user);

        Node facilityGraph = sparql.getDefaultGraph(FacilityModel.class);
        Node osType = sparql.getRDFType(ScientificObjectModel.class).asNode();
        Node facilityType = sparql.getRDFType(FacilityModel.class).asNode();

        // Validate existence of target according experiment
        for (var xpTargets : targetsByXp.entrySet()) {
            var targetByUri = xpTargets.getValue();
            if(targetByUri.isEmpty()){
                continue;
            }

            String xp = xpTargets.getKey();
            Node xpGraph = xp.isEmpty() ?
                    sparql.getDefaultGraph(ScientificObjectModel.class) :
                    NodeFactory.createURI(URIDeserializer.getExpandedURI(xp));

            // Check if the target is an ScientificObject from the os graph (experiment or global) || is a Facility from the Facility graph
            new SparqlMultiGraphQuery<>(
                    sparql,
                    Map.of(osType, xpGraph, facilityType, facilityGraph),
                    targetByUri.keySet()
            ).checkUnknowns(String.format(NO_TARGET_FOUND_ERROR_MSG, xp));
        }
    }

    private void provenanceValidation() throws NoSQLInvalidUriListException, SPARQLException {

        // Check provenance, keep models for device/provenance coherence checking
        List<ProvenanceModel> provenances = new ProvenanceDaoV2(mongodb.getServiceV2()).findByUris(
                provenanceURIs.stream(), // no format of URI since the DAO handle encoding of URI according database storage format of URI
                provenanceURIs.size()
        );
        if (provenances.size() < provenancesByUri.size()) {
            // compute difference : delete existing provenance from all provenance
            provenances.forEach(model -> provenanceURIs.remove(model.getUri()));
            throw new NoSQLInvalidUriListException("Unknown provenance uris: ", provenanceURIs);
        } else {
            provenances.forEach(model -> provenancesByUri.put(model.getUri().toString(), model));
        }

        var fetcher = new SparqlMinimalFetcher<>(SPARQLResourceModel.class);

        // Get agents from device or account
        if(! agentByUri.isEmpty()){
            new SparqlMultiClassQuery<>(agentClasses, agentByUri.keySet(), sparql).getResults(
                    result -> fetcher.getInstance(result, null),
                    NO_AGENT_FOUND_ERROR_MSG
            ).forEach(agent -> agentByUri.put(agent.getUri().toString(), agent));
        }

        // Get existing activities (find inside all the repository)
        if(! activitiesByUri.isEmpty()){
            new SparqlMultiGraphQuery<>(sparql, Collections.emptyMap(), activitiesByUri.keySet()).getResults(
                    result -> fetcher.getInstance(result, null),
                    NO_ACTIVITY_FOUND_ERROR_MSG
            ).forEach(activity -> activitiesByUri.put(activity.getUri().toString(), activity));
        }
    }

    private void batchValidations() throws Exception {
        variableAndDataTypeValidation();
        experimentAndTargetValidation();
        provenanceValidation();
    }

    /**
     * Check that value is coherent with the variable datatype
     */
    private void setDataValidValue(VariableModel variable, DataModel data) throws CSVDataTypeException, DataTypeException {
        URI variableUri = variable.getUri();
        URI dataType = variable.getDataType();
        Object value = data.getValue();
        DataValidateUtils.checkAndConvertValue(data, variableUri, value, dataType);
    }


    private DataPostInsert updateModels() throws CSVDataTypeException, DataTypeException {

        DataPostInsert postInsert = new DataPostInsert();

        // 2 local map used to cache ProvEntityModel by uri,
        // The goal is to reuse ProvEntityModel when they came from the same AgentModel/ActivityModel (and so to minimize object re-creation)
        Map<String, List<ProvEntityModel>> cachedAgents = new PatriciaTrie<>();
        Map<String, List<ProvEntityModel>> cachedActivities = new PatriciaTrie<>();


        // Perform 2nd step of validation
        for (DataModel data : models) {

            // Check variable uri and check and set datatype
            VariableModel variable = variableByUri.get(data.getVariable().toString());
            setDataValidValue(variable, data);

            // Update data provenance
            DataProvenanceModel dataProvenance = data.getProvenance();

            // Use local provenance activities and agents
            var activities = updateProvenanceEntities(dataProvenance, dataProvenance.getProvUsed(), activitiesByUri, cachedActivities);
            dataProvenance.setProvUsed(activities);

            var agents = updateProvenanceEntities(dataProvenance, dataProvenance.getProvWasAssociatedWith(), agentByUri, cachedAgents);
            dataProvenance.setProvWasAssociatedWith(agents);

            // update device to variable association
            dataProvenance.getProvWasAssociatedWith().forEach(agent -> {
                var variables = postInsert.devicesToVariables.computeIfAbsent(agent.getUri(), newDevice -> new HashSet<>());
                variables.add(variable.getUri());
            });

            data.setPublisher(user.getUri());
        }

        return postInsert;
    }

    private List<ProvEntityModel> updateProvenanceEntities(DataProvenanceModel dataProvenance,
                                                           List<ProvEntityModel> globalProvEntities,
                                                           Map<String, SPARQLResourceModel> entityByUri,
                                                           Map<String, List<ProvEntityModel>> agentsToProvEntity
                                                           ) {

        // Complete agent with type from associated agent
        if (!CollectionUtils.isEmpty(globalProvEntities)) {
            globalProvEntities.forEach(entity -> {
                SPARQLResourceModel cachedModel = entityByUri.get(entity.getUri().toString());
                entity.setType(cachedModel.getType());
            });
            return globalProvEntities;
        }

        ProvenanceModel provenance = provenancesByUri.get(dataProvenance.getUri().toString());

        // Convert agents from global provenance to data provenance agents (only if not done before)
        // We use a map in order for object caching, in order to avoid excessive object creation (ProvEntityModel and AgentModel have the same properties)
        String provenanceURI = provenance.getUri().toString();
        List<ProvEntityModel> provEntities = agentsToProvEntity.get(provenanceURI);
        List<? extends GlobalProvenanceEntity> entities = provenance.getAgents();

        // provEntities not yet converted and the global provenance has agent to complete
        if (provEntities != null || entities == null) {
           return Collections.emptyList();
        }
        // Convert AgentModel (global provenance) to ProvEntityModel (local provenance)
        provEntities = entities
                .stream()
                .map(ProvEntityModel::new)
                .collect(Collectors.toList());

        agentsToProvEntity.put(provenanceURI, provEntities);
        return provEntities;
    }

}