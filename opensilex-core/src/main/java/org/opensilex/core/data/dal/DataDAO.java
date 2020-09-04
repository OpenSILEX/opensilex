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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.naming.NamingException;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;

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
        this.RDFTYPE_VARIABLE = new URI("http://www.opensilex.org/vocabulary/oeso#Variable");
        this.RDFTYPE_SCIENTIFICOBJECT = new  URI("http://www.opensilex.org/vocabulary/oeso#ScientificObject");
        
        this.nosql = datanucleus;
        this.sparql = sparql;
    }
    
    public Object create(DataModel instance) throws NamingException, URISyntaxException, Exception{
        nosql.prepareInstanceCreation(instance);
        return instance;
    }
    
    public Object update(DataModel instance) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        nosql.update(instance);
        return instance;
    }
    
    public boolean valid(DataModel data) throws SPARQLException, NamingException, IOException, Exception{
        ProvenanceDAO provenanceDAO = new ProvenanceDAO(nosql);
        boolean exist = false;
        
        exist = sparql.uriExists(RDFTYPE_VARIABLE, data.getVariable());
        exist = (exist && sparql.uriExists(RDFTYPE_SCIENTIFICOBJECT, data.getObject()));        
        exist = (exist && provenanceDAO.exist(data.getProvenance()));
        
        if(exist){
            String[] URICompose = new String[3];
            
            VariableModel var = sparql.getByURI(VariableModel.class, data.getVariable(),null);
            URICompose[0] = var.getName();
            if(URICompose[0] == null) URICompose[0] = var.getLongName();
            
            URICompose[1] = "Not implemented yet";
            
            ProvenanceModel prov = new ProvenanceModel();
            prov = nosql.findByUri(prov, data.getProvenance());
            URICompose[2]=prov.getLabel();
            
            data.setURICompose(URICompose);
        }
        return exist;
    }
    
    public ListWithPagination<DataModel> search(
            UserModel user,
            URI uri,
            URI objectUri,
            URI variableUri,
            URI provenanceUri,
            String startDate,
            String endDate,
            Integer page,
            Integer pageSize) throws NamingException, IOException, ParseException {
        
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            
            try (JDOQLTypedQuery<DataModel> tq = persistenceManager.newJDOQLTypedQuery(DataModel.class)) {
                /*QDataModel cand = QDataModel.candidate();
                List<BooleanExpression> exprs = new ArrayList<>();
                BooleanExpression expr = null;
                
                if (uri != null) exprs.add(cand.uri.eq(uri));
                if (objectUri != null) exprs.add(cand.object.eq(objectUri));
                if (variableUri != null) exprs.add(cand.variable.eq(variableUri));
                if (provenanceUri != null) exprs.add(cand.provenance.eq(provenanceUri));
               if (startDate != null) exprs.add(cand.date.gt(startDate));
                if (endDate != null) exprs.add(cand.date.lt(endDate));
                
                if(!exprs.isEmpty()){
                    expr = exprs.remove(0);
                    for(BooleanExpression e:exprs)
                        expr = expr.and(e);
                }
                
                List<DataModel> results = null;
                if (expr != null) {
                    results = tq.filter(expr).executeList();
                } else {
                    results = tq.executeList();                    
                }*/
                
                Query q = persistenceManager.newQuery(DataModel.class);
                
                q.declareImports("import java.time.ZonedDateTime");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXX");
                
                String filter = "";
                
                if (uri != null)
                    filter = filter + "uri == \"" + uri.toString() + "\" && ";
                
                if (objectUri != null)
                    filter = filter + "object == \"" + objectUri.toString() +"\" && ";
                
                if (variableUri != null)
                    filter = filter + "variable == \"" + variableUri.toString() +"\" && ";
                    
                if (provenanceUri != null)
                    filter = filter + "provenance == \""+ provenanceUri.toString() + "\" && ";
                if (startDate != null){
                    q.declareParameters("ZonedDateTime dateMin");
                    filter = filter + "date > dateMin && ";
                    q.setParameters(ZonedDateTime.parse(startDate,dtf));
                }
                if(endDate != null){
                    q.declareParameters("ZonedDateTime dateMax");
                    filter = filter + "date < dateMax && ";
                    q.setParameters(ZonedDateTime.parse(endDate,dtf));
                }
                
                List<DataModel> results = null;
                
                if(filter.length() > 4)
                    filter = filter.substring(0,filter.length() - 4);
                
                q.setFilter(filter);
                results = q.executeList();
                    
                List<DataModel> datas = new ArrayList<>();
                for (DataModel res:results){
                    DataModel data = new DataModel();
                    data.setUri(res.getUri());
                    data.setObject(res.getObject());
                    data.setProvenance(res.getProvenance());
                    data.setVariable(res.getVariable());
                    data.setDate(res.getDate());
                    data.setValue(res.getValue());
                    datas.add(data);
                }
                
                ListWithPagination LWPdata = new ListWithPagination<>(datas, page, pageSize, results.size());
                
                return LWPdata;
            }
        }       
    }
    
    
}
