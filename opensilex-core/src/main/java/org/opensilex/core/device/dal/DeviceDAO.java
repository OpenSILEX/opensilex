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
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.bson.Document;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.position.api.PositionGetDTO;
import org.opensilex.nosql.mongodb.metadata.MetaDataDao;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 *
 * @author sammy
 */
public class DeviceDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService mongodb;
    protected final FileStorageService fs;
    protected final MongoCollection<MetaDataModel> metaDataCollection;
    protected final Node defaultGraph;

    protected final MetaDataDao metaDataDao;

    public static final String ATTRIBUTES_COLLECTION_NAME = "deviceAttribute";
    private static final URI deviceURI = URI.create(Oeso.Device.getURI());

    public DeviceDAO(SPARQLService sparql, MongoDBService nosql, FileStorageService fs) throws SPARQLException {
        this.sparql = sparql;
        this.mongodb = nosql;
        this.fs = fs;

        defaultGraph = sparql.getDefaultGraph(DeviceModel.class);
        metaDataDao = new MetaDataDao(nosql);
        metaDataCollection = nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, MetaDataModel.class);
    }

    public void initDevice(DeviceModel devModel, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
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

    public MongoCollection<MetaDataModel> getAttributesCollection() {
        return mongodb.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, MetaDataModel.class);
    }

    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection<DeviceModel> attributeCollection = mongodb.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, DeviceModel.class);
        attributeCollection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), unicityOptions);
    }
    
    public void create(DeviceModel model) throws Exception {
        sparql.create(model);
    }

    public DeviceModel update(DeviceModel model) throws Exception {
        sparql.deleteByURI(defaultGraph, model.getUri());
        sparql.create(model);
        return model;
    }

    public ListWithPagination<DeviceModel> search(DeviceSearchFilter filter) throws Exception {

        Document metadata = filter.getMetadata();
        Integer year = filter.getYear();
        UserModel currentUser = filter.getCurrentUser();
        boolean includeSubTypes = filter.getIncludeSubTypes();
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

        ListWithPagination<DeviceModel> returnList;

        if (metadata != null && (filteredUris == null || filteredUris.isEmpty())) {
            return new ListWithPagination<>(new ArrayList<>());
        } else {
            // set the custom filter on type
            Map<String, WhereHandler> customHandlerByFields = new HashMap<>();

            if (includeSubTypes) {
                appendTypeFilter(customHandlerByFields, rdfType);
            }

            returnList = sparql.searchWithPagination(
                    sparql.getDefaultGraph(DeviceModel.class),
                    DeviceModel.class,
                    currentUser.getLanguage(),
                    (SelectBuilder select) -> {                        
                        if (namePattern != null && !namePattern.trim().isEmpty()) {
                            select.addFilter(SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, namePattern));
                        }
                        if (rdfType != null && !includeSubTypes) {
                            select.addFilter(SPARQLQueryHelper.eq(SPARQLResourceModel.TYPE_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(rdfType.toString()))));
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
                        Node uriVar = NodeFactory.createVariable(SPARQLResourceModel.URI_FIELD);
                        if(variable != null) {
                            SPARQLQueryHelper.appendRelationFilter(select, defaultGraph.getURI(), uriVar, Oeso.measures, variable);
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
                            select.addFilter(SPARQLQueryHelper.inURIFilter(SPARQLResourceModel.URI_FIELD, filteredUris));
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
            metadata.forEach((key,value) -> filter.put("attribute." + key,value));
        }
        return mongodb.distinct(MongoModel.URI_FIELD, URI.class, ATTRIBUTES_COLLECTION_NAME, filter);
    }

    public List<DeviceModel> searchForExport(DeviceSearchFilter filter) throws Exception {

        Document metadata = filter.getMetadata();
        Integer year = filter.getYear();
        UserModel currentUser = filter.getCurrentUser();
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
            deviceList = new ArrayList<>();
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
                            select.addFilter(SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, namePattern));
                        }
                        if (rdfType != null && !includeSubTypes) {
                            select.addFilter(SPARQLQueryHelper.eq(SPARQLResourceModel.TYPE_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(rdfType.toString()))));
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
                            Node uriVar = NodeFactory.createVariable(SPARQLResourceModel.URI_FIELD);
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
                            select.addFilter(SPARQLQueryHelper.inURIFilter(SPARQLResourceModel.URI_FIELD, filteredUris));
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
        metaDataDao.getMetaDataAssociatedTo(
                metaDataCollection, // filter in Device attribute collection
                MongoModel.URI_FIELD, // use MetaData URI field
                deviceList, // get Metadata associated with Device uris
                DeviceModel::setMetadata // update Device metadata
        );

        return deviceList;
    }

    private void appendDateFilters(SelectBuilder select, LocalDate date) throws Exception {
        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(DeviceModel.STARTUP_FIELD, date, null, null);
        select.addFilter(dateRangeExpr);
    }
    
    private void appendTypeFilter(Map<String, WhereHandler> customHandlerByFields, URI type) {
        if (type != null) {
            WhereHandler handler = new WhereHandler();
            handler.addWhere(new TriplePath(makeVar(SPARQLResourceModel.TYPE_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(type)));
            customHandlerByFields.put(SPARQLResourceModel.TYPE_FIELD, handler);
        }
    }
    
    /**
     * Linked variables to a device by the Measure relation
     * @param device device
     * @param variables variables to associate
     * @throws Exception if some error is encountered 
     *
     * **/
    public void associateVariablesToDevice(DeviceModel device, List<URI> variables) throws Exception {
        
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
       
       update(device);
    }

    public DeviceModel getDeviceByURI(URI deviceURI, UserModel currentUser) throws Exception {
        DeviceModel device = sparql.getByURI(DeviceModel.class, deviceURI, currentUser.getLanguage());
        if (device != null) {
            metaDataDao.getMetaDataAssociatedTo(metaDataCollection,MongoModel.URI_FIELD,device,DeviceModel::setMetadata);
        }
        return device;
    }

    public List<DeviceModel> getDevicesByURI(List<URI> devicesURI, UserModel currentUser) throws Exception {
        List<DeviceModel> devices = null;
        if (sparql.uriListExists(DeviceModel.class, devicesURI)) {
            devices = sparql.getListByURIs(DeviceModel.class, devicesURI, currentUser.getLanguage());
        }
        if (devices != null) {
            metaDataDao.getMetaDataAssociatedTo(metaDataCollection,MongoModel.URI_FIELD,devices,DeviceModel::setMetadata);
        }
        return devices;
    }

    /**
     *
     * @param uri uri of device
     * @throws ForbiddenURIAccessException if the device is linked to some existing data, datafile or provenance.
     * @throws Exception if some error is encountered during delete
     *
     * @see org.opensilex.core.provenance.dal.ProvenanceModel
     * @see org.opensilex.core.data.dal.DataModel
     * @see org.opensilex.core.data.dal.DataFileModel
     */
    public void delete(URI uri) throws Exception {
        sparql.delete(DeviceModel.class,uri);
    }

    public List<VariableModel> getDeviceVariables(URI uri, String language) throws Exception {
        List<URI> variableURIs = sparql.searchPrimitives(sparql.getDefaultGraph(DeviceModel.class), uri, Oeso.measures, URI.class);
        return sparql.getListByURIs(VariableModel.class, variableURIs, language);
    }

    public List<DeviceModel> getList(List<URI> uris, UserModel userModel) throws Exception {
        return sparql.getListByURIs(DeviceModel.class, uris, userModel.getLanguage());
    }

    public DeviceModel getByName(String name) throws Exception {
        //pageSize=2 in order to detect duplicated names
        ListWithPagination<DeviceModel> results = sparql.searchWithPagination(
            DeviceModel.class,
            null,
            (SelectBuilder select) -> select.addFilter(SPARQLQueryHelper.eq(SPARQLNamedResourceModel.NAME_FIELD, name)),
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

        return results.getList().get(0);
    }

    public FacilityModel getAssociatedFacility(URI deviceURI, UserModel currentUser) throws Exception {

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, mongodb,null);
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

                OrganizationDAO orgaDAO = new OrganizationDAO(sparql, mongodb);
                FacilityDAO infraDAO = new FacilityDAO(sparql, mongodb, orgaDAO);
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
