//******************************************************************************
//                          DataDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
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
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.model.NoSQLModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author sammy
 */
public class DataDAO {

    protected final URI RDFTYPE_VARIABLE;
    private final URI RDFTYPE_SCIENTIFICOBJECT;

    protected final DataNucleusService nosql;
    protected final SPARQLService sparql;
    protected final FileStorageService fs;
    
    public DataDAO(DataNucleusService datanucleus,SPARQLService sparql, FileStorageService fs) throws URISyntaxException{
        this.RDFTYPE_VARIABLE = new URI(Oeso.Variable.toString());
        this.RDFTYPE_SCIENTIFICOBJECT = new URI(Oeso.ScientificObject.toString());

        this.nosql = datanucleus;
        this.sparql = sparql;
        this.fs = fs;
    }
   
    public <T extends DataModel> T create(T instance) throws NamingException, URISyntaxException, Exception{
        nosql.prepareInstanceCreation(instance,  instance.getUri()!=null);
        return instance;
    }

    public Object createAll(List<DataModel> instances) throws NamingException, URISyntaxException, Exception {
        nosql.prepareInstancesListCreation(instances);
        return instances;
    }

    public Object update(DataModel instance) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException {
        nosql.update(instance);
        return instance;
    }

    
    public ErrorResponse valid(DataModel data) throws SPARQLException, NamingException, IOException, Exception{
        //check variables uri
        if (data.getVariable() != null) {
            if (!sparql.uriExists(VariableModel.class, data.getVariable())) {
                return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "wrong variable uri",
                                "Variable "+data.getVariable()+" doesn't exist"
                    );
            }
        }

        //check objects uri
        if (data.getObject() != null) {
            if (!data.getObject().isEmpty()) {
                List<URI> objects = new ArrayList<>();
                data.getObject().forEach(object -> {
                    objects.add(object.getUri());
                });
                if (!sparql.uriListExists(ScientificObjectModel.class, objects )) {
                    return new ErrorResponse(
                                    Response.Status.BAD_REQUEST,
                                    "wrong object uri",
                                    "A given object uri doesn't exist " + objects.toString()
                        );
                }
            }
        }
        //check provenance uri
        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        if (!provDAO.provenanceExists(data.getProvenanceURI())) {
            return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "wrong provenance uri",
                            "Provenance "+ data.getProvenanceURI() +" doesn't exist"
                );
        }
        
        return null;
    }

    public void prepareURI(DataModel data) throws Exception {
        String[] URICompose = new String[3];
        VariableModel var = sparql.getByURI(VariableModel.class, data.getVariable(),null);
        URICompose[0] = var.getName();
        if(URICompose[0] == null) URICompose[0] = var.getLongName();

        if (data.getObject() != null) {
            URICompose[1] = "Not implemented yet";
        }

        ProvenanceModel prov = new ProvenanceModel();
        prov = nosql.findByURI(prov, data.getProvenanceURI());
        URICompose[2] = prov.getLabel();

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
        
        if (objectUri != null) {
            filter = filter + "scientificObjects.contains(obj) && obj.uri == \"" + objectUri.toString() +"\" && ";
        }

        if (variableUri != null) {
            filter = filter + "variable == \"" + variableUri.toString() + "\" && ";
        }

        if (provenanceUri != null) {
            filter = filter + "provenanceURI == \"" + provenanceUri.toString() + "\" && ";
        }

        if (startDate != null) {
            filter = filter + "date > :dateMin && ";
            try {
                LocalDate sdate = LocalDate.parse(startDate, simpleDateFormatter);
                startDate += "T00:00:00+0000";
            } catch (Exception e) {
            }
            params.put("dateMin", convertDateTime(startDate));
        }
        if (endDate != null) {
            filter = filter + "date < :dateMax && ";
            try {
                LocalDate edate = LocalDate.parse(endDate, simpleDateFormatter);
                endDate += "T00:00:00+0000";
            } catch (Exception e) {
            }
            params.put("dateMax", convertDateTime(endDate));
        }

        if (filter.length() > 4) {
            filter = filter.substring(0, filter.length() - 4);
        }

        try (PersistenceManager pm = nosql.getPersistentConnectionManager()) {
            int total = nosql.countResults(pm, DataModel.class, filter, params);

            List<DataModel> results = nosql.searchWithPagination(pm, DataModel.class, filter, params, page, pageSize, total);
            List<DataModel> datas = new ArrayList<>();

            for (DataModel res : results) {
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
    }

    public DataModel get(URI uri) throws NamingException, NoSQLInvalidURIException {
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            Query q = persistenceManager.newQuery(DataModel.class);

            String filter = "uri == \"" + uri.toString() + "\"";
            q.setFilter(filter);
            DataModel res = (DataModel) q.executeUnique();
            DataModel data = new DataModel();
            if (res == null) {
                throw new NoSQLInvalidURIException(uri);
            }
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


    public DataFileModel getFile(URI uri) throws NamingException, NoSQLInvalidURIException{
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            Query q = persistenceManager.newQuery(DataFileModel.class);

            String filter = "uri == \"" + uri.toString() +"\"";
            q.setFilter(filter);
            DataFileModel res = (DataFileModel) q.executeUnique();            
            if(res == null)
                throw new NoSQLInvalidURIException(uri);
            DataFileModel data = convertResultToDataFileModel(res);

            return data;
        }
    }
    
    public void delete(URI uri) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        nosql.delete(new DataModel(), uri);
    }

    public ErrorResponse validList(Set<URI> variables, Set<URI> objects, Set<String> provenances) throws Exception {

        //check variables uri
        if (!variables.isEmpty()) {
            if (!sparql.uriListExists(VariableModel.class, variables)) {
                return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "wrong variable uri",
                                "A given variety uri doesn't exist"
                    );
            }
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

    public List<VariableModel> getVariablesByExperiment(URI xpUri, String language) throws Exception {
        List<URI> provenances = getProvenancesByExperiment(xpUri);
        try (PersistenceManager pm = nosql.getPersistentConnectionManager()) {
            Query<DataModel> query = pm.newQuery(DataModel.class);
            query.setResult("DISTINCT variable");

            if (provenances.size() > 0) {
                String filter = "";
                for (URI provenanceURI : provenances) {
                    filter = filter + "provenanceURI == \"" + provenanceURI.toString() + "\" || ";
                }
                filter = filter.substring(0, filter.length() - 4);

                if (!filter.trim().isEmpty()) {
                    query.setFilter(filter);
                }
//                query.compile();
//                String s = query.toString();
                List variableURIs = query.executeResultList();

                return sparql.getListByURIs(VariableModel.class, variableURIs, language);
            } else {
                return new ArrayList<>();
            }
        }

    }

    public List<URI> getProvenancesByExperiment(URI xpUri) throws Exception {
        try (PersistenceManager pm = nosql.getPersistentConnectionManager()) {
            Query<ProvenanceModel> query = pm.newQuery(ProvenanceModel.class);
            query.setResult("DISTINCT uri");

            String filter = "experiments.contains(exp) && exp.uri == '" + xpUri.toString() + "'";

            query.setFilter(filter);
            List provenancesURIs = query.executeResultList();

            return provenancesURIs;
        }
    }

    public <T extends DataFileModel> void createFile(DataFileModel model, File file) throws URISyntaxException, Exception {
        //generate URI

        nosql.generateUniqueUriIfNullOrValidateCurrent(model);
        
        Path fileStorageDirectory = Paths.get(fs.getStorageBasePath().toString()).toAbsolutePath();
        final String filename = Base64.getEncoder().encodeToString(model.getUri().toString().getBytes());
        model.setPath(fileStorageDirectory.toString() + "/" + filename);    
        
        //copy file to directory
        try {
            fs.createDirectories(fileStorageDirectory);
            fs.writeFile(Paths.get(model.getPath()), file);
            create(model);
            
        } catch (IOException e) {            
        }     

    }

    public ListWithPagination<DataFileModel> searchFiles(UserModel user, URI objectUri, URI provenanceUri, String startDate, String endDate, int page, int pageSize) throws NamingException, Exception {
        //build filter and params
        DateTimeFormatter simpleDateFormatter = DateTimeFormatter.ofPattern(DateFormat.YMD.toString());        

        Map params = new HashMap(); 
        String filter = "";

        if (objectUri != null)
            filter = filter + "scientificObjects.contains(obj) && obj.uri == \"" + objectUri.toString() +"\" && ";

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
            int total = nosql.countResults(pm, DataFileModel.class, filter, params);

            List<DataFileModel> results = nosql.searchWithPagination(pm, DataFileModel.class, filter, params, page, pageSize, total);
            List<DataFileModel> datas = new ArrayList<>();

            for (DataFileModel res:results){
                DataFileModel data = convertResultToDataFileModel(res);
                datas.add(data);
            }

            return new ListWithPagination<>(datas, page, pageSize, total);

        }    
            
    }

    private DataFileModel convertResultToDataFileModel(DataFileModel res) {
        DataFileModel data = new DataFileModel();
        data.setUri(res.getUri());
        data.setObject(res.getObject());
        data.setProvenanceURI(res.getProvenanceURI());
        data.setProvenanceSettings(res.getProvenanceSettings());
        data.setProvUsed(res.getProvUsed());
        data.setDate(res.getDate());
        data.setMetadata(res.getMetadata());
        data.setRdfType(res.getRdfType());
        data.setFilename(res.getFilename());
        data.setPath(res.getPath());
        return(data);

    }
    
}
