//******************************************************************************
//                          DataLogic.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.bll;

import com.mongodb.client.model.CountOptions;
import com.mongodb.client.result.DeleteResult;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.data.api.DataExportDTO;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.DeviceOrTargetToDataException;
import org.opensilex.core.exception.DeviceProvenanceAmbiguityException;
import org.opensilex.core.exception.NoVariableDataTypeException;
import org.opensilex.core.exception.ProvenanceAgentTypeException;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.StreamWithPagination;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class containing all logic used by DataAPI, handles usage of different DAOs
 */
public class DataLogic {
    //TODO Call other Logic classes, one day this class should only directly use a single Dao, DataDao(V2)

    //Data initialized in constructor :
    private final DataDaoV2 dao;
    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel user;

    public DataLogic(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user) {
        this.dao = new DataDaoV2(sparql, nosql, fs);
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.user = user;
    }

    //Private stored data
    private final Map<DeviceModel, List<URI>> variablesToDevices = new HashMap<>();
    private Map<URI, URI> rootDeviceTypes = null;


    //PUBLIC METHODS ==================================================================================================

    /**
     *
     * Validates - throws or inserts the models
     */
    public List<URI> addListData(List<DataModel> modelList) throws Exception{

        modelList = validData(modelList);

        dao.create(modelList);
        if(variablesToDevices.size() > 0) {

            DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
            for (Map.Entry variablesToDevice : variablesToDevices.entrySet() ){

                deviceDAO.associateVariablesToDevice((DeviceModel) variablesToDevice.getKey(),(List<URI>)variablesToDevice.getValue(), user );

            }
        }
        List<URI> createdResources = new ArrayList<>();
        for (DataModel data : modelList) {
            createdResources.add(data.getUri());
        }
        return createdResources;
    }

    public DataModel get(URI uri) throws NoSQLInvalidURIException {
        return dao.get(uri);
    }

    /**
     * Fetches and return all URIs for the variable with the xsd:date datatype.
     *
     * @return
     * @throws Exception
     */
    public Set<URI> getAllDateVariables() throws Exception {
        return new HashSet<>(sparql.searchURIs(VariableModel.class, null, selectBuilder -> {
            Var uriVar = SPARQLQueryHelper.makeVar(VariableModel.URI_FIELD);
            selectBuilder.addWhere(uriVar, Oeso.hasDataType.asNode(), XSD.date.asNode());
        }));
    }

    public <T> ListWithPagination<T> getDataList(MongoSearchQuery<DataModel, DataSearchFilter, T> query){
        return dao.searchWithPagination(query);
    }

    public StreamWithPagination<DataModel> getDataListStream(DataSearchFilter filter){
        return dao.searchAsStreamWithPagination(filter);
    }

    /**
     *
     * @param forWideFormat if true it's for wide export, else for long format
     * @return All of the data needed to perform an export
     */
    public DataExportInformation getDataExportInformation(boolean forWideFormat, DataSearchFilter filter, Logger logger) throws Exception {
        //Init everything needed for result
        Map<URI, VariableModel> variables = new HashMap<>();
        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();
        //Use this next map only in the case of wide format
        Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant = new HashMap<>();
        //And use this next map only when in long format
        HashMap<Instant, List<DataGetDTO>> dataByInstant = new HashMap<>();


        //Get data
        Instant start = Instant.now();
        List<DataModel> resultList = new ArrayList<>();
        //TODO optimization the prepare stuff can just go in the stream handler ?
        dao.searchAsStreamWithPagination(filter).forEach(resultList::add);

        Instant data = Instant.now();
        logger.debug(resultList.size() + " observations retrieved " + Long.toString(Duration.between(start, data).toMillis()) + " milliseconds elapsed");

        Set<URI> dateVariables = getAllDateVariables();


        //Prepare all the other things we have to fetch (variables, objects...)
        for (DataModel dataModel : resultList) {
            if (dataModel.getTarget() != null && !objects.containsKey(dataModel.getTarget())) {
                objects.put(dataModel.getTarget(), null);
            }

            if (!variables.containsKey(dataModel.getVariable())) {
                variables.put(dataModel.getVariable(), null);
            }

            if (!provenances.containsKey(dataModel.getProvenance().getUri())) {
                provenances.put(dataModel.getProvenance().getUri(), null);
            }

            if (forWideFormat && !dataByIndexAndInstant.containsKey(dataModel.getDate())) {
                dataByIndexAndInstant.put(dataModel.getDate(), new HashMap<>());
            }

            if(!forWideFormat){
                if (!dataByInstant.containsKey(dataModel.getDate())) {
                    dataByInstant.put(dataModel.getDate(), new ArrayList<>());
                }
                dataByInstant.get(dataModel.getDate()).add(DataGetDTO.getDtoFromModel(dataModel, dateVariables));
            }

            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
                    }
                    //Everything else in for loop is for wide format only
                    if(!forWideFormat){
                        continue;
                    }

                    ExportDataIndex exportDataIndex = new ExportDataIndex(
                            exp,
                            dataModel.getProvenance().getUri(),
                            dataModel.getTarget()
                    );

                    if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                        dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                    }
                    dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, exp, dateVariables));
                }
            } else if(forWideFormat) {
                //Everything in this else block is for wide format only
                ExportDataIndex exportDataIndex = new ExportDataIndex(
                        null,
                        dataModel.getProvenance().getUri(),
                        dataModel.getTarget()
                );

                if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                    dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                }
                dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, null, dateVariables));
            }
        }
        Instant dataTransform = Instant.now();
        logger.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");


        //Get other stuff we have to get (variables, objects, etc...)
        List<VariableModel> variablesModelList = new VariableDAO(sparql,nosql,fs).getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(new URI(SPARQLDeserializers.getShortURI(variableModel.getUri())), variableModel);
        }
        Instant variableTime = Instant.now();
        logger.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");

        OntologyDAO ontologyDao = new OntologyDAO(sparql);
        // Provide the experiment as context if there is only one, and only in wide format.
        URI context = null;
        if (forWideFormat && experiments.size() == 1) {
            context = experiments.keySet().stream().findFirst().get();
        }
        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), context);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        logger.debug("Get " + objectsList.size() + " target(s) " + Long.toString(Duration.between(variableTime, targetTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDaoV2 provenanceDao = new ProvenanceDaoV2(nosql.getServiceV2());
        List<ProvenanceModel> provenanceModels = provenanceDao.findByUris(provenances.keySet().parallelStream(), provenances.size());
        for (ProvenanceModel prov : provenanceModels) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        logger.debug("Get " + provenanceModels.size() + " provenance(s) " + Long.toString(Duration.between(targetTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        logger.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");


        //Handle return
        DataExportInformation result;
        if(forWideFormat){
            result = new DataWideExportInformation();
            ((DataWideExportInformation)result).setDataByIndexAndInstant(dataByIndexAndInstant);
        }else{
            result = new DataLongExportInformation();
            ((DataLongExportInformation)result).setDataByInstant(dataByInstant);
        }
        result.setVariables(variables)
                .setObjects(objects)
                .setProvenances(provenances)
                .setExperiments(experiments);

        return result;
    }

    public List<ProvenanceModel> searchUsedProvenances(
            List<URI> experiments,
            List<URI> targets,
            List<URI> variables,
            List<URI> devices) {

        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        filter.setExperiments(experiments);
        filter.setTargets(targets);
        filter.setVariables(variables);
        filter.setDevices(devices);

        List<URI> provenanceURIs = dao.distinct(null, "provenance.uri", URI.class, filter);
        List<ProvenanceModel> resultList = new ArrayList<>();

        if(!provenanceURIs.isEmpty()){
            //TODO dont use ProvenanceDaoV2 when provenance logic class has been created
            ProvenanceDaoV2 provenanceDao = new ProvenanceDaoV2(nosql.getServiceV2());
            resultList = provenanceDao.findByUris(provenanceURIs.stream(), provenanceURIs.size());
        }
        return resultList;
    }

    public long countData(DataSearchFilter filter, CountOptions countOptions){
        return dao.count(null, filter, countOptions);
    }

    public void delete(URI uri) throws NoSQLInvalidURIException {
        dao.delete(uri);
    }

    public void updateConfidence(URI dataUri, Float confidence) throws NoSQLInvalidURIException{
        DataModel data = dao.get(dataUri);
        data.setConfidence(confidence);
        dao.update(data);
    }

    public void update(DataModel model) throws Exception{
        validData(Collections.singletonList(model));
        dao.update(model);
    }

    public DeleteResult deleteManyByFilter(DataSearchFilter filter){
        return dao.deleteMany(filter);
    }

    //PRIVATE METHODS ================================================================================================
    /**
     * Check variable data list before creation
     * Complete the prov_was_associated_with provenance attribut
     *
     * @param dataList
     * @throws Exception
     */
    private List<DataModel> validData(List<DataModel> dataList) throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql,nosql,fs);

        Map<URI, VariableModel> variableURIs = new HashMap<>();
        Set<URI> notFoundedVariableURIs = new HashSet<>();
        Set<URI> targetURIs = new HashSet<>();
        Set<URI> notFoundedTargetURIs = new HashSet<>();
        Set<URI> provenanceURIs = new HashSet<>();
        Set<URI> notFoundedProvenanceURIs = new HashSet<>();
        Set<URI> expURIs = new HashSet<>();
        Set<URI> notFoundedExpURIs = new HashSet<>();
        Map<DeviceModel, URI> variableCheckedDevice =  new HashMap<>();
        Map<URI, DeviceModel> provenanceToDevice =  new HashMap<>();

        List<DataModel> validData = new ArrayList<>();
        for (DataModel data : dataList) {

            boolean hasTarget = false;
            // check variable uri and datatype
            if (data.getVariable() != null) {  // and if null ?
                VariableModel variable = null;
                URI variableURI = data.getVariable();
                if (!variableURIs.containsKey(variableURI)) {
                    variable = variableDAO.get(variableURI);
                    if (variable == null) {
                        notFoundedVariableURIs.add(variableURI);
                    } else {
                        if (variable.getDataType() == null) {
                            throw new NoVariableDataTypeException(variableURI);
                        }
                        variableURIs.put(variableURI, variable);
                    }
                } else {
                    variable = variableURIs.get(variableURI);

                }
                if(!notFoundedVariableURIs.contains(variableURI)) {
                    setDataValidValue(variable, data);
                }
            }


            //check targets uri
            if (data.getTarget() != null) {
                hasTarget = true ;
                if (!targetURIs.contains(data.getTarget())) {
                    targetURIs.add(data.getTarget());
                    if (!sparql.uriExists((Node) null, data.getTarget())) {
                        hasTarget = false ;
                        notFoundedTargetURIs.add(data.getTarget());
                    }
                }
            }

            //check provenance uri and variables device association
            ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
            if (!provenanceURIs.contains(data.getProvenance().getUri())) {
                provenanceURIs.add(data.getProvenance().getUri());
                if (!provDAO.exists(data.getProvenance().getUri())) {
                    notFoundedProvenanceURIs.add(data.getProvenance().getUri());
                }
            }

            if(!notFoundedProvenanceURIs.contains(data.getProvenance().getUri())){
                variablesDeviceAssociation(provDAO, data, hasTarget, variableCheckedDevice, provenanceToDevice);
            }

            // check experiments uri
            if (data.getProvenance().getExperiments() != null) {
                for (URI exp : data.getProvenance().getExperiments()) {
                    if (!expURIs.contains(exp)) {
                        expURIs.add(exp);
                        if (!sparql.uriExists(ExperimentModel.class, exp)) {
                            notFoundedExpURIs.add(exp);
                        }
                    }
                }
            }
            validData.add(data);
        }

        if (!notFoundedVariableURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong variable uris: ", new ArrayList<>(notFoundedVariableURIs));// NOSQL Exception ? come from sparql request
        }
        if (!notFoundedTargetURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong target uris", new ArrayList<>(notFoundedTargetURIs)); // NOSQL Exception ? come from sparql request
        }
        if (!notFoundedProvenanceURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong provenance uris: ", new ArrayList<>(notFoundedProvenanceURIs));
        }
        if (!notFoundedExpURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong experiments uris: ", new ArrayList<>(notFoundedExpURIs)); // NOSQL Exception ? come from sparql request
        }

        return validData;
    }

    /**
     * check that value is coherent with the variable datatype
     *
     * @param variable
     * @param data
     * @throws Exception
     */
    private void setDataValidValue(VariableModel variable, DataModel data) throws Exception {
        if (data.getValue() != null) {
            URI variableUri = variable.getUri();
            URI dataType = variable.getDataType();
            Object value = data.getValue();
            DataValidateUtils.checkAndConvertValue(data, variableUri, value, dataType);
        }
    }

    // Map who associate each type with its root type
    private Map<URI, URI> getRootDeviceTypes() throws Exception {

        SPARQLTreeListModel<ClassModel> treeList = SPARQLModule.getOntologyStoreInstance().searchSubClasses(new URI(Oeso.Device.toString()), null, user.getLanguage(), true);
        List<ResourceTreeDTO> treeDtos = ResourceTreeDTO.fromResourceTree(treeList);

        Map<URI, URI> map = new HashMap<>();

        for (ResourceTreeDTO tree : treeDtos) {
            URI agentRootType = tree.getUri();
            List<ResourceTreeDTO> children = tree.getChildren();
            if (!children.isEmpty()) {
                childrenToRoot(children, map, agentRootType);
            }

            // Push root type inside map
            // It allow to recognize device with a type included inside the root types list
            map.put(tree.getUri(),tree.getUri());
        }

        return map;
    }

    private void childrenToRoot( List<ResourceTreeDTO> children,Map<URI, URI> map, URI agentRootType){
        for (ResourceTreeDTO subTree : children) {
            map.put(subTree.getUri(), agentRootType);
            List<ResourceTreeDTO> child = subTree.getChildren();
            if (!child.isEmpty()) {
                childrenToRoot(child, map, agentRootType);
            }
        }
    }

    /**
     * First check in the data provenance if there is a device
     * Then check in the provenance and fill the data provenance with the device
     * If no device and no target return an exception
     *
     * @param provDAO
     * @param data
     * @param hasTarget
     * @param variableCheckedDevice
     * @param provenanceToDevice

     * @throws Exception
     */
    private void variablesDeviceAssociation(ProvenanceDaoV2 provDAO, DataModel data, boolean hasTarget, Map<DeviceModel, URI> variableCheckedDevice, Map<URI, DeviceModel> provenanceToDevice) throws Exception{

        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        URI provenanceURI = data.getProvenance().getUri();
        DeviceModel deviceFromProvWasAssociated = checkAndReturnDeviceFromDataProvenance(data, deviceDAO);
        if (deviceFromProvWasAssociated == null) {

            DeviceModel device = null;
            if(provenanceToDevice.containsKey(provenanceURI)) {
                device = provenanceToDevice.get(provenanceURI);
                //check
            } else {
                device = checkAndReturnDeviceFromProvenance(deviceDAO, provDAO, data);
                provenanceToDevice.put(provenanceURI,device);
            }

            if (device != null) {

                if (!variableIsAssociatedToDevice(device, data.getVariable())) {
                    addVariableToDevice(device,data.getVariable()); // add variable/device
                }

                if (rootDeviceTypes == null) {
                    rootDeviceTypes = getRootDeviceTypes();
                }

                DataProvenanceModel provMod = data.getProvenance();
                List<ProvEntityModel> agents = null;
                if (provMod.getProvWasAssociatedWith() == null) {
                    agents = new ArrayList<>();
                } else {
                    agents = provMod.getProvWasAssociatedWith();
                }

                ProvEntityModel agent = new ProvEntityModel();
                agent.setUri(device.getUri());
                URI rootType = rootDeviceTypes.get(device.getType());
                agent.setType(rootType);
                agents.add(agent);
                provMod.setProvWasAssociatedWith(agents);
                data.setProvenance(provMod);
            } else {
                if(!hasTarget) {
                    throw new DeviceOrTargetToDataException(data);
                }
            }

        } else {
            boolean deviceIsChecked = variableCheckedDevice.containsKey(deviceFromProvWasAssociated) && variableCheckedDevice.get(deviceFromProvWasAssociated) == data.getVariable() ;
            if(!deviceIsChecked){
                if (!variableIsAssociatedToDevice(deviceFromProvWasAssociated, data.getVariable())) {
                    addVariableToDevice(deviceFromProvWasAssociated,data.getVariable()); // add variable/device
                }
                variableCheckedDevice.put(deviceFromProvWasAssociated,data.getVariable());

            }
        }
    }

    //TODO Duplicate method in DataCSVValidation model, it seems to be exactly the same
    private void addVariableToDevice(DeviceModel device, URI variable) {

        if (!variablesToDevices.containsKey(device)) {
            List<URI> list = new ArrayList<>();
            list.add(variable);
            variablesToDevices.put(device, list);
        } else {
            if (!variablesToDevices.get(device).contains(variable)) {
                variablesToDevices.get(device).add(variable);
            }
        }
    }

    /**
     * check if variable is associated to device
     * @param device
     * @param variable
     * @throws Exception
     */
    private boolean variableIsAssociatedToDevice(DeviceModel device, URI variable){
        List<SPARQLModelRelation> variables = device.getRelations(Oeso.measures).collect(Collectors.toList());

        if (!variables.isEmpty()) {
            if (variables.stream().anyMatch(var -> (SPARQLDeserializers.compareURIs(var.getValue(), variable.toString())))) {
                return true;
            }
        }
        return false;

    }

    /**
     * check and return Device from Data Provenance if no ambiguity
     * Exception if two devices as provenance agent
     *
     * @param deviceDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromDataProvenance(DataModel data, DeviceDAO deviceDAO) throws Exception{

        boolean deviceIsLinked = false; // to test if there are 2 devices
        URI agentToReturn = null;
        DeviceModel device = null;
        if(data.getProvenance().getProvWasAssociatedWith()!= null && !data.getProvenance().getProvWasAssociatedWith().isEmpty()){
            for (ProvEntityModel agent : data.getProvenance().getProvWasAssociatedWith()) {

                if(agent.getType() == null) {
                    throw new ProvenanceAgentTypeException(agent.getUri().toString());
                }

                if (deviceDAO.isDeviceType(agent.getType())) {
                    if(!deviceIsLinked) {
                        deviceIsLinked = true;
                        agentToReturn = agent.getUri();

                    } else {
                        throw new DeviceProvenanceAmbiguityException(data.getProvenance().getUri().toString());
                    }
                }
            }
            if(agentToReturn != null){
                device = deviceDAO.getDeviceByURI(agentToReturn, user);
            }
        }
        return device;
    }

    /**
     * check and return Device from Provenance if no ambiguity
     *
     * @param deviceDAO
     * @param provDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromProvenance(DeviceDAO deviceDAO, ProvenanceDaoV2 provDAO, DataModel data) throws Exception {

        ProvenanceModel provenance = provDAO.get(data.getProvenance().getUri());

        DeviceModel deviceToReturn = null ;
        List<DeviceModel> devices = new ArrayList<>();
        List<DeviceModel> linkedDevices = new ArrayList<>();

        if (provenance.getAgents() != null && !provenance.getAgents().isEmpty()) {

            for (AgentModel agent : provenance.getAgents()) {
                if(agent.getRdfType() == null) {
                    throw new ProvenanceAgentTypeException(agent.getUri().toString());
                }
                if (deviceDAO.isDeviceType(agent.getRdfType())) {
                    DeviceModel device = deviceDAO.getDeviceByURI(agent.getUri(), user);
                    if (device != null) {
                        devices.add(device);

                        if(variableIsAssociatedToDevice(device, data.getVariable())){
                            linkedDevices.add(device);
                        }
                    }
                }
            }

            switch (linkedDevices.size()) {
                case 0:
                    if (devices.size() > 1) {
                        throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());
                    } else {
                        if (!devices.isEmpty()) {
                            deviceToReturn = devices.get(0);
                        }
                    }
                    break;
                case 1:
                    deviceToReturn = linkedDevices.get(0);
                    break;
                default :
                    throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());

            }

        }
        return deviceToReturn;
    }
}
