/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.dal;

import java.net.URI;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import java.util.List;
import java.time.LocalDate;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.device.api.DeviceDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author sammy
 */
public class DeviceDAO {
    protected final SPARQLService sparql;
    
    private DeviceModel initDevice(URI deviceType, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(deviceType, new URI(Oeso.Device.getURI()), currentUser.getLanguage());

        DeviceModel device = new DeviceModel();
        
        if (relations != null) {
            for (RDFObjectRelationDTO relation : relations) {
                URI prop = relation.getProperty();
                if (!ontologyDAO.validateObjectValue(sparql.getDefaultGraphURI(DeviceModel.class), model, prop, relation.getValue(), device)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        device.addRelation(sparql.getDefaultGraphURI(DeviceModel.class), new URI(RDFS.label.getURI()), String.class, name);

        return device;
    }
    
    public DeviceDAO(SPARQLService sparql){
        this.sparql = sparql;
    }
    
    public URI create(DeviceDTO devDTO, UserModel currentUser) throws Exception {   
        URI deviceType = devDTO.getType();
        URI deviceURI = devDTO.getUri();
        String deviceName = devDTO.getName();
        List<RDFObjectRelationDTO> relations = devDTO.getRelations();
        
        DeviceModel device = initDevice(deviceType, deviceName, relations, currentUser);
        devDTO.toModel(device);
        
        if (deviceURI == null) {
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            ClassModel model = ontologyDAO.getClassModel(deviceType, new URI(Oeso.Device.getURI()), currentUser.getLanguage());
            DeviceURIGenerator uriGenerator = new DeviceURIGenerator(sparql.getDefaultGraphURI(DeviceModel.class));
            deviceURI = uriGenerator.generateURI(model.getName(), deviceName, 0);
            if(sparql.uriExists(sparql.getDefaultGraphURI(DeviceModel.class), deviceURI)) {
                throw new SPARQLAlreadyExistingUriException(deviceURI);
            }
        }
        device.setUri(deviceURI);
        
        sparql.create(device,false);
        
        return device.getUri();
    }
    
    public ListWithPagination<DeviceModel> search( String namePattern, List<URI> rdfTypes, Integer year, String brandPattern, String model, String snPattern,  UserModel currentUser, Integer page, Integer pageSize) throws Exception {
        LocalDate startDate ;
        LocalDate endDate;
        if (year != null) {
            String yearString = Integer.toString(year);
            startDate = LocalDate.parse(yearString + "-01-01");
            endDate = LocalDate.parse(yearString + "-12-31");
        }else {
            startDate=null;
            endDate=null;
        }
        return sparql.searchWithPagination(
                DeviceModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (namePattern != null && !namePattern.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.NAME_FIELD, namePattern));
                    }
                    if (rdfTypes != null && rdfTypes.size() > 0) {
                        select.addFilter(SPARQLQueryHelper.inURIFilter(DeviceModel.TYPE_FIELD, rdfTypes));
                    }
                    if (brandPattern != null && !brandPattern.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.BRAND_FIELD, brandPattern));
                    }
                    if (model != null && !model.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.eq(DeviceModel.MODEL_FIELD, model));
                    }
                    if (snPattern != null && !snPattern.trim().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(DeviceModel.SERIALNUMBER_FIELD, snPattern));
                    }
                    if(date != null){
                        appendDateFilters(select, startDate);
                    }
                },
                null,
                page,
                pageSize);
    }

    private void appendDateFilters(SelectBuilder select, LocalDate Date) throws Exception {
        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(DeviceModel.OBTAINED_FIELD, Date,null,null);
        select.addFilter(dateRangeExpr);
    }
    
    public URI update(URI deviceType, URI deviceURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        DeviceModel device = initDevice(deviceType, name, relations, currentUser);
        device.setUri(deviceURI);

        sparql.update(device);

        return device.getUri();
    }
    
    public DeviceModel getDeviceByURI(URI deviceURI, UserModel currentUser) throws Exception {
        return sparql.getByURI(DeviceModel.class, deviceURI, currentUser.getLanguage());
    }
    
     public void delete(URI deviceURI, UserModel currentUser) throws Exception {
        sparql.delete(DeviceModel.class, deviceURI);
    }
}