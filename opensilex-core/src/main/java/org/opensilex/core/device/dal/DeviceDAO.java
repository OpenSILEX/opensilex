/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.dal;

import com.mongodb.client.MongoCollection;
import static com.mongodb.client.model.Filters.eq;
import java.net.URI;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import java.util.List;
import java.time.LocalDate;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.device.api.DeviceDTO;
import java.util.ArrayList;
import java.util.Set;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.bson.Document;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.device.api.DeviceCreationDTO;
import org.opensilex.core.device.api.DeviceDTO;
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
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author sammy
 */
public class DeviceDAO {
    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    
    public static final String ATTRIBUTES_COLLECTION_NAME = "devicesAttributes";
    
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
    
    public MongoCollection getAttributesCollection() {
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
    
    public URI create(DeviceModel devModel, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {   
        URI deviceType = devModel.getType();
        URI deviceURI = devModel.getUri();
        String deviceName = devModel.getName();
        
        initDevice(devModel, relations, currentUser);
        
        if (deviceURI == null) {
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            ClassModel model = ontologyDAO.getClassModel(deviceType, new URI(Oeso.Device.getURI()), currentUser.getLanguage());
            DeviceURIGenerator uriGenerator = new DeviceURIGenerator(sparql.getDefaultGraphURI(DeviceModel.class));
            deviceURI = uriGenerator.generateURI(model.getName(), deviceName, 0);
        }
        if(sparql.uriExists(sparql.getDefaultGraphURI(DeviceModel.class), deviceURI)) {
                throw new SPARQLAlreadyExistingUriException(deviceURI);
        }
        
        devModel.setUri(deviceURI);
        
        if(devModel.getAttributes() != null && !devModel.getAttributes().isEmpty()){
            MongoCollection collection = getAttributesCollection();
            sparql.startTransaction();
            nosql.startTransaction();
            try {
                sparql.create(devModel);
                DeviceAttributeModel model = new DeviceAttributeModel();
                model.setUri(devModel.getUri());
                model.setAttribute(devModel.getAttributes());
                collection.insertOne(nosql.getSession(), model);
                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            } 
        }else{
            sparql.create(devModel,false);
        }
        
        return devModel.getUri();
    }
    
    public ListWithPagination<DeviceModel> search( 
                String namePattern,
                URI rdfType,
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
        LocalDate date ;
        if (year != null) {
            String yearString = Integer.toString(year);
            date = LocalDate.parse(yearString + "-01-01");
        }else {
            date=null;
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
            returnList = sparql.searchWithPagination(
                DeviceModel.class,
                currentUser.getLanguage(),
                (SelectBuilder select) -> {
                    if (namePattern != null && !namePattern.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.NAME_FIELD, namePattern));
                    }
                    if (rdfType != null) {
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
                    if(date != null){
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

                    if(filteredUris != null){
                        select.addFilter(SPARQLQueryHelper.inURIFilter(DeviceModel.URI_FIELD, filteredUris));
                    }
                },
                orderByList,
                page,
                pageSize);
        }
        
        return returnList;
    }

    private Set<URI> filterURIsOnAttributes(Document metadata) {
        Document filter = new Document();
        if (metadata != null) {
            for (String key:metadata.keySet()) {
                filter.put("attribute." + key, metadata.get(key));
            }
        }
        Set<URI> devicesURIs = nosql.distinct("uri", URI.class, ATTRIBUTES_COLLECTION_NAME, filter);
        return devicesURIs;
    }
    
    public List<DeviceModel> searchForExport(
                String namePattern,
                URI rdfType,
                Integer year,
                LocalDate existenceDate,
                String brandPattern,
                String modelPattern,
                String snPattern,
                Document metadata,
                UserModel currentUser) throws Exception{
        LocalDate date ;
        if (year != null) {
            String yearString = Integer.toString(year);
            date = LocalDate.parse(yearString + "-01-01");
        }else {
            date=null;
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
            deviceList = sparql.search(
                DeviceModel.class,
                currentUser.getLanguage(),
                (SelectBuilder select) -> {
                    if (namePattern != null && !namePattern.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.NAME_FIELD, namePattern));
                    }
                    if (rdfType != null) {
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
                    if(date != null){
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
                    
                    if(filteredUris != null){
                        select.addFilter(SPARQLQueryHelper.inURIFilter(DeviceModel.URI_FIELD, filteredUris));
                    }
                }
            );
        }
        
        //get metadata part from mongo
        for (DeviceModel device:deviceList) {
            DeviceAttributeModel storedAttributes = getStoredAttributes(device.getUri());
            if (storedAttributes != null) {
                device.setAttributes(storedAttributes.getAttribute());
            }
        }
        return deviceList;
    }

    private void appendDateFilters(SelectBuilder select, LocalDate Date) throws Exception {
        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(DeviceModel.STARTUP_FIELD, Date,null,null);
        select.addFilter(dateRangeExpr);
    }

    public DeviceModel update(DeviceModel instance, List<RDFObjectRelationDTO> relations, UserModel user) throws Exception {
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
        if(sparql.uriListExists(DeviceModel.class, devicesURI)){
            devices = sparql.getListByURIs(DeviceModel.class, devicesURI, currentUser.getLanguage());
        }
        if(devices != null){
            for(DeviceModel device: devices){
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
}
