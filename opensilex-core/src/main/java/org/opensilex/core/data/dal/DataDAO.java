/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.dal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.naming.NamingException;
import javax.ws.rs.core.Response;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author sammy
 */
public class DataDAO {
    
    protected final URI RDFTYPE_VARIABLE ;
    private final URI RDFTYPE_SCIENTIFICOBJECT;
    
    protected final DataNucleusService nosql;
    protected final SPARQLService sparql;
    
    public DataDAO(DataNucleusService datanucleus,SPARQLService sparql) throws URISyntaxException{
        this.RDFTYPE_VARIABLE = new URI(Oeso.Variable.toString());
        this.RDFTYPE_SCIENTIFICOBJECT = new  URI(Oeso.ScientificObject.toString());
        
        this.nosql = datanucleus;
        this.sparql = sparql;
    }
    
    public Object create(DataModel instance) throws NamingException, URISyntaxException, Exception{
        nosql.prepareInstanceCreation(instance);
        return instance;
    }
    
    public Object createAll(List<DataModel> instances) throws NamingException, URISyntaxException, Exception{
        nosql.prepareInstancesListCreation(instances);
        return instances;
    }
    
    public Object update(DataModel instance) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        nosql.update(instance);
        return instance;
    }
    
    public boolean valid(DataModel data) throws SPARQLException, NamingException, IOException, Exception{
        ProvenanceDAO provenanceDAO = new ProvenanceDAO(nosql);
        boolean exist = false;
        
        exist = sparql.uriExists(RDFTYPE_VARIABLE, data.getVariable());
        if(data.getObject() != null)
            exist = (exist && sparql.uriExists(RDFTYPE_SCIENTIFICOBJECT, data.getObject()));        
        exist = (exist && provenanceDAO.provenanceExists(data.getProvenanceURI()));
        
        return exist;
    }
    
    public void prepareURI(DataModel data) throws Exception{
        String[] URICompose = new String[3];
            
        VariableModel var = sparql.getByURI(VariableModel.class, data.getVariable(),null);
        URICompose[0] = var.getName();
        if(URICompose[0] == null) URICompose[0] = var.getLongName();

        if(data.getObject() != null){
            URICompose[1] = "Not implemented yet";
        }

        ProvenanceModel prov = new ProvenanceModel();
        prov = nosql.findByURI(prov, data.getProvenanceURI());
        URICompose[2]=prov.getLabel();

        data.setURICompose(URICompose);
    }
    
    public ListWithPagination<DataModel> search(
            UserModel user,
            //URI uri,
            URI objectUri,
            URI variableUri,
            URI provenanceUri,
            String startDate,
            String endDate,
            Integer page,
            Integer pageSize) throws NamingException, IOException, ParseException, Exception {
            
            //build filter and params
            DateTimeFormatter simpleDateFormatter = DateTimeFormatter.ofPattern(DateFormat.YMD.toString());        
        
            Map params = new HashMap(); 
            String filter = "";

            /*if (uri != null)
                filter = filter + "uri == \"" + uri.toString() + "\" && ";*/

            if (objectUri != null)
                filter = filter + "object == \"" + objectUri.toString() +"\" && ";

            if (variableUri != null)
                filter = filter + "variable == \"" + variableUri.toString() +"\" && ";

            if (provenanceUri != null)
                filter = filter + "provenanceURI == \""+ provenanceUri.toString() + "\" && ";

            if (startDate != null){
                filter = filter + "date > :dateMin && ";
                try {
                    LocalDate sdate = LocalDate.parse(startDate,simpleDateFormatter);
                    startDate += "T00:00:00+0000";
                } catch (Exception e) {
                }
                params.put("dateMin", convertDateTime(startDate));
            }
            if(endDate != null){
                filter = filter + "date < :dateMax && ";
                try {
                    LocalDate edate = LocalDate.parse(endDate,simpleDateFormatter);
                    endDate += "T00:00:00+0000";
                } catch (Exception e) {
                }
                params.put("dateMax", convertDateTime(endDate));
            }

            if(filter.length() > 4)
                    filter = filter.substring(0,filter.length() - 4);         
            
            try (PersistenceManager pm = nosql.getPersistentConnectionManager()) {
                int total = nosql.countResults(pm, DataModel.class, filter, params);
                
                List<DataModel> results = nosql.searchWithPagination(pm, DataModel.class, filter, params, page, pageSize, total);
                List<DataModel> datas = new ArrayList<>();

                for (DataModel res:results){
                    DataModel data = new DataModel();
                    data.setUri(res.getUri());
                    data.setObject(res.getObject());
                    data.setProvenanceURI(res.getProvenanceURI());
                    data.setProvenanceSettings(res.getProvenanceSettings());
                    data.setProvUsed(res.getProvUsed());
                    data.setVariable(res.getVariable());
                    data.setDate(res.getDate());
                    data.setValue(res.getValue());
                    data.setConfidence(res.getConfidence());
                    data.setMetadata(res.getMetadata());
                    datas.add(data);
                }
                
                return new ListWithPagination<>(datas, page, pageSize, total);
                
            }

            
        
//        try (PersistenceManager pm = nosql.getPersistentConnectionManager()) {
//            nosql.flush();
//            
//            Query countQuery = pm.newQuery("SELECT count(uri) FROM org.opensilex.core.data.dal.DataModel");
//            countQuery = buildCountQuery(countQuery, objectUri, variableUri, provenanceUri, startDate, endDate);
//            Object countResult = countQuery.execute();
//            long total = (long) countResult;
//            int i = (int) total;
//            
//            List<DataModel> results;
//            if (pageSize == null || pageSize == 0) {
//                results = new ArrayList<>();                
//            } else if (total > 0 && (page * pageSize) < total) {
//                Integer offset = null;
//                Integer limit = null;
//                if (page == null || page < 0) {
//                    page = 0;
//                }
//                if (pageSize != null && pageSize > 0) {
//                    offset = page * pageSize;
//                    limit = pageSize;
//                }
//                Query selectQuery = pm.newQuery(DataModel.class);
//                selectQuery = buildSelectQuery(selectQuery, objectUri, variableUri, provenanceUri, startDate, endDate);  
//                selectQuery.setRange(offset, offset+limit);
//                results = selectQuery.executeList();
//            } else {
//                results = new ArrayList<>();
//            }
//            
//            List<DataModel> datas = new ArrayList<>();
//
//            for (DataModel res:results){
//                DataModel data = new DataModel();
//                data.setUri(res.getUri());
//                data.setObject(res.getObject());
//                data.setProvenanceURI(res.getProvenanceURI());
//                data.setProvenanceSettings(res.getProvenanceSettings());
//                data.setProvUsed(res.getProvUsed());
//                data.setVariable(res.getVariable());
//                data.setDate(res.getDate());
//                data.setValue(res.getValue());
//                data.setConfidence(res.getConfidence());
//                data.setMetadata(res.getMetadata());
//                datas.add(data);
//            }
//
//            ListWithPagination LWPdata = new ListWithPagination<>(datas, page, pageSize, (int) total);
//
//            return LWPdata;
//        }       
    }
    
    public DataModel get(URI uri) throws NamingException, NoSQLInvalidURIException{
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            Query q = persistenceManager.newQuery(DataModel.class);

            String filter = "uri == \"" + uri.toString() +"\"";
            q.setFilter(filter);
            DataModel res = (DataModel) q.executeUnique();
            DataModel data = new DataModel();
            if(res == null)
                throw new NoSQLInvalidURIException(uri);
            data.setUri(res.getUri());
            data.setObject(res.getObject());
            data.setProvenanceURI(res.getProvenanceURI());
            data.setProvenanceSettings(res.getProvenanceSettings());
            data.setProvUsed(res.getProvUsed());
            data.setVariable(res.getVariable());
            data.setDate(res.getDate());
            data.setValue(res.getValue());
            data.setConfidence(res.getConfidence());
            data.setMetadata(res.getMetadata());

            return data;
        }
    }
    
    public void delete(URI uri) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        nosql.delete(new DataModel(), uri);
    }

    public ErrorResponse validList(Set<URI> variables, Set<URI> objects, Set<String> provenances) throws Exception {

        //check variables uri
        if (!sparql.uriListExists(VariableModel.class, variables)) {
            return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "wrong variable uri",
                            "A given variety uri doesn't exist"
                );
        }
        
        //check objects uri
        if (!objects.isEmpty()) {
            if (!sparql.uriListExists(ScientificObjectModel.class, objects)) {
                return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "wrong object uri",
                                "A given object uri doesn't exist"
                    );
            }
        }
        
        //check provenance uri
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        if (!provDAO.provenanceListExists(provenances)) {
            return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "wrong provenance uri",
                            "A given provenance uri doesn't exist"
                );
        }
        
        return null;
        
    }

    public ZonedDateTime convertDateTime(String strDate) {
        DateFormat[] formats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ};
        ZonedDateTime zdt = null;
        for (DateFormat dateCheckFormat : formats) {
            try { 
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                zdt = ZonedDateTime.parse(strDate, dtf);
                break;
            } catch (DateTimeParseException e) {
            }                    
        }
        return zdt;        
    }
    
}
