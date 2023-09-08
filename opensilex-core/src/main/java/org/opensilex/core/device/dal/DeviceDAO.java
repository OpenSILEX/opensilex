/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.bson.Document;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.position.api.PositionGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 *
 * @author sammy
 */
public class DeviceDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final FileStorageService fs;

    public static final String ATTRIBUTES_COLLECTION_NAME = "deviceAttribute";
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
                if (!ontologyDAO.validateObjectValue(sparql.getDefaultGraphURI(DeviceModel.class), model, prop, relation.getValue(), devModel)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        devModel.addRelation(sparql.getDefaultGraphURI(DeviceModel.class), new URI(RDFS.label.getURI()), String.class, devModel.getName());
    }

    public MongoCollection<DeviceAttributeModel> getAttributesCollection() {
        return nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, DeviceAttributeModel.class);
    }

    private DeviceAttributeModel getStoredAttributes(URI uri) {
        DeviceAttributeModel storedAttributes = null;
        try {
            storedAttributes = nosql.findByURI(DeviceAttributeModel.class, ATTRIBUTES_COLLECTION_NAME, uri);
        } catch (NoSQLInvalidURIException e) {
            // no attribute found, just continue since it's optional
        }
        return storedAttributes;

    }

    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection<DeviceModel> attributeCollection = nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, DeviceModel.class);
        attributeCollection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), unicityOptions);
    }
    
    public URI create(DeviceModel devModel, AccountModel currentUser) throws Exception {


        ClassModel classModel = SPARQLModule.getOntologyStoreInstance().getClassModel(
                devModel.getType(),
                new URI(Oeso.Device.getURI()),
                currentUser.getLanguage()
        );
        devModel.setTypeLabel(classModel.getLabel());

        if (!MapUtils.isEmpty(devModel.getAttributes())) {

            MongoCollection<DeviceAttributeModel> collection = getAttributesCollection();
            collection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
            sparql.startTransaction();
            nosql.startTransaction();
            try {
                sparql.create(devModel);

                DeviceAttributeModel attributeModel = new DeviceAttributeModel();
                attributeModel.setUri(devModel.getUri());
                attributeModel.setAttribute(devModel.getAttributes());
                collection.insertOne(nosql.getSession(), attributeModel);

                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            }
        } else {
            sparql.create(devModel, true);
        }

        return devModel.getUri();
    }

    public ListWithPagination<DeviceModel> search(DeviceSearchFilter filter) throws Exception {

        Document metadata = filter.getMetadata();
        Integer year = filter.getYear();
        AccountModel currentUser = filter.getCurrentUser();
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

        final Set<URI> filteredUris;
        if (metadata != null) {
            filteredUris = filterURIsOnAttributes(metadata);
        } else {
            filteredUris = null;
        }

        ListWithPagination<DeviceModel> returnList = null;

        if (metadata != null && (filteredUris == null || filteredUris.isEmpty())) {
            return new ListWithPagination<>(new ArrayList<>());
        } else {
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
                        String graph = sparql.getDefaultGraph(DeviceModel.class).getURI();
                        Node uriVar = NodeFactory.createVariable(DeviceModel.URI_FIELD);
                        if(variable != null) {
                            SPARQLQueryHelper.appendRelationFilter(select,graph, uriVar, Oeso.measures, variable);
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

                        if (filteredUris != null) {
                            select.addFilter(SPARQLQueryHelper.inURIFilter(DeviceModel.URI_FIELD, filteredUris));
                        }
                    },
                    customHandlerByFields,
                    null,
                    filter.getOrderByList(),
                    filter.getPage(),
                    filter.getPageSize());
        }

        return returnList;
    }

    private Set<URI> filterURIsOnAttributes(Document metadata) {
        Document filter = new Document();
        if (metadata != null) {
            for (String key : metadata.keySet()) {
                filter.put("attribute." + key, metadata.get(key));
            }
        }
        return nosql.distinct(MongoModel.URI_FIELD, URI.class, ATTRIBUTES_COLLECTION_NAME, filter);
    }

    public List<DeviceModel> searchForExport(DeviceSearchFilter filter) throws Exception {

        Document metadata = filter.getMetadata();
        Integer year = filter.getYear();
        AccountModel currentUser = filter.getCurrentUser();
        Boolean includeSubTypes = filter.getIncludeSubTypes();
        String namePattern = filter.getNamePattern();
        URI rdfType = filter.getRdfType();
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

        final Set<URI> filteredUris;
        if (metadata != null) {
            filteredUris = filterURIsOnAttributes(metadata);
        } else {
            filteredUris = null;
        }

        List<DeviceModel> deviceList;
        if (metadata != null && (filteredUris == null || filteredUris.isEmpty())) {
            deviceList = new ArrayList();
        } else {
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
                        DateDeserializer dateDeserializer = new DateDeserializer();
                        ExprFactory exprFactory = new ExprFactory();
                        if (existenceDate != null) {
                            Node uriVar = NodeFactory.createVariable(DeviceModel.URI_FIELD);
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

                        if (filteredUris != null) {
                            select.addFilter(SPARQLQueryHelper.inURIFilter(DeviceModel.URI_FIELD, filteredUris));
                        }
                    },
                customHandlerByFields,
                null,
                null,
                0,
                0
            );
        }

        //get metadata part from mongo
        for (DeviceModel device : deviceList) {
            DeviceAttributeModel storedAttributes = getStoredAttributes(device.getUri());
            if (storedAttributes != null) {
                device.setAttributes(storedAttributes.getAttribute());
            }
        }
        return deviceList;
    }

    private void appendDateFilters(SelectBuilder select, LocalDate Date) throws Exception {
        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(DeviceModel.STARTUP_FIELD, Date, null, null);
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
     * Linked variables to a device by the 'oeso:measures' relation
     * @param device device
     * @param variables variables to associate
     * @throws Exception if some error is encountered 
     *
     * **/
    public DeviceModel associateVariablesToDevice(DeviceModel device, List<URI> variables, AccountModel user) throws Exception {
        
        List<SPARQLModelRelation> relations = device.getRelations();
        
        // Fix cause the addRelationQuads in SPARQLClassQueryBuilder need a not null relation type  
        // Works only if all relations are measure relations .. 
        List<SPARQLModelRelation> newRelations = new ArrayList<>();
        for (SPARQLModelRelation relation : relations) {
            SPARQLModelRelation rel = new SPARQLModelRelation();
            rel.setProperty(relation.getProperty());
            rel.setValue(relation.getValue());
            if(relation.getType() == null) {
                rel.setType(URI.class);
            }
            newRelations.add(rel);
        }
        // Fix
       
        for (URI variable : variables) {
            SPARQLModelRelation relation = new SPARQLModelRelation();
            relation.setProperty(Oeso.measures);
            relation.setValue(variable.toString());
            relation.setType(URI.class);
            newRelations.add(relation);
        }
       device.setRelations(newRelations);
       
       device = update(device, user);
       return device;
    }

    public DeviceModel update(DeviceModel instance, AccountModel user) throws Exception {
        createIndexes();
        DeviceAttributeModel storedAttributes = getStoredAttributes(instance.getUri());
        Node graph = sparql.getDefaultGraph(DeviceModel.class);
        instance.setLastUpdateDate(OffsetDateTime.now());
        if ((instance.getAttributes() == null || instance.getAttributes().isEmpty()) && storedAttributes == null) {
            sparql.deleteByURI(graph, instance.getUri());
            sparql.create(instance);
        } else {
            nosql.startTransaction();
            sparql.startTransaction();
            sparql.deleteByURI(graph, instance.getUri());
            sparql.create(instance);
            MongoCollection<DeviceAttributeModel> collection = getAttributesCollection();

            try {
                if (instance.getAttributes() != null && !instance.getAttributes().isEmpty()) {
                    DeviceAttributeModel model = new DeviceAttributeModel();
                    model.setUri(instance.getUri());
                    model.setAttribute(instance.getAttributes());
                    if (storedAttributes != null) {
                        collection.findOneAndReplace(nosql.getSession(), eq(MongoModel.URI_FIELD, instance.getUri()), model);
                    } else {
                        collection.insertOne(nosql.getSession(), model);
                    }
                } else {
                    collection.findOneAndDelete(nosql.getSession(), eq(MongoModel.URI_FIELD, instance.getUri()));
                }
                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            }

        }
        return instance;
    }

    public DeviceModel getDeviceByURI(URI deviceURI, AccountModel currentUser) throws Exception {
        DeviceModel device = sparql.getByURI(DeviceModel.class, deviceURI, currentUser.getLanguage());
        if (device != null) {
            Objects.requireNonNull(device.getUri());
            DeviceAttributeModel storedAttributes = getStoredAttributes(device.getUri());
            if (storedAttributes != null) {
                device.setAttributes(storedAttributes.getAttribute());
            }
        }
        return device;
    }


    public List<DeviceModel> getDevicesByURI(List<URI> devicesURI, AccountModel currentUser) throws Exception {
        List<DeviceModel> devices = null;
        if (sparql.uriListExists(DeviceModel.class, devicesURI)) {
            devices = sparql.getListByURIs(DeviceModel.class, devicesURI, currentUser.getLanguage());
        }
        if (devices != null) {
            for (DeviceModel device : devices) {
                if (device != null) {
                    DeviceAttributeModel storedAttributes = getStoredAttributes(device.getUri());
                    if (storedAttributes != null) {
                        device.setAttributes(storedAttributes.getAttribute());
                    }
                }
            }
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
        
        nosql.startTransaction();
        sparql.startTransaction();

        deleteVariableLinks(deviceURI);
        sparql.delete(DeviceModel.class, deviceURI);
        MongoCollection<DeviceAttributeModel> collection = getAttributesCollection();

        try {
            collection.findOneAndDelete(nosql.getSession(), eq(MongoModel.URI_FIELD, deviceURI));
            nosql.commitTransaction();
            sparql.commitTransaction();
        } catch (Exception ex) {
            nosql.rollbackTransaction();
            sparql.rollbackTransaction(ex);
        }
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

    public FacilityModel getAssociatedFacility(URI deviceURI, AccountModel currentUser) throws Exception {

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);
        MoveModel moveEvent = moveDAO.getLastMoveAfter(deviceURI, null);

        FacilityModel facility = null;

        List<PositionGetDTO> resultDTOList = new ArrayList<>();
        if (moveEvent != null) {
            LinkedHashMap<MoveModel, PositionModel> positionHistory = moveDAO.getPositionsHistory(
                    deviceURI,
                    null,
                    null,
                    null,
                    null,
                    0,
                    0
            );

            positionHistory.forEach((move, position) -> {
                try {
                    resultDTOList.add(new PositionGetDTO(move, position));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            });

            PositionGetDTO lastPosition = resultDTOList.get(0);
            if (lastPosition.getTo() != null) {
                URI facilityUri = new URI(URIDeserializer.getShortURI(lastPosition.getTo().getUri().toString()));

                OrganizationDAO orgaDAO = new OrganizationDAO(sparql, nosql);
                FacilityDAO infraDAO = new FacilityDAO(sparql, nosql, orgaDAO);
                facility = infraDAO.get(facilityUri, currentUser);
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
