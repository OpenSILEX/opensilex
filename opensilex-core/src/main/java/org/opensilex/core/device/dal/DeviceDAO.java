/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.dal;

import com.mongodb.client.MongoCollection;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.net.URI;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import java.util.List;
import java.time.LocalDate;
import org.apache.jena.arq.querybuilder.ExprFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.bson.Document;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.CoreModule;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author sammy
 */
public class DeviceDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;

    public static final String ATTRIBUTES_COLLECTION_NAME = "deviceAttribute";

    public void initDevice(DeviceModel devModel, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(devModel.getType(), new URI(Oeso.Device.getURI()), currentUser.getLanguage());

        if (relations != null) {
            for (RDFObjectRelationDTO relation : relations) {
                URI prop = relation.getProperty();
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
        }
        return storedAttributes;

    }

    public DeviceDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
    }
    
    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection attributeCollection = nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, DeviceModel.class);
        attributeCollection.createIndex(Indexes.ascending("uri"), unicityOptions);
    }
    
    public URI create(DeviceModel devModel, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {

        initDevice(devModel, relations, currentUser);

        ClassModel classModel = CoreModule.getOntologyCacheInstance().getClassModel(
                devModel.getType(),
                new URI(Oeso.Device.getURI()),
                currentUser.getLanguage()
        );
        devModel.setTypeLabel(classModel.getLabel());

        if (!MapUtils.isEmpty(devModel.getAttributes())) {

            MongoCollection<DeviceAttributeModel> collection = getAttributesCollection();
            collection.createIndex(Indexes.ascending("uri"), new IndexOptions().unique(true));
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

    public ListWithPagination<DeviceModel> search(
            String namePattern,
            URI rdfType,
            boolean includeSubTypes,
            Integer year,
            LocalDate existenceDate,
            String brandPattern,
            String modelPattern,
            String snPattern,
            Document metadata,
            UserModel currentUser,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {
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
            return new ListWithPagination<>(new ArrayList());
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
                    orderByList,
                    page,
                    pageSize);
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
        Set<URI> devicesURIs = nosql.distinct("uri", URI.class, ATTRIBUTES_COLLECTION_NAME, filter);
        return devicesURIs;
    }

    public List<DeviceModel> searchForExport(
            String namePattern,
            URI rdfType,
            boolean includeSubTypes,
            Integer year,
            LocalDate existenceDate,
            String brandPattern,
            String modelPattern,
            String snPattern,
            Document metadata,
            UserModel currentUser) throws Exception {
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

    public DeviceModel update(DeviceModel instance, List<RDFObjectRelationDTO> relations, UserModel user) throws Exception {
        createIndexes();
        DeviceAttributeModel storedAttributes = getStoredAttributes(instance.getUri());
        initDevice(instance, relations, user);
        Node graph = sparql.getDefaultGraph(DeviceModel.class);
        if ((instance.getAttributes() == null || instance.getAttributes().isEmpty()) && storedAttributes == null) {
            sparql.deleteByURI(graph, instance.getUri());
            sparql.create(instance);
        } else {
            nosql.startTransaction();
            sparql.startTransaction();
            sparql.deleteByURI(graph, instance.getUri());
            sparql.create(instance);
            MongoCollection collection = getAttributesCollection();

            try {
                if (instance.getAttributes() != null && !instance.getAttributes().isEmpty()) {
                    DeviceAttributeModel model = new DeviceAttributeModel();
                    model.setUri(instance.getUri());
                    model.setAttribute(instance.getAttributes());
                    if (storedAttributes != null) {
                        collection.findOneAndReplace(nosql.getSession(), eq("uri", instance.getUri()), model);
                    } else {
                        collection.insertOne(nosql.getSession(), model);
                    }
                } else {
                    collection.findOneAndDelete(nosql.getSession(), eq("uri", instance.getUri()));
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

    public DeviceModel getDeviceByURI(URI deviceURI, UserModel currentUser) throws Exception {
        DeviceModel device = sparql.getByURI(DeviceModel.class, deviceURI, currentUser.getLanguage());
        if (device != null) {
            DeviceAttributeModel storedAttributes = getStoredAttributes(device.getUri());
            if (storedAttributes != null) {
                device.setAttributes(storedAttributes.getAttribute());
            }
        }
        return device;
    }

    public List<DeviceModel> getDevicesByURI(List<URI> devicesURI, UserModel currentUser) throws Exception {
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

    public void delete(URI deviceURI, UserModel currentUser) throws Exception {
        nosql.startTransaction();
        sparql.startTransaction();
        sparql.delete(DeviceModel.class, deviceURI);
        MongoCollection collection = getAttributesCollection();

        try {
            collection.findOneAndDelete(nosql.getSession(), eq("uri", deviceURI));
            nosql.commitTransaction();
            sparql.commitTransaction();
        } catch (Exception ex) {
            nosql.rollbackTransaction();
            sparql.rollbackTransaction(ex);
        }
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

        return results.getList().get(0);
    }

    public boolean isDeviceType(URI rdfType) throws SPARQLException {
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(rdfType), Ontology.subClassAny, Oeso.Device)
        );
    }
}
