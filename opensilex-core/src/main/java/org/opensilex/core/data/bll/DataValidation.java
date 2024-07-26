package org.opensilex.core.data.bll;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.graph.Node;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.CSVDataTypeException;
import org.opensilex.core.exception.DataTypeException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.mapping.SparqlMapper;
import org.opensilex.sparql.mapping.SparqlMinimalFetcher;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An object used for performing validation of {@link DataModel}
 * It run in 3 steps :
 *
 * <ul>
 *     <li>Collect fields by reading the provided list and grouping values with one batch/field to validate. Complexity : O(n) with n model</li>
 *     <li>
 *         Perform validations by using SPARQL/Mongodb dao on a batch of values for each field to validate.
 *         <ul>
 *              <li> Complexity : O(k) with k the maximum number of distinct values collected inside a group</li>
 *              <li>Request complexity (the number of executed request): O(m) with m fields group to validate</li>
 *          </ul>
 *     </li>
 *     <li>Re-read the models by updating field if required or by applying more complex validation which required a previous call to database. Complexity O(n)</li>
 * </ul>
 * <p>
 * The field group (the field or list of field which are validated are the following) :
 * <br><br>
 * <ul>
 *     <li><b>(variable, value)</b>
 *     <ul>
 *         <li>Check that the variable URI exist</li>
 *         <li>Check that the variable datatype is coherent with the value</li>
 *     </ul>
 *     </li>
 *     <li><b>(target, provenance.experiments)</b>
 *     <ul>
 *         <li>Check that the experiment exists</li>
 *         <li>Check that the user has the permissions on the given experiment</li>
 *         <li>Check that the target belongs to the experiment (if experiment is null, just check if it exists)</li>
 *     </ul>
 *     </li>
 *     <li><b>(provenance, provenance.wasAssociatedWith)</b>
 *     <ul>
 *         <li>Check that the provenance URI (provenance.uri) exists)</li>
 *         <li>Check that ant agent declared inside  provenance.wasAssociatedWith exists</li>
 *     </ul>
 *     </li>
 * </ul>
 *
 * @author rcolin
 */
public class DataValidation {

    // Data structures for collecting values before a batch validation
    Map<String, VariableModel> variableByUri;
    Map<String, Map<String, Boolean>> targetsByXp;
    Map<String, ProvenanceModel> provenancesByUri;
    Map<String, DeviceModel> deviceByUri;

    final Collection<DataModel> models;
    final SPARQLService sparql;
    final MongoDBService mongodb;
    final AccountModel user;

    public DataValidation(Collection<DataModel> models, SPARQLService sparql, MongoDBService mongodb, AccountModel user) {
        this.models = models;
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.user = user;

        // Prefer the user of PatriciaTrie instead of Map with URI as key due to faster string compare and lower memory space
        variableByUri = new PatriciaTrie<>();
        targetsByXp = new PatriciaTrie<>();
        provenancesByUri = new PatriciaTrie<>();
        deviceByUri = new PatriciaTrie<>();
    }

    /**
     * Validate the input models and return validation results
     */
    public DataPostInsert validate() throws Exception {
        DataPostInsert postInsert = new DataPostInsert();
        collectValidation();
        batchValidations();
        updateModels(postInsert);
        return postInsert;
    }

    private void collectValidation() {
        // collect fields for which a validation is required : variable, provenance, xp, target, device
        for (DataModel data : models) {
            variableByUri.put(data.getVariable().toString(), null);

            DataProvenanceModel provenance = data.getProvenance();
            provenancesByUri.put(provenance.getUri().toString(), null);

            if (!CollectionUtils.isEmpty(provenance.getProvWasAssociatedWith())) {
                provenance.getProvWasAssociatedWith().forEach(agent -> deviceByUri.put(agent.getUri().toString(), null));
            }

            if (provenance.getExperiments() != null) {
                provenance.getExperiments().forEach(xp ->
                        targetsByXp.computeIfAbsent(xp.toString(), key -> new PatriciaTrie<>()).put(data.getTarget().toString(), Boolean.TRUE)
                );
            } else if (data.getTarget() != null) {
                targetsByXp.computeIfAbsent("", key -> new PatriciaTrie<>()).put(data.getTarget().toString(), Boolean.TRUE);
            }
        }
    }

    private void batchValidations() throws Exception {

        // Load variable list -> throw Exception if any variable is unknown
        List<VariableModel> variables = new VariableDAO(sparql, null, null, user).loadListByURIs(
                variableByUri
                        .keySet()
                        .stream()
                        .map(URI::create),
                user.getLanguage()
        );
        variables.forEach(model -> variableByUri.put(model.getUri().toString(), model));

        // Validate user permissions with experiment and existence of targets according experiment
        // Validate both target and xp existence
        ExperimentDAO xpDao = new ExperimentDAO(sparql, mongodb);
        for (var xpTargets : targetsByXp.entrySet()) {

            String xp = xpTargets.getKey();
            Set<String> targets = xpTargets.getValue().keySet();
            Node graph = null;

            if (!xp.isEmpty()) {
                xpDao.validateExperimentAccess(URI.create(xp), user);
                graph = SPARQLDeserializers.nodeURI(xp);
            }

            Set<URI> unknownTargets = sparql.getExistingUriStream(
                    ScientificObjectModel.class,
                    targets.stream(),
                    targets.size(),
                    false,
                    graph
            );
            if (!unknownTargets.isEmpty()) {
                throw new SPARQLInvalidUriListException("These targets are unknown from the experiment : " + xp, unknownTargets); // NOSQL Exception ? come from sparql request
            }
        }

        // Check provenance, keep models for device/provenance coherence checking
        List<ProvenanceModel> provenances = new ProvenanceDaoV2(mongodb.getServiceV2()).findByUris(
                provenancesByUri
                        .keySet()
                        .stream()
                        .map(URI::create), // no format of URI since the DAO handle encoding of URI according database storage format of URI
                provenancesByUri.size()
        );
        if (provenances.size() < provenancesByUri.size()) {
            // compute difference : delete existing provenance from all provenance
            provenances.forEach(model -> provenancesByUri.remove(model.getUri().toString()));
            throw new NoSQLInvalidUriListException("Unknown provenance uris: ", provenancesByUri.keySet());
        } else {
            provenances.forEach(model -> provenancesByUri.put(model.getUri().toString(), model));
        }


        // Check devices and fetch models
        // #TODO -> use DeviceDao instead of generic sparql. Here we don't want to fetch other device properties
        //  (this should be available for any sparql dao to apply a custom projection/fetching)
        SparqlMapper<DeviceModel> sparqlMapper = new SparqlMinimalFetcher<>(DeviceModel.class);
        List<DeviceModel> devices = sparql.loadListByURIs(
                sparql.getDefaultGraph(DeviceModel.class),
                DeviceModel.class,
                deviceByUri.keySet().stream().map(URI::create),
                user.getLanguage(),
                result -> sparqlMapper.getInstance(result, user.getLanguage()),
                Collections.emptySet()
        );
        devices.forEach(model -> deviceByUri.put(model.getUri().toString(), model));
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

    private void updateModels(DataPostInsert postInsert) throws CSVDataTypeException, DataTypeException {

        Map<String, List<ProvEntityModel>> agentsToProvEntity = new PatriciaTrie<>();

        // Perform 2nd step of validation
        for (DataModel data : models) {

            // check variable uri and datatype
            VariableModel variable = variableByUri.get(data.getVariable().toString());
            setDataValidValue(variable, data);

            Set<URI> variableDevices = postInsert.variableToDevices.computeIfAbsent(variable.getUri(), key -> new HashSet<>());

            // Complete agent with type from associated device
            DataProvenanceModel dataProvenance = data.getProvenance();
            if (!CollectionUtils.isEmpty(dataProvenance.getProvWasAssociatedWith())) {
                dataProvenance.getProvWasAssociatedWith().forEach(agent -> {
                    DeviceModel device = deviceByUri.get(agent.getUri().toString());
                    agent.setType(device.getType());
                });
            } else {
                ProvenanceModel provenance = provenancesByUri.get(dataProvenance.getUri().toString());

                // convert agents from global provenance to data provenance agents (only if not done before)
                String provenanceURI = provenance.getUri().toString();
                List<ProvEntityModel> provEntities = agentsToProvEntity.get(provenanceURI);
                if (provEntities == null) {
                    provEntities = provenance
                            .getAgents()
                            .stream()
                            .map(agent -> {
                                ProvEntityModel provEntityModel = new ProvEntityModel();
                                provEntityModel.setUri(agent.getUri());
                                provEntityModel.setType(agent.getRdfType());
                                return provEntityModel;
                            })
                            .collect(Collectors.toList());

                    agentsToProvEntity.put(provenanceURI, provEntities);
                }
                dataProvenance.setProvWasAssociatedWith(provEntities);
            }

            dataProvenance.getProvWasAssociatedWith().forEach(device -> variableDevices.add(device.getUri()));
            data.setPublisher(user.getUri());
        }
    }

}
