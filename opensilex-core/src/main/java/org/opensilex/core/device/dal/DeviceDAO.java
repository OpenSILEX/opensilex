/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.position.api.PositionGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import static org.opensilex.sparql.service.SPARQLService.TYPE_VAR;

/**
 *
 * @author sammy
 */
public class DeviceDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final FileStorageService fs;

    private static final URI deviceURI = URI.create(Oeso.Device.getURI());

    public DeviceDAO(SPARQLService sparql, MongoDBService nosql, FileStorageService fs) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
    }

    public void initDevice(DeviceModel devModel, List<RDFObjectRelationDTO> relations, AccountModel currentUser) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(devModel.getType(), deviceURI, currentUser.getLanguage());

        if (relations != null) {
            for (RDFObjectRelationDTO relation : relations) {
                URI prop = SPARQLDeserializers.formatURI(relation.getProperty());
                if (!ontologyDAO.validateThenAddObjectRelationValue(sparql.getDefaultGraphURI(DeviceModel.class), model, prop, relation.getValue(), devModel)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        devModel.addRelation(sparql.getDefaultGraphURI(DeviceModel.class), new URI(RDFS.label.getURI()), String.class, devModel.getName());
    }
    
    public URI create(DeviceModel devModel, AccountModel currentUser) throws Exception {

        ClassModel classModel = SPARQLModule.getOntologyStoreInstance().getClassModel(
                devModel.getType(),
                new URI(Oeso.Device.getURI()),
                currentUser.getLanguage()
        );
        devModel.setTypeLabel(classModel.getLabel());
        sparql.create(devModel, true);
        return devModel.getUri();
    }

    /**
     * Modifies select, using variable filter if not in the case of an export
     */
    private void addFiltersForSomeSearch(SelectBuilder select, DeviceSearchFilter filter, boolean forExport) throws Exception {
        Integer year = filter.getYear();
        Boolean includeSubTypes = filter.getIncludeSubTypes();
        String namePattern = filter.getNamePattern();
        URI rdfType = filter.getRdfType();
        URI variable = filter.getVariable();
        String brandPattern = filter.getBrandPattern();
        String modelPattern = filter.getModelPattern();
        String snPattern = filter.getSnPattern();
        LocalDate existenceDate = filter.getExistenceDate();
        LocalDate date;

        if (year != null) {
            String yearString = Integer.toString(year);
            date = LocalDate.parse(yearString + "-01-01");
        } else {
            date = null;
        }

        if (namePattern != null && !namePattern.trim().isEmpty()) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.NAME_FIELD, namePattern));
        }
        if (rdfType != null && !includeSubTypes) {
            select.addFilter(SPARQLQueryHelper.eq(DeviceModel.TYPE_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(rdfType.toString()))));
        }
        if (brandPattern != null && !brandPattern.trim().isEmpty()) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.BRAND_FIELD, brandPattern));
        }
        if (modelPattern != null && !modelPattern.trim().isEmpty()) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.MODEL_FIELD, modelPattern));
        }
        if (snPattern != null && !snPattern.trim().isEmpty()) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.SERIALNUMBER_FIELD, snPattern));
        }
        if (date != null) {
            appendDateFilters(select, date);
        }

        Node uriVar = NodeFactory.createVariable(DeviceModel.URI_FIELD);
        if(!forExport){
            String graph = sparql.getDefaultGraph(DeviceModel.class).getURI();
            if(variable != null) {
                SPARQLQueryHelper.appendRelationFilter(select,graph, uriVar, Oeso.measures, variable);
            }
        }

        DateDeserializer dateDeserializer = new DateDeserializer();
        ExprFactory exprFactory = new ExprFactory();
        if (existenceDate != null) {
            Node startupVar = NodeFactory.createVariable(DeviceModel.STARTUP_FIELD);
            Node removalVar = NodeFactory.createVariable(DeviceModel.REMOVAL_FIELD);

            WhereBuilder optionalRemoval = new WhereBuilder();
            optionalRemoval.addWhere(uriVar, Oeso.removal, removalVar);
            select.addFilter(
                    exprFactory.and(
                            exprFactory.le(startupVar, dateDeserializer.getNode(existenceDate)),
                            exprFactory.or(
                                    exprFactory.not(exprFactory.exists(optionalRemoval)),
                                    exprFactory.ge(removalVar, dateDeserializer.getNode(existenceDate))
                            )
                    )
            );
        }

        if (!CollectionUtils.isEmpty(filter.getIncludedUris())) {
            select.addFilter(SPARQLQueryHelper.inURIFilter(DeviceModel.URI_FIELD, filter.getIncludedUris()));
        }
    }

    public ListWithPagination<DeviceModel> search(DeviceSearchFilter filter) throws Exception {
        AccountModel currentUser = filter.getCurrentUser();
        Boolean includeSubTypes = filter.getIncludeSubTypes();
        URI rdfType = filter.getRdfType();

        ListWithPagination<DeviceModel> returnList = null;

        // set the custom filter on type
        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();

        if (BooleanUtils.isTrue(includeSubTypes)) {
            appendTypeFilter(customHandlerByFields, rdfType);
        }

        returnList = sparql.searchWithPagination(
                sparql.getDefaultGraph(DeviceModel.class),
                DeviceModel.class,
                currentUser.getLanguage(),
                (SelectBuilder select) -> {
                    this.addFiltersForSomeSearch(select, filter, false);
                },
                customHandlerByFields,
                null,
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize());


        return returnList;
    }

    public List<DeviceModel> searchForExport(DeviceSearchFilter filter) throws Exception {

        AccountModel currentUser = filter.getCurrentUser();
        Boolean includeSubTypes = filter.getIncludeSubTypes();
        URI rdfType = filter.getRdfType();

        List<DeviceModel> deviceList;
        // set the custom filter on type
        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        if (includeSubTypes) {
            appendTypeFilter(customHandlerByFields, rdfType);
        }
        deviceList = sparql.search(
                sparql.getDefaultGraph(DeviceModel.class),
                DeviceModel.class,
                currentUser.getLanguage(),
                (SelectBuilder select) -> {
                    this.addFiltersForSomeSearch(select, filter, true);
                },
            customHandlerByFields,
            null,
            null,
            0,
            0
        );
        return deviceList;
    }

    private void appendDateFilters(SelectBuilder select, LocalDate date) throws Exception {
        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(DeviceModel.STARTUP_FIELD, date, null, null);
        select.addFilter(dateRangeExpr);
    }
    
    private void appendTypeFilter(Map<String, WhereHandler> customHandlerByFields, URI type) throws Exception {
        if (type != null) {
            WhereHandler handler = new WhereHandler();
            handler.addWhere(new TriplePath(makeVar(DeviceModel.TYPE_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(type)));
            customHandlerByFields.put(DeviceModel.TYPE_FIELD, handler);
        }
    }
    
    /**
     * Link devices to variable(s) with {@link Oeso#measures} property
     * @param deviceToVariables Association of devices <-> variables
     *
     * @apiNote For each device, the last date of modification is updated
     */
    public void linkDevicesToVariables(@NotNull Map<URI, Set<URI>> deviceToVariables) throws SPARQLException {
        Objects.requireNonNull(deviceToVariables);
        if(deviceToVariables.isEmpty()){
            return;
        }
        UpdateBuilder update = new UpdateBuilder();

        Node graph = sparql.getDefaultGraph(DeviceModel.class);
        Node measuresNode = Oeso.measures.asNode();
        boolean runUpdate = false;

        //An index to append to the rdfType var for each device so that it still works when they don't
        //have the same type
        int deviceIndex = 0;
        for(var entry : deviceToVariables.entrySet()){
            Set<URI> variables = entry.getValue();
            if(variables.isEmpty()){
                continue;
            }
            runUpdate = true;

            // Update link between device and variable
            // delete (device, measures, variable) triple and insert new (device, measures, variable) to avoid duplicate
            Node deviceNode = NodeFactory.createURI(URIDeserializer.getExpandedURI(entry.getKey()));
            variables.forEach(variable -> {
                Node variableNode = NodeFactory.createURI(URIDeserializer.getExpandedURI(variable));
                update.addDelete(graph, deviceNode, measuresNode, variableNode);
                update.addInsert(graph, deviceNode, measuresNode, variableNode);
            });

            // Add where clause in order to match the existing device
            Var deviceTypeVar = makeVar(SPARQLResourceModel.TYPE_FIELD + deviceIndex);
            update.addWhere(deviceTypeVar, Ontology.subClassAny, Oeso.Device.asNode())
                    .addGraph(graph, new WhereBuilder().addWhere(deviceNode, RDF.type, deviceTypeVar));
            ++deviceIndex;
        }

        if(runUpdate){
            sparql.executeUpdateQuery(update);
        }
    }


    public DeviceModel update(DeviceModel instance, AccountModel user) throws Exception {
        Node graph = sparql.getDefaultGraph(DeviceModel.class);
        instance.setLastUpdateDate(OffsetDateTime.now());
        sparql.update(graph, instance);
        return instance;
    }

    /**
     * WARNING : This method no longer returns Metadata with the device (mongo attributes) for an example of how to get Metadata look at the getByUri service!
     */
    public DeviceModel getDeviceByURI(URI deviceURI, AccountModel currentUser) throws Exception {
        return sparql.getByURI(DeviceModel.class, deviceURI, currentUser.getLanguage());
    }


    public List<DeviceModel> getDevicesByURI(List<URI> devicesURI, AccountModel currentUser) throws Exception {
        List<DeviceModel> devices = null;
        if (sparql.uriListExists(DeviceModel.class, devicesURI)) {
            devices = sparql.getListByURIs(DeviceModel.class, devicesURI, currentUser.getLanguage());
        }
        return devices;
    }

    public List<DeviceModel> getDevicesByFacility(URI facilityUri, AccountModel currentUser) throws Exception {
        List<DeviceModel> devices = new ArrayList<>();

        SelectBuilder select = new SelectBuilder();

        sparql.getDefaultGraph(MoveModel.class);
        Var target = makeVar("target");
        Var subject = makeVar("s");
        select.addVar(target);
        select.setDistinct(true);

        select.addWhere(subject, Oeev.to, SPARQLDeserializers.nodeURI(facilityUri))
            .addWhere(subject, Ontology.typeSubClassAny, Oeev.Move)
            .addWhere(subject, Oeev.concerns, target);

        List<SPARQLResult> list = sparql.executeSelectQuery(select);

        if (!list.isEmpty()) {
            list.forEach(l -> System.out.println(l.getStringValue("target")));

            List<URI> deviceUris = list.stream().map((x) -> URI.create(x.getStringValue("target"))).collect(Collectors.toList());
            devices = getDevicesByURI(deviceUris, currentUser);
        }

        return devices;
    }

    /**
     *
     * @param deviceURI uri of device
     * @param currentUser current user
     * @throws ForbiddenURIAccessException if the device is linked to some existing data, datafile or provenance.
     * @throws Exception if some error is encountered during delete
     *
     * @see org.opensilex.core.provenance.dal.ProvenanceModel
     * @see org.opensilex.core.data.dal.DataModel
     * @see org.opensilex.core.data.dal.DataFileModel
     */
    public void delete(URI deviceURI, AccountModel currentUser) throws Exception {
        
        // test if device in provenances
        ProvenanceDAO provenanceDAO = new ProvenanceDAO(nosql, sparql);
        int provCount = provenanceDAO.count(null, null, null, null, null, null, deviceURI);
        if(provCount > 0) {
            throw new ForbiddenURIAccessException(deviceURI, provCount+" provenance(s)");
        }
        
        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        int dataCount = dataDAO.count(currentUser, null, null, null, null, Collections.singletonList(deviceURI),null, null, null, null, null, null);
        if(dataCount > 0){
            throw new ForbiddenURIAccessException(deviceURI, dataCount+" data");
        }  
        
        int dataFileCount = dataDAO.countFiles(currentUser, null, null, null, null, Collections.singletonList(deviceURI),null, null, null, null);
        if(dataFileCount > 0) {
            throw new ForbiddenURIAccessException(deviceURI, dataFileCount + " datafile(s)");
        }

        deleteVariableLinks(deviceURI);
        sparql.delete(DeviceModel.class, deviceURI);
    }

    private void deleteVariableLinks(URI deviceUri) throws Exception {
        Node graph = sparql.getDefaultGraph(DeviceModel.class);

        UpdateBuilder delete = new UpdateBuilder();
        Var objectVar = makeVar("o");
        delete.addDelete(graph, SPARQLDeserializers.nodeURI(deviceUri), Oeso.measures.asNode(), objectVar);
        delete.addWhere(new WhereBuilder().addGraph(graph, SPARQLDeserializers.nodeURI(deviceUri), Oeso.measures.asNode(), objectVar));

        sparql.executeDeleteQuery(delete);
    }

    public List<VariableModel> getDeviceVariables(URI uri, String language) throws Exception {
        List<URI> variableURIs = sparql.searchPrimitives(sparql.getDefaultGraph(DeviceModel.class), uri, Oeso.measures, URI.class);
        return sparql.getListByURIs(VariableModel.class, variableURIs, language);
    }

    public List<DeviceModel> getList(List<URI> uris, AccountModel accountModel) throws Exception {
        List<DeviceModel> devices = sparql.getListByURIs(DeviceModel.class, uris, accountModel.getLanguage());
        return devices;
    }

    public DeviceModel getByName(String name) throws Exception {
        //pageSize=2 in order to detect duplicated names
        ListWithPagination<DeviceModel> results = sparql.searchWithPagination(
            DeviceModel.class,
            null,
            (SelectBuilder select) -> {
                select.addFilter(SPARQLQueryHelper.eq(DeviceModel.NAME_FIELD, name));
            },
            null,
            0,
            2
        );
        
        if (results.getList().isEmpty()) {
            return null;
        }
        
        if (results.getList().size() > 1) {
            throw new DuplicateNameException(name);
        }

        DeviceModel device = results.getList().get(0);

        return device;
    }

    public DeviceModel getDeviceByNameOrURI(AccountModel user, String deviceNameOrUri) throws Exception {
        DeviceModel device;
        if (URIDeserializer.validateURI(deviceNameOrUri)) {
            URI deviceURI = URI.create(deviceNameOrUri);
            device = getDeviceByURI(deviceURI, user);
        } else {
            device = getByName(deviceNameOrUri);
        }
        return device;
    }

    // Map which associates each type with its root type
    public Map<URI, URI> getRootDeviceTypes(AccountModel user) throws Exception {

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
            // It allows the recognition of device with a type included inside the root types list
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

    public FacilityModel getAssociatedFacility(URI deviceURI, AccountModel currentUser) throws Exception {

        MoveLogic moveLogic = new MoveLogic(sparql, nosql, currentUser);

        MoveModel moveEvent = moveLogic.getLastMoveAfter(deviceURI, null);

        FacilityModel facility = null;

        List<PositionGetDTO> resultDTOList = new ArrayList<>();
        if (moveEvent != null) {
            var positionHistory = moveLogic.getPositionsHistory(
                    deviceURI,
                    null,
                    null,
                    null,
                    null,
                    0,
                    0
            );

            positionHistory.forEach((move) -> {
                try {
                    resultDTOList.add(new PositionGetDTO(move, move.getNoSqlModel().getTargetPositions().get(0).getPosition()));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            });

            PositionGetDTO lastPosition = resultDTOList.get(0);
            if (lastPosition.getTo() != null) {
                URI facilityUri = new URI(URIDeserializer.getShortURI(lastPosition.getTo().getUri().toString()));

                FacilityLogic infraLogic = new FacilityLogic(sparql, nosql.getServiceV2());
                facility = infraLogic.get(facilityUri, currentUser);
            }
        }

        return facility;
    }

    public boolean isDeviceType(URI rdfType) throws SPARQLException {
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(rdfType), Ontology.subClassAny, Oeso.Device)
        );
    }
}
